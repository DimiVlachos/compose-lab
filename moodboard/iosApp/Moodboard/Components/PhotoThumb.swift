import SwiftUI

struct PhotoThumb: View {
    let path: String
    var maxPixel: CGFloat = 400
    var contentMode: ContentMode = .fill

    @State private var loaded: UIImage?

    var body: some View {
        // A cached image draws on the first frame; only a miss shows the placeholder.
        let image = loaded ?? ImageCache.shared.cached(path: path, maxPixel: maxPixel)
        Rectangle()
            .fill(.quaternary)
            .overlay {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: contentMode)
                }
            }
            .clipped()
            .task(id: path) {
                loaded = await ImageCache.shared.image(path: path, maxPixel: maxPixel)
            }
    }
}

/// The lifted context-menu preview: the photo at its own aspect ratio, starting from the grid's
/// cached thumbnail and sharpening once the larger version is decoded.
struct PhotoPreview: View {
    let path: String

    @State private var sharp: UIImage?

    var body: some View {
        Group {
            if let image = sharp ?? ImageCache.shared.cached(path: path, maxPixel: 400) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 360, maxHeight: 480)
            } else {
                Rectangle().fill(.quaternary).frame(width: 320, height: 320)
            }
        }
        .task(id: path) {
            sharp = await ImageCache.shared.image(path: path, maxPixel: 1000)
        }
    }
}
