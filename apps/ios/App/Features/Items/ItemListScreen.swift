import AppCore
import SwiftUI

struct ItemListScreen: View {
    let viewModel: ItemListViewModel

    var body: some View {
        let state = viewModel.state
        ScreenContainer(title: "Items") {
            LoadStatusView(
                status: state.status,
                errorMessage: state.errorMessage,
                isRefreshing: { viewModel.state.isRefreshing },
                onRefresh: { viewModel.send(action: .refresh) },
                content: {
                    if state.items.isEmpty {
                        MessageState(title: "Nothing here yet", message: "Items you add will show up here.")
                    } else {
                        list(of: state.items)
                    }
                }
            )
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                AppIconButton(systemImage: "gearshape", label: "Settings") {
                    viewModel.send(action: .openSettings)
                }
                .accessibilityIdentifier("settings-button")
            }
        }
    }

    private func list(of items: [Item]) -> some View {
        ScrollView {
            LazyVStack(spacing: Spacing.md) {
                ForEach(items) { item in
                    ItemRow(item: item) { viewModel.send(action: .selectItem(id: item.id)) }
                }
            }
            .padding(Spacing.lg)
        }
        .accessibilityIdentifier("items-list")
    }
}
