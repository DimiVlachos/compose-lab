// Draws the iOS app icon from the same geometry as the Android adaptive icon
// (androidApp/src/main/res/drawable/ic_launcher_foreground.xml): a lab flask, half full of the
// theme's violet, with a few bubbles rising, on the theme's background.
// Run: swift scripts/app-icon.swift  (writes iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png)
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

let violet: UInt32 = 0x7C5CFF
ctx.setFillColor(color(0x0B0D12))
ctx.fill(CGRect(x: 0, y: 0, width: 108, height: 108))

// The rim, then the flask: a neck that opens into a cone with rounded feet.
ctx.setFillColor(color(0xFFFFFF))
ctx.addPath(CGPath(
    roundedRect: CGRect(x: 45, y: 28, width: 18, height: 5), cornerWidth: 2.5, cornerHeight: 2.5,
    transform: nil))
ctx.fillPath()

let flask = CGMutablePath()
flask.move(to: CGPoint(x: 49, y: 33))
flask.addLine(to: CGPoint(x: 59, y: 33))
flask.addLine(to: CGPoint(x: 59, y: 48))
flask.addLine(to: CGPoint(x: 73, y: 72))
flask.addQuadCurve(to: CGPoint(x: 69, y: 78), control: CGPoint(x: 76, y: 78))
flask.addLine(to: CGPoint(x: 39, y: 78))
flask.addQuadCurve(to: CGPoint(x: 35, y: 72), control: CGPoint(x: 32, y: 78))
flask.addLine(to: CGPoint(x: 49, y: 48))
flask.closeSubpath()
ctx.addPath(flask)
ctx.fillPath()

let liquid = CGMutablePath()
liquid.move(to: CGPoint(x: 40.8, y: 62))
liquid.addCurve(to: CGPoint(x: 54, y: 62), control1: CGPoint(x: 45, y: 59), control2: CGPoint(x: 49, y: 59))
liquid.addCurve(to: CGPoint(x: 67.2, y: 62), control1: CGPoint(x: 59, y: 65), control2: CGPoint(x: 63, y: 65))
liquid.addLine(to: CGPoint(x: 73, y: 72))
liquid.addQuadCurve(to: CGPoint(x: 69, y: 78), control: CGPoint(x: 76, y: 78))
liquid.addLine(to: CGPoint(x: 39, y: 78))
liquid.addQuadCurve(to: CGPoint(x: 35, y: 72), control: CGPoint(x: 32, y: 78))
liquid.closeSubpath()
ctx.addPath(liquid)
ctx.setFillColor(color(violet))
ctx.fillPath()

func bubble(_ x: CGFloat, _ y: CGFloat, _ r: CGFloat, _ fill: CGColor) {
    ctx.setFillColor(fill)
    ctx.fillEllipse(in: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r))
}
bubble(50, 70, 2, color(0xFFFFFF, 0.7))
bubble(59, 73, 1.5, color(0xFFFFFF, 0.7))
bubble(56, 54, 1.5, color(violet))

let out = URL(fileURLWithPath: "iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try! rep.representation(using: .png, properties: [:])!.write(to: out)
print("wrote \(out.path)")
