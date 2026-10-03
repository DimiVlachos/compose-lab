import SwiftUI

/// The big title, as the first thing in a screen's scrolling content (the App Store's Today tab,
/// Apple Music). It moves exactly with the finger, like any other content.
struct LargeTitleHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.largeTitle.bold())
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 10)
            .accessibilityAddTraits(.isHeader)
    }
}

extension View {
    /// Keeps the navigation bar at its fixed inline size and fades the small title in once the
    /// scrolling `LargeTitleHeader` has gone under the bar. The system large title instead resizes
    /// the bar while you scroll, and the scroll view's insets change with it: on short grids that
    /// fell into a layout loop, and at the handover it froze and then snapped. Here nothing ever
    /// resizes, so there is nothing to jump.
    func scrollingTitle(_ title: String) -> some View {
        modifier(ScrollingTitle(title: title))
    }
}

private struct ScrollingTitle: ViewModifier {
    let title: String
    @State private var headerScrolledAway = false

    /// About the header's height: past this the big title has slid under the bar.
    private let threshold: CGFloat = 44

    func body(content: Content) -> some View {
        content
            .onScrollGeometryChange(for: Bool.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top > threshold
            } action: { _, scrolledAway in
                withAnimation(.easeInOut(duration: 0.2)) { headerScrolledAway = scrolledAway }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    // Always white: it only shows once photos are scrolling under the bar.
                    Text(title)
                        .font(.headline)
                        .foregroundStyle(.white)
                        .opacity(headerScrolledAway ? 1 : 0)
                        .accessibilityHidden(!headerScrolledAway)
                }
            }
    }
}
