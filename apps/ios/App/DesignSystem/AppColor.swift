import SwiftUI

enum AppColor {
    static let accent = Color(argb: ColorToken.accent)
    static let accentPressed = Color(argb: ColorToken.accentPressed)
    static let onAccent = Color(argb: ColorToken.onAccent)
    static let background = Color(argb: ColorToken.background)
    static let surface = Color(argb: ColorToken.surface)
    static let surfaceRaised = Color(argb: ColorToken.surfaceRaised)
    static let stroke = Color(argb: ColorToken.stroke)
    static let textPrimary = Color(argb: ColorToken.textPrimary)
    static let textSecondary = Color(argb: ColorToken.textSecondary)
    static let textDisabled = Color(argb: ColorToken.textDisabled)
    static let success = Color(argb: ColorToken.success)
    static let warning = Color(argb: ColorToken.warning)
    static let danger = Color(argb: ColorToken.danger)
}

private extension Color {
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}
