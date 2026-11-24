import AppCore
import SwiftUI

@main
struct MobileStackApp: App {
    @State private var core = AppHost.makeCore()

    var body: some Scene {
        WindowGroup {
            RootView(app: core.app)
                .preferredColorScheme(.dark)
                .task { DebugIntrospection.start(core: core) }
        }
    }
}
