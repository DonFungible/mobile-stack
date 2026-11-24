import Foundation

public final class ItemListViewModel: ViewModel<
    ItemListViewModel.State, ItemListViewModel.Action, ItemListViewModel.Message
> {
    public struct State: Equatable {
        public var status = LoadStatus.loading
        public var items: [Item] = []
        public var isRefreshing = false
        public var errorMessage: String?
    }

    public enum Action {
        case refresh
        case selectItem(id: Int)
        case openSettings
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
        case started
        case loaded(Result<[Item], APIError>)
    }

    private let dependencies: Dependencies
    private weak var navigation: Navigating?

    init(dependencies: Dependencies, navigation: Navigating) {
        self.dependencies = dependencies
        self.navigation = navigation
        super.init(state: State())
    }

    override func start() {
        send(message: .started)
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case .started:
            load(force: false)

        case .viewAction(.refresh):
            // A refresh keeps showing the last good value; a retry after failure shows loading again.
            if state.status == .loaded {
                state.isRefreshing = true
            } else {
                state.status = .loading
                state.errorMessage = nil
            }
            load(force: true)

        case let .loaded(.success(items)):
            state.status = .loaded
            state.items = items
            state.isRefreshing = false
            state.errorMessage = nil

        case let .loaded(.failure(error)):
            state.isRefreshing = false
            guard state.status != .loaded else { return }
            state.status = .failed
            state.errorMessage = error.userMessage

        case let .viewAction(.selectItem(id)):
            guard state.items.contains(where: { $0.id == id }) else { return }
            dependencies.haptics.play(.selection)
            navigation?.push(.itemDetail(id: id))

        case .viewAction(.openSettings):
            navigation?.push(.settings)
        }
    }

    private func load(force: Bool) {
        let queries = dependencies.queries
        let repository = dependencies.items
        run({ try await queries.fetch(.items(), force: force, repository.items) }, then: Message.loaded)
    }
}
