import XCTest

/// Walks every native component once. Set SCREENSHOT_DIR to also save a PNG at each step.
final class NativeComponentsUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launch()
    }

    func testGalleryDetailMenusAndSheets() {
        let firstPhoto = app.scrollViews.firstMatch.buttons.firstMatch
        XCTAssertTrue(firstPhoto.waitForExistence(timeout: 10))
        snap("gallery")

        firstPhoto.tap()
        XCTAssertTrue(app.navigationBars["Santorini"].waitForExistence(timeout: 5))
        snap("detail")
        app.navigationBars.buttons["More"].tap()
        XCTAssertTrue(app.buttons["Add to Board"].waitForExistence(timeout: 3))
        snap("detail-menu")
        app.buttons["Delete"].tap()
        XCTAssertTrue(app.alerts["Delete photo?"].waitForExistence(timeout: 3))
        snap("delete-alert")
        app.alerts.buttons["Cancel"].tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()

        let secondPhoto = app.scrollViews.firstMatch.buttons.element(boundBy: 1)
        XCTAssertTrue(secondPhoto.waitForExistence(timeout: 5))
        secondPhoto.press(forDuration: 1.0)
        XCTAssertTrue(app.buttons["Favorite"].waitForExistence(timeout: 3))
        snap("context-menu")
        app.buttons["Add to Board"].tap()
        XCTAssertTrue(app.buttons["New Board…"].waitForExistence(timeout: 3))
        snap("context-submenu")
        app.buttons["New Board…"].tap()
        XCTAssertTrue(app.alerts["New Board"].waitForExistence(timeout: 3))
        snap("new-board-alert")
        app.alerts.buttons["Cancel"].tap()

        // Confirm paths: a new board from the context menu shows up in Boards.
        app.scrollViews.firstMatch.buttons.element(boundBy: 2).press(forDuration: 1.0)
        XCTAssertTrue(app.buttons["Add to Board"].waitForExistence(timeout: 3))
        app.buttons["Add to Board"].tap()
        app.buttons["New Board…"].tap()
        let nameField = app.alerts["New Board"].textFields.firstMatch
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.typeText("Cliffs")
        app.alerts["New Board"].buttons["Create"].tap()

        app.navigationBars.buttons["Filter"].tap()
        XCTAssertTrue(app.staticTexts["Favorites only"].waitForExistence(timeout: 5))
        snap("filter-sheet")
        app.swipeDown(velocity: .fast)

        app.tabBars.buttons["Boards"].tap()
        XCTAssertTrue(app.staticTexts["Cliffs"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["1 photo"].exists)
    }

    func testDeletingAPhotoFromItsDetailPopsBack() {
        let photos = app.scrollViews.firstMatch.buttons
        XCTAssertTrue(photos.firstMatch.waitForExistence(timeout: 10))
        let before = photos.count
        photos.firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Santorini"].waitForExistence(timeout: 5))
        app.navigationBars.buttons["More"].tap()
        app.buttons["Delete"].tap()
        app.alerts["Delete photo?"].buttons["Delete"].tap()
        XCTAssertTrue(app.navigationBars["Gallery"].waitForExistence(timeout: 5))
        XCTAssertEqual(photos.count, before - 1)
        XCTAssertFalse(app.navigationBars["Santorini"].exists)
    }

    func testRemovingAPhotoFromItsBoardShowsOnTheBoard() {
        app.tabBars.buttons["Boards"].tap()
        let blue = app.staticTexts["Blue"]
        XCTAssertTrue(blue.waitForExistence(timeout: 5))
        blue.tap()
        XCTAssertTrue(app.navigationBars["Blue"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Santorini"].waitForExistence(timeout: 5))
        // Remove the photo from the board while the board is covered by it.
        app.buttons["Santorini"].tap()
        XCTAssertTrue(app.navigationBars["Santorini"].waitForExistence(timeout: 5))
        app.navigationBars.buttons["More"].tap()
        app.buttons["Add to Board"].tap()
        app.buttons.matching(identifier: "Blue").element(boundBy: 0).tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Blue"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Santorini"].exists)
    }

    func testBoardsAndSearch() {
        app.tabBars.buttons["Boards"].tap()
        let blue = app.staticTexts["Blue"]
        XCTAssertTrue(blue.waitForExistence(timeout: 5))
        snap("boards")
        app.cells.containing(.staticText, identifier: "Blue").firstMatch.swipeLeft()
        app.buttons["Delete"].firstMatch.tap()
        XCTAssertTrue(app.buttons["Delete Board Only"].waitForExistence(timeout: 3))
        snap("delete-board-dialog")
        // iOS 26 presents it as a popover with no Cancel button; a tap outside dismisses it.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)).tap()

        blue.tap()
        XCTAssertTrue(app.navigationBars["Blue"].waitForExistence(timeout: 5))
        app.navigationBars.buttons["Edit"].tap()
        XCTAssertTrue(app.navigationBars["Edit Board"].waitForExistence(timeout: 5))
        snap("board-editor")
        app.navigationBars["Edit Board"].buttons["Done"].tap()

        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("moun")
        snap("search")
    }

    private func snap(_ name: String) {
        guard let dir = ProcessInfo.processInfo.environment["SCREENSHOT_DIR"] else { return }
        Thread.sleep(forTimeInterval: 0.6)
        let url = URL(fileURLWithPath: dir).appendingPathComponent("\(name).png")
        try? XCUIScreen.main.screenshot().pngRepresentation.write(to: url)
    }
}
