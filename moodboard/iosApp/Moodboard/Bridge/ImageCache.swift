import MoodboardShared
import UIKit

/// Decoded photos, downsampled per requested size so grid cells never hold full images.
/// Lookups are synchronous (NSCache is thread-safe), so a view can draw a cached image on its
/// first frame; loads for the same key share one decode.
final class ImageCache: @unchecked Sendable {
    static let shared = ImageCache()

    private let cache: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.totalCostLimit = 96 * 1024 * 1024
        return cache
    }()
    private let loads = Loads()

    func cached(path: String, maxPixel: CGFloat) -> UIImage? {
        cache.object(forKey: key(path, maxPixel))
    }

    func image(path: String, maxPixel: CGFloat) async -> UIImage? {
        let key = key(path, maxPixel)
        if let cached = cache.object(forKey: key) { return cached }
        let cache = cache
        return await loads.load(key as String) {
            guard let data = try? await ImageDataKt.imageData(path: path),
                let full = UIImage(data: data)
            else { return nil }
            let scale = min(1, maxPixel / max(full.size.width, full.size.height))
            let size = CGSize(width: full.size.width * scale, height: full.size.height * scale)
            let image = await full.byPreparingThumbnail(ofSize: size) ?? full
            // Cached before the load leaves the in-flight table, so no request falls in between
            // and decodes it again.
            let cost = Int(image.size.width * image.size.height * image.scale * image.scale * 4)
            cache.setObject(image, forKey: key, cost: cost)
            // The Compose detail reads its own cache; warm it so the zoom opens on pixels.
            try? await ImageDataKt.warmPhoto(path: path)
            return image
        }
    }

    private func key(_ path: String, _ maxPixel: CGFloat) -> NSString {
        "\(path)@\(Int(maxPixel))" as NSString
    }
}

/// In-flight decodes, so two cells asking for one photo at once decode it once.
private actor Loads {
    private var inFlight: [String: Task<UIImage?, Never>] = [:]

    func load(_ key: String, _ make: @escaping @Sendable () async -> UIImage?) async -> UIImage? {
        if let running = inFlight[key] { return await running.value }
        let task = Task { await make() }
        inFlight[key] = task
        let image = await task.value
        inFlight[key] = nil
        return image
    }
}
