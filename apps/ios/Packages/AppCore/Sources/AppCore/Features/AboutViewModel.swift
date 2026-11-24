import Foundation

public final class AboutViewModel: ViewModel<AboutViewModel.State, AboutViewModel.Action, AboutViewModel.Message> {
    public struct State: Equatable {
        public let version: String
        public let environment: String
        public let dataSource: String
    }

    /// Nothing to do on this screen; leaving it is the navigator's `goBack`.
    public enum Action {}

    public enum Message: ViewMessage {
        case viewAction(Action)
    }

    init(dependencies: Dependencies) {
        let config = dependencies.config
        super.init(state: State(
            version: config.appVersion,
            environment: config.environment.rawValue,
            dataSource: config.useFixtures ? "Bundled fixtures" : "Live API"
        ))
    }
}
