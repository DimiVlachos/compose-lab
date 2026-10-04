// Draws the iOS app icon from the same geometry as the Android adaptive icon
// (moodboard/androidApp/src/main/res/drawable/ic_launcher_foreground.xml): a photo card with a sunset
// over the sea and a second card fanned behind it, on the brand's Aegean blue.
// Run: swift scripts/app-icon.swift  (writes Moodboard/Assets.xcassets/AppIcon.appiconset/AppIcon.png)
import AppKit

let size = 1024
// Android shows the middle 72dp of its 108dp canvas; iOS shows its whole square, so map that window.
let window: CGFloat = 72
let origin: CGFloat = (108 - window) / 2
let scale = CGFloat(size) / window

func color(_ hex: UInt32, _ alpha: CGFloat = 1) -> CGColor {
    CGColor(
        red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
        blue: CGFloat(hex & 0xFF) / 255, alpha: alpha)
}

let space = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(
    data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: space,
    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
// Android's y axis points down; flip so the same numbers draw the same picture.
ctx.translateBy(x: 0, y: CGFloat(size))
ctx.scaleBy(x: scale, y: -scale)
ctx.translateBy(x: -origin, y: -origin)

ctx.setFillColor(color(0x1E6FC0))
ctx.fill(CGRect(x: 0, y: 0, width: 108, height: 108))

let card = CGPath(
    roundedRect: CGRect(x: 37, y: 33, width: 34, height: 44), cornerWidth: 3, cornerHeight: 3,
    transform: nil)

func rotated(_ degrees: CGFloat, _ draw: () -> Void) {
    ctx.saveGState()
    ctx.translateBy(x: 54, y: 55)
    ctx.rotate(by: degrees * .pi / 180)
    ctx.translateBy(x: -54, y: -55)
    draw()
    ctx.restoreGState()
}

rotated(-12) {
    ctx.addPath(card)
    ctx.setFillColor(color(0xFFFFFF, 0.5))
    ctx.fillPath()
}
rotated(6) {
    ctx.addPath(card)
    ctx.setFillColor(color(0xFFFFFF))
    ctx.fillPath()
    ctx.setFillColor(color(0xA4C9FF))
    ctx.fill(CGRect(x: 40, y: 36, width: 28, height: 28))
    ctx.setFillColor(color(0xFFB77C))
    ctx.fillEllipse(in: CGRect(x: 57, y: 41, width: 8, height: 8))
    let sea = CGMutablePath()
    sea.move(to: CGPoint(x: 40, y: 55))
    sea.addCurve(to: CGPoint(x: 53, y: 55), control1: CGPoint(x: 44.5, y: 52), control2: CGPoint(x: 48.5, y: 52))
    sea.addCurve(to: CGPoint(x: 68, y: 55), control1: CGPoint(x: 57.5, y: 58), control2: CGPoint(x: 63.5, y: 58))
    sea.addLine(to: CGPoint(x: 68, y: 64))
    sea.addLine(to: CGPoint(x: 40, y: 64))
    sea.closeSubpath()
    ctx.addPath(sea)
    ctx.setFillColor(color(0x1E6FC0))
    ctx.fillPath()
}

let out = URL(fileURLWithPath: "Moodboard/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try! rep.representation(using: .png, properties: [:])!.write(to: out)
print("wrote \(out.path)")
