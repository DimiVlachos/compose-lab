import MoodboardShared
import SwiftUI

/// SwiftUI owns the glass toolbar; the photo, tags and boards are the shared Compose screen.
/// Both read one PhotoDetailViewModel, so a tap on the toolbar heart updates the Compose content.
struct PhotoDetailView: View {
    @State private var model: ScreenModel<PhotoDetailViewModel, PhotoDetailState>
    @State private var deleting: Photo?
    @State private var newBoardShown = false
    @State private var newBoardName = ""
    @Environment(\.dismiss) private var dismiss

    init(photoId: String) {
        _model = State(initialValue: ScreenModel({ $0.photoDetail(photoId: photoId) }, state: { $0.state }))
    }

    var body: some View {
        ComposeScreen { ViewControllersKt.PhotoDetailViewController(viewModel: model.viewModel) }
            .ignoresSafeArea()
            .background(Color(.systemBackground))
            .navigationTitle(model.state.photo?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if let photo = model.state.photo {
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        Button(
                            photo.isFavorite ? "Unfavorite" : "Favorite",
                            systemImage: photo.isFavorite ? "heart.fill" : "heart"
                        ) {
                            send(PhotoDetailActionToggleFavorite.shared)
                        }
                        .sensoryFeedback(.selection, trigger: photo.isFavorite)
                        ShareLink(item: SharedPhoto(path: photo.path, title: photo.title), preview: SharePreview(photo.title))
                        Menu("More", systemImage: "ellipsis") {
                            PhotoMenu(
                                photo: photo,
                                boards: model.state.boards,
                                showsShare: false,
                                onToggleFavorite: { send(PhotoDetailActionToggleFavorite.shared) },
                                onSetMembership: { boardId, member in
                                    send(PhotoDetailActionSetMembership(boardId: boardId, member: member))
                                },
                                onNewBoard: {
                                    newBoardName = ""
                                    newBoardShown = true
                                },
                                onDelete: { deleting = photo }
                            )
                        }
                    }
                }
            }
            .deletePhotoAlert($deleting) { _ in send(PhotoDetailActionDelete.shared) }
            .nameAlert("New Board", isPresented: $newBoardShown, name: $newBoardName, confirm: "Create") {
                send(PhotoDetailActionCreateBoard(name: $0))
            }
            .onChange(of: model.state.isDeleted, initial: true) { _, deleted in
                if deleted { dismiss() }
            }
            .task { await model.observe() }
    }

    private func send(_ action: PhotoDetailAction) {
        model.viewModel.onAction(action: action)
    }
}
