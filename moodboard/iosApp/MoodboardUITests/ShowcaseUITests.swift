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
        at(1.5) { self.scroll(up: true) }                          // scroll: the title collapses
        at(3.0) { self.scroll(up: false) }
        at(4.5) { grid.buttons["Mykonos"].tap() }                  // open a photo
        at(7.5) { self.back() }                                    // close it
        at(9.0) { grid.buttons["Santorini"].press(forDuration: 0.8) }  // the photo menu
        at(11.0) { self.app.buttons["Delete"].tap() }              // delete: the confirmation
        at(12.5) { self.app.alerts.buttons["Cancel"].tap() }
        at(14.0) { self.app.navigationBars.buttons["Filter"].tap() }  // the filter sheet
        // Dragged by the grabber: the hosted Compose body keeps touches that start on it.
        at(16.0) { self.drag(from: 0.49, to: 0.97) }               // swiped away
        at(17.5) { self.app.tabBars.buttons["Search"].tap() }      // search tab: suggestions first
        at(19.0) { self.app.searchFields.firstMatch.tap() }
        at(20.0) { self.app.searchFields.firstMatch.typeText("ruins") }  // results as you type
        at(21.5) { self.app.searchFields.firstMatch.typeText("\n") }
        at(23.0) { self.app.buttons["Delphi"].firstMatch.tap() }   // open a result
        at(25.5) { self.back() }
        // While search is open, iOS folds the other tabs into one button, back to where you were.
        at(27.0) { self.app.tabBars.buttons.element(boundBy: 0).tap() }  // back to the gallery
        at(28.5) { self.app.tabBars.buttons["Boards"].tap() }      // boards tab
        at(30.0) { self.app.staticTexts["Islands"].tap() }         // a board
        at(32.5) { self.edgeBack() }                               // the back gesture
        at(34.0) {                                                 // delete a board: the dialog
            self.app.cells.containing(.staticText, identifier: "Blue").firstMatch.swipeLeft()
        }
        at(35.5) { self.app.buttons["Delete"].firstMatch.tap() }
        // iOS 26 shows it as a popover with no Cancel button; a tap outside dismisses it.
        at(37.5) { self.app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85)).tap() }
        at(39.0) { self.app.tabBars.buttons["Gallery"].tap() }     // home
        at(41.0) {}
    }

    /// A finger drag over half the screen in 0.4 s, like the Android script's swipe.
    private func scroll(up: Bool) {
        let (from, to) = up ? (0.8, 0.31) : (0.31, 0.8)
        drag(from: from, to: to, velocity: 3200)
    }

    private func drag(from: CGFloat, to: CGFloat, velocity: CGFloat = 2400) {
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: from))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: to))
        start.press(forDuration: 0.05, thenDragTo: end, withVelocity: XCUIGestureVelocity(velocity), thenHoldForDuration: 0)
    }

    /// Swipe back from the left edge, the system gesture.
    private func edgeBack() {
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.5))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.6, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: end, withVelocity: XCUIGestureVelocity(2400), thenHoldForDuration: 0)
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
