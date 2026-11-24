import Foundation

public final class WelcomeViewModel: ViewModel<
    WelcomeViewModel.State,
    WelcomeViewModel.Action,
    WelcomeViewModel.Message
> {
    public struct State: Equatable {}

    public enum Action {
        case continueAsGuest
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
    }

    private let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
        super.init(state: State())
    }

    override func update(state _: inout State, message: Message) {
        switch message {
        case .viewAction(.continueAsGuest):
            dependencies.haptics.play(.success)
            dependencies.session.signIn(token: "guest-\(UUID().uuidString)")
            dependencies.analytics.track(.signedIn(method: "guest"))
        }
    }
}
