import Foundation

/// What view models are constructed with. Built once by the core from the platform it was given.
@MainActor
struct Dependencies {
    let config: AppConfig
    let storeURL: String
    let session: SessionStore
    let queries: QueryClient
    let items: ItemsRepository
    let system: SystemRepository
    let flags: FeatureFlags
    let deepLinks: DeepLinkParser
    let preferences: PreferencesStore
    let haptics: Haptics
    let external: ExternalActions
    let analytics: Analytics
}

/// Plays feedback only when the user has not turned it off, so the preference is checked in one place.
struct Haptics: Sendable {
    let player: HapticsPlayer
    let preferences: PreferencesStore

    var isEnabled: Bool {
        preferences.bool(for: StorageKey.hapticsEnabled) ?? true
    }

    func play(_ event: HapticEvent) {
        if isEnabled {
            player.play(event)
        }
    }
}

/// How a screen asks to move. The stack navigator implements it; pushing is a state change, not a view.
@MainActor
protocol Navigating: AnyObject {
    func push(_ route: Route)
}
