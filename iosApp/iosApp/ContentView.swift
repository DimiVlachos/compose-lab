import SwiftUI
import ComposeApp

struct LaunchOptions {
    let demoId: String?
    let record: Bool
    let label: Bool

    init(_ arguments: [String]) {
        if let index = arguments.firstIndex(of: "-demo"), index + 1 < arguments.count {
            demoId = arguments[index + 1]
        } else {
            demoId = nil
        }
        record = arguments.contains("-record")
        label = arguments.contains("-label")
    }
}

struct ContentView: UIViewControllerRepresentable {
    let launch: LaunchOptions

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(demoId: launch.demoId, record: launch.record, label: launch.label)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
