import MoodboardShared
import SwiftUI

struct BoardsView: View {
    @State private var model = ScreenModel({ $0.boards() }, state: { $0.state })
    @State private var creating = false
    @State private var renaming: BoardSummary?
    @State private var deleting: BoardSummary?
    @State private var name = ""

    var body: some View {
        NavigationStack {
            List {
                LargeTitleHeader(title: "Boards")
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                ForEach(model.state.boards) { board in
                    NavigationLink(value: BoardRoute(id: board.id)) {
                        BoardRow(board: board)
                    }
                    .contextMenu {
                        Button("Rename", systemImage: "pencil") {
                            name = board.name
                            renaming = board
                        }
                        Button("Delete", systemImage: "trash", role: .destructive) { deleting = board }
                    }
                    .swipeActions {
                        Button("Delete", systemImage: "trash") { deleting = board }
                            .tint(.red)
                    }
                }
            }
            .scrollBounceBehavior(.basedOnSize)
            .scrollingTitle("Boards")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("New Board", systemImage: "plus") {
                        name = ""
                        creating = true
                    }
                }
            }
            .navigationDestination(for: BoardRoute.self) { BoardDetailView(boardId: $0.id) }
            .nameAlert("New Board", isPresented: $creating, name: $name, confirm: "Create") {
                send(BoardsActionCreate(name: $0))
            }
            .nameAlert("Rename Board", item: $renaming, name: $name, confirm: "Rename") { board, newName in
                send(BoardsActionRename(id: board.id, name: newName))
            }
            .confirmationDialog(
                "Delete “\(deleting?.name ?? "")”?",
                isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
                titleVisibility: .visible,
                presenting: deleting
            ) { board in
                Button("Delete Board Only") { send(BoardsActionDelete(id: board.id, deletePhotos: false)) }
                Button("Delete Board and Its Photos", role: .destructive) {
                    send(BoardsActionDelete(id: board.id, deletePhotos: true))
                }
                Button("Cancel", role: .cancel) {}
            }
        }
        .task { await model.observe() }
    }

    private func send(_ action: BoardsAction) {
        model.viewModel.onAction(action: action)
    }
}

private struct BoardRow: View {
    let board: BoardSummary

    var body: some View {
        HStack(spacing: 12) {
            Group {
                if let cover = board.coverPath {
                    PhotoThumb(path: cover, maxPixel: 160)
                } else {
                    Rectangle().fill(.quaternary)
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(.rect(cornerRadius: 10))
            VStack(alignment: .leading) {
                Text(board.name).font(.headline)
                Text("\(board.count) photos").font(.subheadline).foregroundStyle(.secondary)
            }
        }
    }
}
