import XCTest

/// The README showcase: the same walk-through as scripts/moodboard/showcase-android.py, on the
/// same timeline, so the two recordings play side by side step for step. Skipped unless the run
/// sets SHOWCASE=1 (TEST_RUNNER_SHOWCASE=1 from xcodebuild); scripts/moodboard/showcase.sh does.
final class ShowcaseUITests: XCTestCase {
    private let app = XCUIApplication()
    private var start = Date()

    override func setUpWithError() throws {
        try XCTSkipUnless(ProcessInfo.processInfo.environment["SHOWCASE"] == "1", "showcase runs only on request")
        continueAfterFailure = false
        app.launch()
        XCTAssertTrue(app.scrollViews.firstMatch.buttons["Mykonos"].waitForExistence(timeout: 10))
        start = Date()
    }

    func testShowcase() {
        let grid = app.scrollViews.firstMatch
        at(1.5) { grid.buttons["Mykonos"].tap() }                 // open a photo
        at(4.5) { self.back() }                                    // close it
        at(6.5) { grid.buttons["Santorini"].press(forDuration: 0.8) }  // the photo menu
        at(8.5) { self.app.buttons["Delete"].tap() }               // delete: the confirmation
        at(10.0) { self.app.alerts.buttons["Cancel"].tap() }
        at(11.5) { self.app.tabBars.buttons["Search"].tap() }      // search tab: suggestions first
        at(13.0) { self.app.searchFields.firstMatch.tap() }
        at(14.0) { self.app.searchFields.firstMatch.typeText("ruins") }  // results as you type
        at(15.5) { self.app.searchFields.firstMatch.typeText("\n") }
        at(17.0) { self.app.buttons["Delphi"].firstMatch.tap() }   // open a result
        at(19.5) { self.back() }
        // While search is open, iOS folds the other tabs into one button, back to where you were.
        at(21.0) { self.app.tabBars.buttons.element(boundBy: 0).tap() }  // back to the gallery
        at(22.5) { self.app.tabBars.buttons["Boards"].tap() }      // boards tab
        at(24.0) { self.app.staticTexts["Islands"].tap() }         // a board
        at(26.5) { self.back() }
        at(28.0) { self.app.tabBars.buttons["Gallery"].tap() }     // home
        at(30.5) {}
    }

    private func back() {
        app.navigationBars.buttons.element(boundBy: 0).tap()
    }

    /// Runs [step] at [seconds] from the start, or at once if the previous step overran.
    private func at(_ seconds: TimeInterval, _ step: () -> Void) {
        let wait = start.addingTimeInterval(seconds).timeIntervalSinceNow
        if wait > 0 { Thread.sleep(forTimeInterval: wait) }
        step()
    }
}
