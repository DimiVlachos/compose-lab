import MoodboardShared
import Observation

/// One SwiftUI screen's view of a shared Kotlin ViewModel. It owns the screen's
/// SwiftViewModelStore, mirrors the ViewModel's StateFlow into `state`, and clears the
/// store when the screen goes away, which cancels the ViewModel's scope.
@MainActor
@Observable
final class ScreenModel<ViewModel: AnyObject, State: AnyObject> {
    let viewModel: ViewModel
    private(set) var state: State
    @ObservationIgnored private let store: SwiftViewModelStore
    @ObservationIgnored private let flow: SkieSwiftStateFlow<State>

    init(
        _ make: (SwiftViewModelStore) -> ViewModel,
        state: (ViewModel) -> SkieSwiftStateFlow<State>
    ) {
        let store = SwiftViewModelStore()
        self.store = store
        viewModel = make(store)
        flow = state(viewModel)
        self.state = flow.value
    }

    /// Run from the screen's `.task`, so collection stops when the view disappears.
    func observe() async {
        for await value in flow {
            state = value
        }
    }

    deinit {
        store.clear()
    }
}
