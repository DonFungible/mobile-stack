import AppCore
import SwiftUI

struct UpdateRequiredScreen: View {
    let viewModel: UpdateRequiredViewModel

    var body: some View {
        MessageState(
            title: "Update required",
            message: "This version is no longer supported. Update to keep going.",
            actionTitle: "Open the App Store",
            action: { viewModel.send(action: .openStore) }
        )
        .background(AppColor.background)
    }
}
