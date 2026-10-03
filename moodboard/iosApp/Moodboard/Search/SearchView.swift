import MoodboardShared
import SwiftUI

struct SearchView: View {
    @State private var model = ScreenModel({ $0.search() }, state: { $0.state })
    @State private var query = ""
    @Namespace private var zoom

    var body: some View {
        NavigationStack {
            ScrollView {
                PhotoGrid(photos: model.state.results, zoom: zoom) { photo in
                    ShareLink(item: SharedPhoto(path: photo.path, title: photo.title), preview: SharePreview(photo.title))
                }
            }
            .navigationTitle("Search")
            .searchable(text: $query, prompt: "Photos and tags")
            .searchSuggestions {
                ForEach(model.state.suggestions, id: \.self) { tag in
                    Label(tag, systemImage: "tag").searchCompletion(tag)
                }
            }
            .onChange(of: query) { _, newValue in
                model.viewModel.onAction(action: SearchActionQueryChanged(query: newValue))
            }
            .navigationDestination(for: PhotoRoute.self) { route in
                PhotoDetailView(photoId: route.id)
                    .navigationTransition(.zoom(sourceID: route.id, in: zoom))
            }
        }
        .task { await model.observe() }
    }
}
