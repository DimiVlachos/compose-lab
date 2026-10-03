import SwiftUI

/// Liquid Glass tab bar; the search tab gets the bottom glass search field.
struct RootTabView: View {
    var body: some View {
        TabView {
            Tab("Gallery", systemImage: "photo.on.rectangle") { GalleryView() }
            Tab("Boards", systemImage: "square.grid.2x2") { BoardsView() }
            Tab(role: .search) { SearchView() }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
    }
}
