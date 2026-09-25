import SwiftUI

@main
struct iOSApp: App {
    private let launch = LaunchOptions(ProcessInfo.processInfo.arguments)

    var body: some Scene {
        WindowGroup {
            ContentView(launch: launch)
                .ignoresSafeArea()
                .statusBarHidden(launch.record)
                .persistentSystemOverlays(launch.record ? .hidden : .automatic)
        }
    }
}
