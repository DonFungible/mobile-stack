import Foundation

public final class UpdateRequiredViewModel: ViewModel<
    UpdateRequiredViewModel.State, UpdateRequiredViewModel.Action, UpdateRequiredViewModel.Message
> {
    public struct State: Equatable {
        public let storeURL: String
    }

    public enum Action {
        case openStore
    }

    public enum Message: ViewMessage {
        case viewAction(Action)
    }

    private let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
        super.init(state: State(storeURL: dependencies.storeURL))
    }

    override func update(state: inout State, message: Message) {
        switch message {
        case .viewAction(.openStore):
            dependencies.external.open(url: state.storeURL)
        }
    }
}
