import SwiftUI

struct LoadingState: View {
    var body: some View {
        ProgressView()
            .tint(AppColor.textSecondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel("Loading")
    }
}

struct MessageState: View {
    private let title: String
    private let message: String
    private let actionTitle: String?
    private let action: (() -> Void)?

    init(title: String, message: String, actionTitle: String? = nil, action: (() -> Void)? = nil) {
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: Spacing.md) {
            AppText(title, style: .titleL)
            AppText(message, color: AppColor.textSecondary).multilineTextAlignment(.center)
            if let actionTitle, let action {
                AppButton(actionTitle, variant: .secondary, action: action)
                    .padding(.top, Spacing.sm)
            }
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
