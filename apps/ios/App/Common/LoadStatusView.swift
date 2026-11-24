import AppCore
import SwiftUI

/// Renders the loading and failure states every loaded screen shares, so each screen only draws its content.
struct LoadStatusView<Content: View>: View {
    private let status: LoadStatus
    private let errorMessage: String?
    private let isRefreshing: () -> Bool
    private let onRefresh: () -> Void
    private let content: Content

    init(
        status: LoadStatus,
        errorMessage: String?,
        isRefreshing: @escaping () -> Bool,
        onRefresh: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.status = status
        self.errorMessage = errorMessage
        self.isRefreshing = isRefreshing
        self.onRefresh = onRefresh
        self.content = content()
    }

    var body: some View {
        switch status {
        case .loading:
            LoadingState()
        case .failed:
            MessageState(
                title: "Couldn't load",
                message: errorMessage ?? "",
                actionTitle: "Try again",
                action: onRefresh
            )
        case .loaded:
            content.refreshable {
                onRefresh()
                // The pull indicator stays until the view model reports the refresh finished.
                repeat {
                    try? await Task.sleep(for: .milliseconds(50))
                } while isRefreshing() && !Task.isCancelled
            }
        }
    }
}
