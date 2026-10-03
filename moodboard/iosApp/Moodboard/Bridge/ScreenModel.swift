import MoodboardShared
import Observation

/// One SwiftUI screen's view of a shared Kotlin ViewModel. It mirrors the ViewModel's
/// StateFlow into `state` and clears its SwiftViewModelStore when the screen goes away, which
/// cancels the ViewModel's scope.
///
/// SwiftUI builds a fresh `@State` initial value each time a parent re-renders and throws all
/// but the first away, so the store and ViewModel are created on first use: a discarded
/// instance costs two closures, not a ViewModel.
@MainActor
@Observable
final class ScreenModel<ViewModel: AnyObject, State: AnyObject> {
    @ObservationIgnored private let make: (SwiftViewModelStore) -> ViewModel
    @ObservationIgnored private let stateFlow: (ViewModel) -> SkieSwiftStateFlow<State>
    @ObservationIgnored private var store: SwiftViewModelStore?
    @ObservationIgnored private var bound: (viewModel: ViewModel, flow: SkieSwiftStateFlow<State>)?
    private var latest: State?

    init(
        _ make: @escaping (SwiftViewModelStore) -> ViewModel,
        state: @escaping (ViewModel) -> SkieSwiftStateFlow<State>
    ) {
        self.make = make
        stateFlow = state
    }

    var viewModel: ViewModel { binding().viewModel }

    var state: State { latest ?? binding().flow.value }

    /// Run from the screen's `.task`, so collection stops when the view disappears.
    func observe() async {
        for await value in binding().flow {
            latest = value
        }
    }

    private func binding() -> (viewModel: ViewModel, flow: SkieSwiftStateFlow<State>) {
        if let bound { return bound }
        let store = SwiftViewModelStore()
        let viewModel = make(store)
        let made = (viewModel: viewModel, flow: stateFlow(viewModel))
        self.store = store
        bound = made
        return made
    }

    deinit {
        store?.clear()
    }
}
