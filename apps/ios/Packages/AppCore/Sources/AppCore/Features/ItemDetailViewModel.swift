import Foundation

public final class ItemDetailViewModel: ViewModel<
    ItemDetailViewModel.State, ItemDetailViewModel.Action, ItemDetailViewModel.Message
> {
    public struct State: Equatable {
        public let itemID: Int
        public let canShare: Bool
        public var status = LoadStatus.loading
        public var item: Item?
        public var isRefreshing = false
        public var errorMessage: String?
    }

    public enum Action {
        case refresh
        case share
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
        case started
        case loaded(Result<Item, APIError>)
    }

    private let dependencies: Dependencies

    init(itemID: Int, dependencies: Dependencies) {
        self.dependencies = dependencies
        super.init(state: State(itemID: itemID, canShare: dependencies.flags.isEnabled(.itemSharing)))
    }

    override func start() {
        send(message: .started)
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case .started:
            dependencies.analytics.track(.itemViewed(id: state.itemID))
            load(id: state.itemID, force: false)

        case .viewAction(.refresh):
            if state.status == .loaded {
                state.isRefreshing = true
            } else {
                state.status = .loading
                state.errorMessage = nil
            }
            load(id: state.itemID, force: true)

        case let .loaded(.success(item)):
            state.status = .loaded
            state.item = item
            state.isRefreshing = false
            state.errorMessage = nil

        case let .loaded(.failure(error)):
            state.isRefreshing = false
            guard state.status != .loaded else { return }
            state.status = .failed
            state.errorMessage = error.userMessage

        case .viewAction(.share):
            guard state.canShare, let item = state.item else { return }
            dependencies.external.share(text: item.title)
        }
    }

    private func load(id: Int, force: Bool) {
        let queries = dependencies.queries
        let repository = dependencies.items
        run(
            { try await queries.fetch(.item(id: id), force: force) { try await repository.item(id: id) } },
            then: Message.loaded
        )
    }
}
