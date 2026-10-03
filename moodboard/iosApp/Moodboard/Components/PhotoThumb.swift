import SwiftUI

struct PhotoThumb: View {
    let path: String
    var maxPixel: CGFloat = 400
    var contentMode: ContentMode = .fill

    @State private var image: UIImage?

    var body: some View {
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
                image = await ImageCache.shared.image(path: path, maxPixel: maxPixel)
            }
    }
}
