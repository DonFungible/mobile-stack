import Foundation

public enum Route: Hashable, Sendable {
    case home
    case itemDetail(id: Int)
    case settings
    case about

    /// Matches `route` in packages/contracts/deeplinks.json, shared with Android.
    public var name: String {
        switch self {
        case .home: "home"
        case .itemDetail: "itemDetail"
        case .settings: "settings"
        case .about: "about"
        }
    }

    /// The screens pushed above the root, so a deep link lands with a working back stack.
    public var stack: [Route] {
        switch self {
        case .home: []
        case .itemDetail, .settings: [self]
        case .about: [.settings, .about]
        }
    }
}
