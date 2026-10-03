import MoodboardShared
import SwiftUI

/// Three columns of square photos. Each cell pushes the photo with the system zoom transition
/// and carries a native context menu with a lifted preview.
struct PhotoGrid<Menu: View>: View {
    let photos: [Photo]
    let zoom: Namespace.ID
    @ViewBuilder let menu: (Photo) -> Menu

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 2), count: 3)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 2) {
            ForEach(photos) { photo in
                NavigationLink(value: PhotoRoute(id: photo.id)) {
                    PhotoThumb(path: photo.path)
                        .aspectRatio(1, contentMode: .fit)
                        .matchedTransitionSource(id: photo.id, in: zoom)
                }
                .buttonStyle(.plain)
                .contextMenu {
                    menu(photo)
                } preview: {
                    PhotoThumb(path: photo.path, maxPixel: 1000, contentMode: .fit)
                        .frame(width: 320, height: 400)
                }
            }
        }
    }
}
