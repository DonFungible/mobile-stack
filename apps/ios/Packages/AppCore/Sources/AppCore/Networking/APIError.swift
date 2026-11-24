import Foundation

public enum APIError: Error, Equatable, Sendable {
    case network
    case unauthorized
    case http(status: Int, requestID: String)
    case decoding(requestID: String)
    case cancelled

    /// Only server faults and contract breaks are worth an alert; offline users and 4xx are not.
    public var isReportable: Bool {
        switch self {
        case let .http(status, _): status >= 500
        case .decoding: true
        case .network, .unauthorized, .cancelled: false
        }
    }

    public var userMessage: String {
        switch self {
        case .network: "You appear to be offline. Check your connection and try again."
        case .unauthorized: "Your session has expired. Please sign in again."
        case .http, .decoding, .cancelled: "Something went wrong. Please try again."
        }
    }
}
