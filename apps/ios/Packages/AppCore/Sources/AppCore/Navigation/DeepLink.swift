import Foundation

public struct DeepLinkParser: Sendable {
    private let scheme: String
    private let universalLinkHosts: Set<String>

    public init(scheme: String, universalLinkHosts: Set<String>) {
        self.scheme = scheme
        self.universalLinkHosts = universalLinkHosts
    }

    /// Returns `nil` for anything unrecognized; unknown links are ignored, never guessed at.
    public func route(for link: String) -> Route? {
        guard let url = URL(string: link), let segments = segments(of: url) else { return nil }
        switch segments.count {
        case 1 where segments[0] == "home":
            return .home
        case 1 where segments[0] == "settings":
            return .settings
        case 2 where segments[0] == "settings" && segments[1] == "about":
            return .about
        case 2 where segments[0] == "items":
            guard let id = Int(segments[1]), id > 0, String(id) == segments[1] else { return nil }
            return .itemDetail(id: id)
        default:
            return nil
        }
    }

    private func segments(of url: URL) -> [String]? {
        let pathSegments = url.pathComponents.filter { $0 != "/" }
        switch url.scheme {
        case scheme:
            guard let host = url.host(), !host.isEmpty else { return nil }
            return [host] + pathSegments
        case "https":
            guard let host = url.host(), universalLinkHosts.contains(host) else { return nil }
            return pathSegments
        default:
            return nil
        }
    }
}
