import AppCore
import UIKit

// The platform capabilities the core asks for. This folder and the views are the only code that touches UIKit.

nonisolated struct UserDefaultsPreferencesStore: PreferencesStore, @unchecked Sendable {
    private let defaults = UserDefaults.standard

    func bool(for key: String) -> Bool? {
        defaults.object(forKey: key) as? Bool
    }

    func set(_ value: Bool, for key: String) {
        defaults.set(value, forKey: key)
    }
}

nonisolated struct SystemHaptics: HapticsPlayer {
    func play(_ event: HapticEvent) {
        Task { @MainActor in
            switch event {
            case .selection: UISelectionFeedbackGenerator().selectionChanged()
            case .success: UINotificationFeedbackGenerator().notificationOccurred(.success)
            case .failure: UINotificationFeedbackGenerator().notificationOccurred(.error)
            }
        }
    }
}

nonisolated struct SystemExternalActions: ExternalActions {
    func open(url: String) {
        Task { @MainActor in
            guard let url = URL(string: url) else { return }
            UIApplication.shared.open(url)
        }
    }

    func share(text: String) {
        Task { @MainActor in
            let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
            var presenter = scene?.keyWindow?.rootViewController
            while let presented = presenter?.presentedViewController {
                presenter = presented
            }
            presenter?.present(
                UIActivityViewController(activityItems: [text], applicationActivities: nil),
                animated: true
            )
        }
    }
}
