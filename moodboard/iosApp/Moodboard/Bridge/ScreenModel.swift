import MoodboardShared
import Observation

/// One SwiftUI screen's view of a shared Kotlin ViewModel. It mirrors the ViewModel's
/// StateFlow into `state` for as long as the screen exists, and clears its SwiftViewModelStore
/// when the screen goes away, which cancels the ViewModel's scope.
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
    @ObservationIgnored private var collecting: Task<Void, Never>?
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

    /// Starts mirroring the state. Not tied to the view's `.task`, which SwiftUI cancels whenever
    /// another screen is pushed over this one: a covered board kept showing a photo removed from
    /// the screen above it, and only caught up after the zoom back had landed on the stale grid.
    func observe() async {
        guard collecting == nil else { return }
        let flow = binding().flow
        collecting = Task { [weak self] in
            for await value in flow {
                self?.latest = value
            }
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

    isolated deinit {
        collecting?.cancel()
        store?.clear()
    }
}
