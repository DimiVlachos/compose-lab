import MoodboardShared
import SwiftUI

struct BoardDetailView: View {
    @State private var model: ScreenModel<BoardDetailViewModel, BoardDetailState>
    @State private var editing = false
    @Namespace private var zoom
    @Environment(\.dismiss) private var dismiss
    private let boardId: String

    init(boardId: String) {
        self.boardId = boardId
        _model = State(initialValue: ScreenModel({ $0.boardDetail(boardId: boardId) }, state: { $0.state }))
    }

    var body: some View {
        ScrollView {
            PhotoGrid(photos: model.state.photos, zoom: zoom) { photo in
                ShareLink(item: SharedPhoto(path: photo.path, title: photo.title), preview: SharePreview(photo.title))
                Button("Remove from Board", systemImage: "minus.circle", role: .destructive) {
                    model.viewModel.onAction(action: BoardDetailActionRemove(photoId: photo.id))
                }
            }
        }
        .navigationTitle(model.state.board?.name ?? "")
        // Inline, as pushed screens are on iOS. A board is often shorter than the screen, and a
        // large title left half-collapsed over it fell into a layout loop: title and grid jumped
        // between two positions every frame, even with no finger on the screen.
        .navigationBarTitleDisplayMode(.inline)
        .scrollBounceBehavior(.basedOnSize)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Edit") { editing = true }
            }
        }
        .navigationDestination(for: PhotoRoute.self) { route in
            PhotoDetailView(photoId: route.id)
                .navigationTransition(.zoom(sourceID: route.id, in: zoom))
        }
        .sheet(isPresented: $editing) {
            BoardEditorSheet(boardId: boardId)
        }
        .onChange(of: model.state.isDeleted) { _, deleted in
            if deleted { dismiss() }
        }
        .task { await model.observe() }
    }
}

/// The shared Compose board editor in a native sheet with a Done button.
private struct BoardEditorSheet: View {
    @State private var model: ScreenModel<BoardEditorViewModel, BoardEditorState>
    @Environment(\.dismiss) private var dismiss

    init(boardId: String) {
        _model = State(initialValue: ScreenModel({ $0.boardEditor(boardId: boardId) }, state: { $0.state }))
    }

    var body: some View {
        NavigationStack {
            ComposeScreen { ViewControllersKt.BoardEditorViewController(viewModel: model.viewModel) }
                .ignoresSafeArea()
                .navigationTitle("Edit Board")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done", systemImage: "checkmark") { dismiss() }
                    }
                }
        }
    }
}
