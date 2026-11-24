import Foundation

/// The root of the tree. Decides which world the user is in: forced update, signed out, or the app.
public final class AppViewModel: ViewModel<AppViewModel.State, AppViewModel.Action, AppViewModel.Message> {
    public enum Phase: String, Equatable, Sendable {
        case checking
        case updateRequired
        case signedOut
        case main
    }

    public struct State: Equatable {
        public var phase = Phase.checking
        /// A link that arrived before there was a session, held for replay.
        var pendingRoute: Route?
    }

    public enum Action {
        case openLink(String)
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
        case started
        case updateChecked(isRequired: Bool)
        case sessionChanged(SessionStore.Status)
    }

    public let welcome: WelcomeViewModel
    public let updateRequired: UpdateRequiredViewModel
    public let stack: StackNavigator

    private let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
        welcome = WelcomeViewModel(dependencies: dependencies)
        updateRequired = UpdateRequiredViewModel(dependencies: dependencies)
        stack = StackNavigator(dependencies: dependencies)
        super.init(state: State())
        dependencies.session.onChange = { [weak self] status in self?.send(message: .sessionChanged(status)) }
    }

    override func start() {
        send(message: .started)
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case .started:
            dependencies.analytics.track(.appOpened)
            let system = dependencies.system
            let current = dependencies.config.appVersion
            run {
                try await system.minimumVersion()
            } then: { result in
                // A failed check never blocks launch; only a readable, higher minimum does.
                let minimum = try? result.get().ios
                return .updateChecked(
                    isRequired: minimum.map { UpdatePolicy.isUpdateRequired(current: current, minimum: $0) } ?? false
                )
            }

        case let .updateChecked(isRequired):
            if isRequired {
                state.phase = .updateRequired
            } else {
                enter(for: dependencies.session.status, state: &state)
            }

        case let .sessionChanged(status):
            guard state.phase == .signedOut || state.phase == .main else { return }
            enter(for: status, state: &state)

        case let .viewAction(.openLink(link)):
            guard let route = dependencies.deepLinks.route(for: link) else { return }
            dependencies.analytics.track(.deepLinkOpened(route: route.name))
            if state.phase == .main {
                stack.reset(to: route.stack)
            } else {
                state.pendingRoute = route
            }
        }
    }

    private func enter(for status: SessionStore.Status, state: inout State) {
        switch status {
        case .expired:
            state.phase = .signedOut
            dependencies.session.signOut()
        case .signedOut:
            state.phase = .signedOut
            stack.clear()
        case .signedIn:
            // Screens are built only once there is a session, so nothing fetches while signed out.
            state.phase = .main
            stack.reset(to: state.pendingRoute?.stack ?? [])
            state.pendingRoute = nil
        }
    }
}
