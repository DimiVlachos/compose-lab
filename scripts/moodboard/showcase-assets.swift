// Draws the showcase's overlays: a rounded-screen mask and shadow for each phone, and its label.
// ffmpeg here has no text filter, so text and shapes are rendered once as PNGs.
// Usage: swift showcase-assets.swift <outDir> <w> <h> <radius> <label> <name>
import AppKit

let a = CommandLine.arguments
let out = URL(fileURLWithPath: a[1])
let w = Int(a[2])!, h = Int(a[3])!, r = CGFloat(Double(a[4])!)
let label = a[5], name = a[6]

func png(_ w: Int, _ h: Int, _ draw: (CGContext) -> Void) -> Data {
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8, samplesPerPixel: 4,
        hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    draw(NSGraphicsContext.current!.cgContext)
    NSGraphicsContext.restoreGraphicsState()
    return rep.representation(using: .png, properties: [:])!
}

// The screen's rounded corners, as an alpha mask (white inside).
try! png(w, h) { c in
    c.setFillColor(.white)
    c.addPath(CGPath(roundedRect: CGRect(x: 0, y: 0, width: w, height: h), cornerWidth: r, cornerHeight: r, transform: nil))
    c.fillPath()
}.write(to: out.appendingPathComponent("\(name)-mask.png"))

// A soft shadow, larger than the screen, drawn behind it.
let pad = 60
try! png(w + 2 * pad, h + 2 * pad) { c in
    c.setShadow(offset: CGSize(width: 0, height: -14), blur: 46, color: CGColor(gray: 0, alpha: 0.55))
    c.setFillColor(CGColor(gray: 0, alpha: 1))
    c.addPath(CGPath(roundedRect: CGRect(x: pad, y: pad, width: w, height: h), cornerWidth: r, cornerHeight: r, transform: nil))
    c.fillPath()
}.write(to: out.appendingPathComponent("\(name)-shadow.png"))

// The platform's name under its phone.
let font = NSFont.systemFont(ofSize: 34, weight: .semibold)
let text = NSAttributedString(string: label, attributes: [.font: font, .foregroundColor: NSColor.white])
let size = text.size()
try! png(Int(size.width) + 8, Int(size.height) + 8) { _ in
    text.draw(at: NSPoint(x: 4, y: 4))
}.write(to: out.appendingPathComponent("\(name)-label.png"))
print("assets for \(name)")
