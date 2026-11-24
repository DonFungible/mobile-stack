import AppCore
import SwiftUI

struct AboutScreen: View {
    let viewModel: AboutViewModel

    var body: some View {
        let state = viewModel.state
        ScreenContainer(title: "About") {
            Card {
                VStack(alignment: .leading, spacing: Spacing.sm) {
                    row("Version", state.version)
                    row("Environment", state.environment)
                    row("Data", state.dataSource)
                }
            }
            .padding(Spacing.lg)
        }
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            AppText(title, color: AppColor.textSecondary)
            Spacer()
            AppText(value)
        }
        .accessibilityElement(children: .combine)
    }
}
