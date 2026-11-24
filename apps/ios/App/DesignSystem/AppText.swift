import SwiftUI

enum AppTextStyle {
    case display
    case headline
    case titleL
    case titleM
    case body
    case label
    case labelS

    var token: TypeToken {
        switch self {
        case .display: TypeScale.display
        case .headline: TypeScale.headline
        case .titleL: TypeScale.titleL
        case .titleM: TypeScale.titleM
        case .body: TypeScale.body
        case .label: TypeScale.label
        case .labelS: TypeScale.labelS
        }
    }

    var weight: Font.Weight {
        switch token.weight {
        case ..<450: .regular
        case ..<550: .medium
        case ..<650: .semibold
        default: .bold
        }
    }

    var isHeader: Bool {
        switch self {
        case .display, .headline, .titleL: true
        case .titleM, .body, .label, .labelS: false
        }
    }
}

struct AppText: View {
    private let text: String
    private let style: AppTextStyle
    private let color: Color

    /// Scales with Dynamic Type while keeping the token's size as the default.
    @ScaledMetric private var scale: CGFloat = 1

    init(_ text: String, style: AppTextStyle = .body, color: Color = AppColor.textPrimary) {
        self.text = text
        self.style = style
        self.color = color
    }

    var body: some View {
        let token = style.token
        Text(text)
            .font(.system(size: token.size * scale, weight: style.weight))
            .lineSpacing((token.lineHeight - token.size) * scale)
            .foregroundStyle(color)
            .accessibilityAddTraits(style.isHeader ? .isHeader : [])
    }
}
