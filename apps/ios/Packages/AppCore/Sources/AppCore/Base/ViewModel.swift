import Foundation
import Observation

/// The internal alphabet of a view model: every `Action` wrapped as `.viewAction`, plus results from services.
public protocol ViewMessage<Action> {
    associatedtype Action
    static func viewAction(_ action: Action) -> Self
}

/// Action in, state out. A view reads `state` and calls `send(action:)`; that is the whole contract.
///
/// Everything else here is internal to the core module on purpose: a view cannot write state, send a
/// message, or call `update`, because it cannot see them.
@MainActor
@Observable
public class ViewModel<State: Equatable, Action, Message: ViewMessage<Action>> {
    public private(set) var state: State

    @ObservationIgnored private var queue: [Message] = []
    @ObservationIgnored private var isUpdating = false

    init(state: State) {
        self.state = state
    }

    public func send(action: Action) {
        send(message: .viewAction(action))
    }

    /// Messages sent while an update is running are queued, so `update` never re-enters itself.
    func send(message: Message) {
        queue.append(message)
        guard !isUpdating else { return }
        isUpdating = true
        defer { isUpdating = false }
        while !queue.isEmpty {
            var draft = state
            update(state: &draft, message: queue.removeFirst())
            if draft != state {
                state = draft
            }
        }
    }

    /// The only place state ever changes.
    func update(state _: inout State, message _: Message) {}

    /// Called once, when the screen enters the tree.
    func start() {}

    /// Runs async work off the reducer and feeds its result back in as a message.
    func run<Value: Sendable>(
        _ work: @escaping @Sendable () async throws -> Value,
        then message: @escaping @MainActor (Result<Value, APIError>) -> Message
    ) {
        Task { [weak self] in
            let result: Result<Value, APIError>
            do {
                result = try await .success(work())
            } catch APIError.cancelled {
                return
            } catch let error as APIError {
                result = .failure(error)
            } catch {
                result = .failure(.network)
            }
            self?.send(message: message(result))
        }
    }
}

public enum LoadStatus: String, Equatable, Sendable {
    case loading
    case loaded
    case failed
}
