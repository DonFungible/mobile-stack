import AppCore
import SwiftUI

struct ItemRow: View {
    let item: Item
    let onSelect: () -> Void

    var body: some View {
        AppPressable(action: onSelect) {
            Card {
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    AppText(item.title, style: .titleM)
                    AppText(item.summary, color: AppColor.textSecondary)
                        .multilineTextAlignment(.leading)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("item-row-\(item.id)")
    }
}
