import AppCore
import SwiftUI

struct ItemDetailScreen: View {
    let viewModel: ItemDetailViewModel

    var body: some View {
        let state = viewModel.state
        ScreenContainer(title: "Item") {
            LoadStatusView(
                status: state.status,
                errorMessage: state.errorMessage,
                isRefreshing: { viewModel.state.isRefreshing },
                onRefresh: { viewModel.send(action: .refresh) },
                content: {
                    if let item = state.item {
                        detail(of: item, canShare: state.canShare)
                    }
                }
            )
        }
    }

    private func detail(of item: Item, canShare: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.md) {
                AppText(item.title, style: .headline)
                    .accessibilityIdentifier("item-detail-title")
                AppText(item.summary, color: AppColor.textSecondary)
                AppText(
                    "Updated \(item.updatedAt.formatted(date: .abbreviated, time: .omitted))",
                    style: .label,
                    color: AppColor.textSecondary
                )
                if canShare {
                    AppButton("Share", variant: .secondary) { viewModel.send(action: .share) }
                        .accessibilityIdentifier("item-detail-share")
                }
            }
            .padding(Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
