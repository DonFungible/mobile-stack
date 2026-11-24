import Foundation

/// Everything the core needs from outside the process. The app passes real implementations;
/// the CLI passes fixtures. The core never reaches for a platform API itself.
public struct Platform: Sendable {
    public var config: AppConfig
    public var urlScheme: String
    public var storeURL: String
    public var transport: HTTPTransport
    public var tokens: TokenStore
    public var preferences: PreferencesStore
    public var flagProvider: FeatureFlagProvider
    public var flagOverrides: Set<FeatureFlag>
    public var haptics: HapticsPlayer
    public var external: ExternalActions
    public var analytics: Analytics
    public var reporter: ErrorReporter

    public init(
        config: AppConfig,
        urlScheme: String,
        storeURL: String,
        transport: HTTPTransport,
        tokens: TokenStore,
        preferences: PreferencesStore,
        flagProvider: FeatureFlagProvider = StaticFeatureFlagProvider(),
        flagOverrides: Set<FeatureFlag> = [],
        haptics: HapticsPlayer = NoHaptics(),
        external: ExternalActions = NoExternalActions(),
        analytics: Analytics = ConsoleAnalytics(),
        reporter: ErrorReporter = ConsoleErrorReporter()
    ) {
        self.config = config
        self.urlScheme = urlScheme
        self.storeURL = storeURL
        self.transport = transport
        self.tokens = tokens
        self.preferences = preferences
        self.flagProvider = flagProvider
        self.flagOverrides = flagOverrides
        self.haptics = haptics
        self.external = external
        self.analytics = analytics
        self.reporter = reporter
    }
}

/// The registry of every persisted key. User-scoped keys take the user id so accounts never share state.
public enum StorageKey {
    public static let hapticsEnabled = "haptics_enabled"

    public static func draft(userID: String) -> String {
        "\(userID)_draft"
    }
}

public protocol PreferencesStore: Sendable {
    func bool(for key: String) -> Bool?
    func set(_ value: Bool, for key: String)
}

public final class InMemoryPreferencesStore: PreferencesStore, @unchecked Sendable {
    private let lock = NSLock()
    private var values: [String: Bool]

    public init(_ values: [String: Bool] = [:]) {
        self.values = values
    }

    public func bool(for key: String) -> Bool? {
        lock.withLock { values[key] }
    }

    public func set(_ value: Bool, for key: String) {
        lock.withLock { values[key] = value }
    }
}

/// Semantic feedback events. Callers say what happened, not which generator to fire.
public enum HapticEvent: Sendable {
    case selection
    case success
    case failure
}

public protocol HapticsPlayer: Sendable {
    func play(_ event: HapticEvent)
}

public struct NoHaptics: HapticsPlayer {
    public init() {}
    public func play(_: HapticEvent) {}
}

/// Hand-offs that leave the app: opening a URL, the share sheet.
public protocol ExternalActions: Sendable {
    func open(url: String)
    func share(text: String)
}

public struct NoExternalActions: ExternalActions {
    public init() {}
    public func open(url _: String) {}
    public func share(text _: String) {}
}
