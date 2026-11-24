import Foundation

@MainActor
public final class SessionStore {
    public enum Status: String, Equatable, Sendable {
        case signedOut
        case signedIn
        case expired
    }

    public private(set) var status: Status {
        didSet {
            if status != oldValue {
                onChange?(status)
            }
        }
    }

    var onChange: (@MainActor (Status) -> Void)?

    private let tokens: TokenStore
    private var onSignOut: [@MainActor () -> Void] = []

    public init(tokens: TokenStore) {
        self.tokens = tokens
        status = tokens.token == nil ? .signedOut : .signedIn
    }

    public func signIn(token: String) {
        tokens.save(token)
        status = .signedIn
    }

    /// Called by the API layer on a 401; the app root reacts by signing out.
    public func markExpired() {
        guard status == .signedIn else { return }
        status = .expired
    }

    public func signOut() {
        tokens.clear()
        onSignOut.forEach { $0() }
        status = .signedOut
    }

    /// Every cache or store holding user data registers its reset here, so sign-out cannot miss one.
    public func registerSignOutCleanup(_ cleanup: @escaping @MainActor () -> Void) {
        onSignOut.append(cleanup)
    }
}
