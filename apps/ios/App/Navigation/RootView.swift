import AppCore
import SwiftUI

/// Draws whichever world the app root says the user is in. The decision itself lives in the core.
struct RootView: View {
    let app: AppViewModel

    var body: some View {
        Group {
            switch app.state.phase {
            case .checking:
                LoadingState().background(AppColor.background)
            case .updateRequired:
                UpdateRequiredScreen(viewModel: app.updateRequired)
            case .signedOut:
                WelcomeScreen(viewModel: app.welcome)
            case .main:
                MainStack(navigator: app.stack)
            }
        }
        .onOpenURL { app.send(action: .openLink($0.absoluteString)) }
    }
}

private struct MainStack: View {
    let navigator: StackNavigator

    var body: some View {
        NavigationStack(path: path) {
            if let root = navigator.state.entries.first {
                screen(for: root).navigationDestination(for: Int.self) { id in
                    if let entry = navigator.state.entries.first(where: { $0.id == id }) {
                        screen(for: entry)
                    }
                }
            }
        }
        .tint(AppColor.accent)
    }

    /// The navigator owns the stack. The system's back button and swipe report themselves as an action.
    private var path: Binding<[Int]> {
        Binding(
            get: { navigator.state.entries.dropFirst().map(\.id) },
            set: { navigator.send(action: .popTo(depth: $0.count)) }
        )
    }

    @ViewBuilder
    private func screen(for entry: StackNavigator.Entry) -> some View {
        switch entry.screen {
        case let .itemList(viewModel): ItemListScreen(viewModel: viewModel)
        case let .itemDetail(viewModel): ItemDetailScreen(viewModel: viewModel)
        case let .settings(viewModel): SettingsScreen(viewModel: viewModel)
        case let .about(viewModel): AboutScreen(viewModel: viewModel)
        }
    }
}
