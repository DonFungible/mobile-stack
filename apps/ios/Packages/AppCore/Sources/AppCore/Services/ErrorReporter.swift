import Foundation

/// The seam for Sentry, Crashlytics or similar. Features never import a vendor SDK directly.
public protocol ErrorReporter: Sendable {
    func report(_ error: Error, context: [String: String])
}

public struct ConsoleErrorReporter: ErrorReporter {
    public init() {}

    public func report(_ error: Error, context: [String: String]) {
        let details = context.sorted { $0.key < $1.key }.map { "\($0.key)=\($0.value)" }.joined(separator: " ")
        Log.error("reported: \(error) \(details)")
    }
}
