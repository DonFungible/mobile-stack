import Foundation

/// Where the session token lives. The app passes a Keychain-backed store; the CLI passes this one.
public protocol TokenStore: Sendable {
    var token: String? { get }
    func save(_ token: String)
    func clear()
}

public final class InMemoryTokenStore: TokenStore, @unchecked Sendable {
    private let lock = NSLock()
    private var value: String?

    public init(token: String? = nil) {
        value = token
    }

    public var token: String? {
        lock.withLock { value }
    }

    public func save(_ token: String) {
        lock.withLock { value = token }
    }

    public func clear() {
        lock.withLock { value = nil }
    }
}
