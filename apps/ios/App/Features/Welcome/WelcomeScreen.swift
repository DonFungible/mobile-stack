import AppCore
import SwiftUI

struct WelcomeScreen: View {
    let viewModel: WelcomeViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Spacer()
            AppText("MobileStack", style: .display)
            AppText(
                "A native starting point with the boring decisions already made.",
                color: AppColor.textSecondary
            )
            Spacer()
            AppButton("Continue as guest") { viewModel.send(action: .continueAsGuest) }
                .accessibilityIdentifier("welcome-continue")
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(AppColor.background)
    }
}
