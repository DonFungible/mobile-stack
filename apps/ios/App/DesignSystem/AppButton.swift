import SwiftUI

enum AppButtonVariant {
    case primary
    case secondary
    case destructive
}

struct AppButton: View {
    private let title: String
    private let variant: AppButtonVariant
    private let isLoading: Bool
    private let action: () -> Void

    @Environment(\.isEnabled) private var isEnabled

    init(
        _ title: String,
        variant: AppButtonVariant = .primary,
        isLoading: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.variant = variant
        self.isLoading = isLoading
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            ZStack {
                AppText(title, style: .titleM, color: foreground).opacity(isLoading ? 0 : 1)
                if isLoading {
                    ProgressView().tint(foreground)
                }
            }
            .frame(maxWidth: .infinity, minHeight: HitTarget.minimum)
            .padding(.horizontal, Spacing.lg)
            .background(background, in: .rect(cornerRadius: Radius.xl))
            .overlay {
                if variant == .secondary {
                    RoundedRectangle(cornerRadius: Radius.xl).strokeBorder(AppColor.stroke)
                }
            }
        }
        .buttonStyle(PressableStyle())
        .disabled(isLoading)
        .accessibilityLabel(title)
    }

    private var foreground: Color {
        guard isEnabled else { return AppColor.textDisabled }
        return switch variant {
        case .primary: AppColor.onAccent
        case .secondary: AppColor.textPrimary
        case .destructive: AppColor.danger
        }
    }

    private var background: Color {
        guard isEnabled else { return AppColor.surfaceRaised }
        return switch variant {
        case .primary: AppColor.accent
        case .secondary, .destructive: AppColor.surface
        }
    }
}

struct AppIconButton: View {
    private let systemImage: String
    private let label: String
    private let action: () -> Void

    /// `label` is required: an icon-only control is invisible to VoiceOver without one.
    init(systemImage: String, label: String, action: @escaping () -> Void) {
        self.systemImage = systemImage
        self.label = label
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .foregroundStyle(AppColor.textPrimary)
                .frame(minWidth: HitTarget.minimum, minHeight: HitTarget.minimum)
                .contentShape(.rect)
        }
        .buttonStyle(PressableStyle())
        .accessibilityLabel(label)
    }
}

enum HitTarget {
    static let minimum: CGFloat = 44
}

/// The one press treatment in the app: dim on touch, fade back on release.
struct PressableStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.6 : 1)
            .animation(reduceMotion ? nil : .easeOut(duration: Motion.pressFade), value: configuration.isPressed)
    }
}

/// Makes a whole row tappable with the standard press treatment.
struct AppPressable<Label: View>: View {
    private let action: () -> Void
    private let label: Label

    init(action: @escaping () -> Void, @ViewBuilder label: () -> Label) {
        self.action = action
        self.label = label()
    }

    var body: some View {
        Button(action: action) {
            label.frame(minHeight: HitTarget.minimum).contentShape(.rect)
        }
        .buttonStyle(PressableStyle())
    }
}
