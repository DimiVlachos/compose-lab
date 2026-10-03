import MoodboardShared
import SwiftUI

struct GalleryView: View {
    @State private var model = ScreenModel({ $0.gallery() }, state: { $0.state })
    @Namespace private var zoom
    @State private var filterShown = false
    @State private var deleting: Photo?
    @State private var newBoardFor: Photo?
    @State private var newBoardName = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                PhotoGrid(photos: model.state.photos, zoom: zoom) { photo in
                    PhotoMenu(
                        photo: photo,
                        boards: model.state.boards,
                        onToggleFavorite: { send(GalleryActionToggleFavorite(id: photo.id)) },
                        onSetMembership: { boardId, member in
                            send(
                                member
                                    ? GalleryActionAddToBoard(photoId: photo.id, boardId: boardId)
                                    : GalleryActionRemoveFromBoard(photoId: photo.id, boardId: boardId))
                        },
                        onNewBoard: {
                            newBoardName = ""
                            newBoardFor = photo
                        },
                        onDelete: { deleting = photo }
                    )
                }
            }
            .navigationTitle("Gallery")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Filter", systemImage: "line.3.horizontal.decrease") { filterShown = true }
                }
            }
            .navigationDestination(for: PhotoRoute.self) { route in
                PhotoDetailView(photoId: route.id)
                    .navigationTransition(.zoom(sourceID: route.id, in: zoom))
            }
            .sheet(isPresented: $filterShown) {
                ComposeScreen { ViewControllersKt.FilterSheetViewController(viewModel: model.viewModel) }
                    .presentationDetents([.medium, .large])
            }
            .deletePhotoAlert($deleting) { send(GalleryActionDelete(id: $0.id)) }
            .nameAlert("New Board", item: $newBoardFor, name: $newBoardName, confirm: "Create") { photo, name in
                send(GalleryActionCreateBoardWith(photoId: photo.id, name: name))
            }
        }
        .task { await model.observe() }
    }

    private func send(_ action: GalleryAction) {
        model.viewModel.onAction(action: action)
    }
}
