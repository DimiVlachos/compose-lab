import MoodboardShared
import SwiftUI
import UIKit

/// Hosts a Kotlin-built ComposeUIViewController inside SwiftUI.
struct ComposeScreen: UIViewControllerRepresentable {
    let make: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController { make() }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
