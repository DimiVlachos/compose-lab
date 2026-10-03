import MoodboardShared
import UIKit

/// Decoded photos, downsampled per requested size so grid cells never hold full images.
actor ImageCache {
    static let shared = ImageCache()

    private let cache = NSCache<NSString, UIImage>()

    func image(path: String, maxPixel: CGFloat) async -> UIImage? {
        let key = "\(path)@\(Int(maxPixel))" as NSString
        if let cached = cache.object(forKey: key) { return cached }
        guard let data = try? await ImageDataKt.imageData(path: path),
            let full = UIImage(data: data)
        else { return nil }
        let scale = min(1, maxPixel / max(full.size.width, full.size.height))
        let size = CGSize(width: full.size.width * scale, height: full.size.height * scale)
        let image = await full.byPreparingThumbnail(ofSize: size) ?? full
        cache.setObject(image, forKey: key)
        return image
    }
}
