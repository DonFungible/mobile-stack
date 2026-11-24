import AppCore
import SwiftUI

struct SettingsScreen: View {
    let viewModel: SettingsViewModel

    var body: some View {
        ScreenContainer(title: "Settings") {
            VStack(spacing: Spacing.md) {
                Card {
                    AppToggle("Haptic feedback", isOn: hapticsEnabled)
                        .accessibilityIdentifier("settings-haptics")
                }
                AppPressable(action: { viewModel.send(action: .openAbout) }, label: {
                    Card { AppText("About") }
                })
                .accessibilityIdentifier("settings-about")
                Spacer()
                AppButton("Sign out", variant: .destructive) { viewModel.send(action: .signOut) }
                    .accessibilityIdentifier("settings-sign-out")
            }
            .padding(Spacing.lg)
        }
    }

    private var hapticsEnabled: Binding<Bool> {
        Binding(
            get: { viewModel.state.hapticsEnabled },
            set: { viewModel.send(action: .setHaptics(enabled: $0)) }
        )
    }
}
