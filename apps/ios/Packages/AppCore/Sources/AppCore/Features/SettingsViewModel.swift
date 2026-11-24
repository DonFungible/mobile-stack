import Foundation

public final class SettingsViewModel: ViewModel<
    SettingsViewModel.State, SettingsViewModel.Action, SettingsViewModel.Message
> {
    public struct State: Equatable {
        public var hapticsEnabled: Bool
    }

    public enum Action {
        case setHaptics(enabled: Bool)
        case openAbout
        case signOut
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
    }

    private let dependencies: Dependencies
    private weak var navigation: Navigating?

    init(dependencies: Dependencies, navigation: Navigating) {
        self.dependencies = dependencies
        self.navigation = navigation
        super.init(state: State(hapticsEnabled: dependencies.haptics.isEnabled))
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case let .viewAction(.setHaptics(enabled)):
            dependencies.preferences.set(enabled, for: StorageKey.hapticsEnabled)
            state.hapticsEnabled = enabled
        case .viewAction(.openAbout):
            navigation?.push(.about)
        case .viewAction(.signOut):
            dependencies.analytics.track(.signedOut)
            dependencies.session.signOut()
        }
    }
}
