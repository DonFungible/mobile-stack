import Foundation

/// The screens currently on the stack, each with its view model. Navigation is state: the CLI can walk
/// the whole app without a single view being created.
public final class StackNavigator: ViewModel<StackNavigator.State, StackNavigator.Action, StackNavigator.Message>,
    Navigating
{
    public enum Screen {
        case itemList(ItemListViewModel)
        case itemDetail(ItemDetailViewModel)
        case settings(SettingsViewModel)
        case about(AboutViewModel)
    }

    public struct Entry: Identifiable, Equatable {
        public let id: Int
        public let route: Route
        public let screen: Screen

        public static func == (lhs: Entry, rhs: Entry) -> Bool {
            lhs.id == rhs.id
        }
    }

    public struct State: Equatable {
        /// Empty while signed out; otherwise the first entry is the home screen.
        public var entries: [Entry] = []

        public var top: Entry? {
            entries.last
        }
    }

    public enum Action {
        case goBack
        /// Keeps `depth` screens above the root. The platform's own back gesture reports itself this way.
        case popTo(depth: Int)
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
        case push(Route)
        case reset([Route])
        case clear
    }

    private let dependencies: Dependencies
    private var nextID = 0

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
        super.init(state: State())
    }

    func push(_ route: Route) {
        send(message: .push(route))
    }

    func reset(to routes: [Route]) {
        send(message: .reset(routes))
    }

    func clear() {
        send(message: .clear)
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case .viewAction(.goBack):
            if state.entries.count > 1 {
                state.entries.removeLast()
            }
        case let .viewAction(.popTo(depth)):
            let keep = max(1, min(state.entries.count, depth + 1))
            state.entries.removeLast(state.entries.count - keep)
        case let .push(route):
            state.entries.append(makeEntry(for: route))
        case let .reset(routes):
            state.entries = ([.home] + routes).map(makeEntry)
        case .clear:
            state.entries = []
        }
    }

    private func makeEntry(for route: Route) -> Entry {
        nextID += 1
        let screen: Screen
        switch route {
        case .home:
            let viewModel = ItemListViewModel(dependencies: dependencies, navigation: self)
            viewModel.start()
            screen = .itemList(viewModel)
        case let .itemDetail(id):
            let viewModel = ItemDetailViewModel(itemID: id, dependencies: dependencies)
            viewModel.start()
            screen = .itemDetail(viewModel)
        case .settings:
            screen = .settings(SettingsViewModel(dependencies: dependencies, navigation: self))
        case .about:
            screen = .about(AboutViewModel(dependencies: dependencies))
        }
        return Entry(id: nextID, route: route, screen: screen)
    }
}
