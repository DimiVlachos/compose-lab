import MoodboardShared
import SwiftUI

/// Share, favorite, add to board ▸ and delete — the same actions as Android's long-press menu.
struct PhotoMenu: View {
    let photo: Photo
    let boards: [Board]
    /// Off where the screen already has its own share button.
    var showsShare = true
    let onToggleFavorite: () -> Void
    let onSetMembership: (_ boardId: String, _ member: Bool) -> Void
    let onNewBoard: () -> Void
    let onDelete: () -> Void

    var body: some View {
        if showsShare {
            ShareLink(item: SharedPhoto(path: photo.path, title: photo.title), preview: SharePreview(photo.title))
        }
        Button(
            photo.isFavorite ? "Unfavorite" : "Favorite",
            systemImage: photo.isFavorite ? "heart.slash" : "heart",
            action: onToggleFavorite
        )
        Menu("Add to Board", systemImage: "square.grid.2x2") {
            ForEach(boards) { board in
                let member = board.photoIds.contains(photo.id)
                Button {
                    onSetMembership(board.id, !member)
                } label: {
                    if member {
                        Label(board.name, systemImage: "checkmark")
                    } else {
                        Text(board.name)
                    }
                }
            }
            Divider()
            Button("New Board…", systemImage: "plus", action: onNewBoard)
        }
        Divider()
        Button("Delete", systemImage: "trash", role: .destructive, action: onDelete)
    }
}
