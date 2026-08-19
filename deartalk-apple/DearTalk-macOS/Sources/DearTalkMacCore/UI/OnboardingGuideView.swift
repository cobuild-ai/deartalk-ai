import SwiftUI
import AppKit

public struct OnboardingGuideView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    public let onComplete: () -> Void

    public init(onComplete: @escaping () -> Void = {}) {
        self.onComplete = onComplete
    }

    public var body: some View {
        VStack(spacing: 20) {
            if monitor.hasAccessibilityPermission {
                // Success / Ready State
                VStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.green.opacity(0.15))
                            .frame(width: 80, height: 80)
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.green)
                    }

                    Text(UiStrings.onboardingReadyTitle)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)

                    Text(UiStrings.onboardingReadyDesc)
                        .font(.system(size: 13))
                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)

                    Button(action: onComplete) {
                        Text(UiStrings.onboardingStartBtn)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 10)
                }
            } else {
                // Permission Setup Guide State
                VStack(spacing: 18) {
                    // Header
                    HStack(spacing: 10) {
                        Text("✨")
                            .font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(UiStrings.onboardingHeaderTitle)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                            Text(UiStrings.onboardingHeaderDesc)
                                .font(.system(size: 11))
                                .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        }
                    }

                    Divider()
                        .background(Color.white.opacity(0.1))

                    // 2-Step Guide Cards
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(alignment: .top, spacing: 12) {
                            Text("1")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.black)
                                .frame(width: 22, height: 22)
                                .background(Color.cyan)
                                .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 2) {
                                Text(UiStrings.onboardingStep1Title)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                                Text(UiStrings.onboardingStep1Desc)
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(nsColor: .secondaryLabelColor))
                            }
                        }

                        HStack(alignment: .top, spacing: 12) {
                            Text("2")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.black)
                                .frame(width: 22, height: 22)
                                .background(Color.cyan)
                                .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 2) {
                                Text(UiStrings.onboardingStep2Title)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                                Text(UiStrings.onboardingStep2Desc)
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(nsColor: .secondaryLabelColor))
                            }
                        }
                    }
                    .padding(14)
                    .background(Color.black.opacity(0.3))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.white.opacity(0.1), lineWidth: 1)
                    )

                    // Open Accessibility Settings Action Button
                    Button(action: {
                        monitor.requestPermission()
                    }) {
                        HStack {
                            Image(systemName: "gearshape.fill")
                            Text(UiStrings.onboardingOpenSettingsBtn)
                        }
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(
                            LinearGradient(
                                colors: [Color.cyan, Color(red: 0.22, green: 0.85, blue: 0.65)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(8)
                    }
                    .buttonStyle(.plain)

                    // Privacy notice
                    Text(UiStrings.onboardingPrivacyDesc)
                        .font(.system(size: 10))
                        .foregroundColor(Color(nsColor: .tertiaryLabelColor))
                }
            }
        }
        .padding(24)
        .frame(width: 420)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(red: 0.1, green: 0.12, blue: 0.16))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                )
        )
    }
}

/// Raycast 스타일의 온보딩 윈도우 컨트롤러
public final class OnboardingWindowController: NSObject {
    public static let shared = OnboardingWindowController()

    private var window: NSWindow?

    public func showOnboarding() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            if self.window == nil {
                let view = OnboardingGuideView(onComplete: { [weak self] in
                    self?.close()
                })

                let hostingView = NSHostingView(rootView: view)
                let w = NSWindow(
                    contentRect: NSRect(x: 0, y: 0, width: 420, height: 360),
                    styleMask: [.titled, .closable, .fullSizeContentView],
                    backing: .buffered,
                    defer: false
                )
                w.center()
                w.title = "DearTalk AI 시작하기"
                w.titlebarAppearsTransparent = true
                w.titleVisibility = .hidden
                w.isMovableByWindowBackground = true
                w.isReleasedWhenClosed = false
                w.backgroundColor = .clear
                w.contentView = hostingView
                self.window = w
            }

            self.window?.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
        }
    }

    public func close() {
        DispatchQueue.main.async { [weak self] in
            self?.window?.orderOut(nil)
        }
    }
}
