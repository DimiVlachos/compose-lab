import MoodboardShared
import SwiftUI

// Spike: a native list pushing a hosted Compose screen under glass toolbar items.
struct RootTabView: View {
    @State private var path: [Int] =
        ProcessInfo.processInfo.arguments.contains("-spike") ? [3] : []

    var body: some View {
        TabView {
            Tab("Gallery", systemImage: "photo.on.rectangle") {
                NavigationStack(path: $path) {
                    List(0..<30, id: \.self) { i in
                        NavigationLink("Photo \(i)", value: i)
                    }
                    .navigationTitle("Gallery")
                    .navigationDestination(for: Int.self) { _ in
                        ComposeScreen { ViewControllersKt.SpikeDetailViewController() }
                            .ignoresSafeArea()
                            .navigationTitle("Detail")
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .topBarTrailing) {
                                    Button("Favorite", systemImage: "heart") {}
                                }
                                ToolbarItem(placement: .bottomBar) {
                                    Button("Share", systemImage: "square.and.arrow.up") {}
                                }
                            }
                    }
                }
            }
            Tab("Boards", systemImage: "square.grid.2x2") { Text("Boards") }
        }
    }
}
