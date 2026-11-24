import Foundation

/// The single catalog of events. Names are snake_case; properties are typed here, not at call sites.
public enum AnalyticsEvent: Equatable, Sendable {
    case appOpened
    case signedIn(method: String)
    case signedOut
    case itemViewed(id: Int)
    case deepLinkOpened(route: String)

    public var name: String {
        switch self {
        case .appOpened: "app_opened"
        case .signedIn: "signed_in"
        case .signedOut: "signed_out"
        case .itemViewed: "item_viewed"
        case .deepLinkOpened: "deep_link_opened"
        }
    }

    public var properties: [String: String] {
        switch self {
        case .appOpened, .signedOut: [:]
        case let .signedIn(method): ["method": method]
        case let .itemViewed(id): ["item_id": String(id)]
        case let .deepLinkOpened(route): ["route": route]
        }
    }
}

/// The seam for PostHog, Firebase or similar.
public protocol Analytics: Sendable {
    func track(_ event: AnalyticsEvent)
    func identify(userID: String)
    func reset()
}

public struct ConsoleAnalytics: Analytics {
    public init() {}

    public func track(_ event: AnalyticsEvent) {
        Log.debug("analytics: \(event.name) \(event.properties)")
    }

    public func identify(userID: String) {
        Log.debug("analytics: identify \(userID)")
    }

    public func reset() {
        Log.debug("analytics: reset")
    }
}
