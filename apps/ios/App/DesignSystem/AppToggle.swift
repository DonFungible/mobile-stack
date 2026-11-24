import SwiftUI

struct AppToggle: View {
    private let title: String
    @Binding private var isOn: Bool

    init(_ title: String, isOn: Binding<Bool>) {
        self.title = title
        _isOn = isOn
    }

    var body: some View {
        Toggle(isOn: $isOn) {
            AppText(title)
        }
        .tint(AppColor.accent)
        .frame(minHeight: HitTarget.minimum)
    }
}
