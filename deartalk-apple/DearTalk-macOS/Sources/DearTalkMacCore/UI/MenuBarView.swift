import SwiftUI

public struct MenuBarView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    @StateObject private var toneManager = CustomToneManager.shared
    public let onOpenSandbox: () -> Void
    public let onOpenSettings: () -> Void

    public init(onOpenSandbox: @escaping () -> Void, onOpenSettings: @escaping () -> Void) {
        self.onOpenSandbox = onOpenSandbox
        self.onOpenSettings = onOpenSettings
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // 상단 타이틀 & 상태
            HStack {
                Text("✨")
                Text(UiStrings.appName)
                    .font(.system(size: 13, weight: .bold))
                Spacer()
                if monitor.hasAccessibilityPermission && monitor.isMonitoring {
                    Text(UiStrings.statusActive)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.green)
                } else if !monitor.hasAccessibilityPermission {
                    Button(action: {
                        OnboardingWindowController.shared.showOnboarding()
                    }) {
                        Text(UiStrings.permissionNeeded)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(.orange)
                    }
                    .buttonStyle(.plain)
                } else {
                    Text(UiStrings.statusPaused)
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                }
            }

            Divider()

            // Real-time Monitoring Toggle Button
            Button(action: {
                if !monitor.hasAccessibilityPermission {
                    OnboardingWindowController.shared.showOnboarding()
                } else if monitor.isMonitoring {
                    monitor.stopMonitoring()
                } else {
                    monitor.startMonitoring()
                }
            }) {
                HStack {
                    Image(systemName: monitor.isMonitoring && monitor.hasAccessibilityPermission ? "pause.circle.fill" : "play.circle.fill")
                        .foregroundColor(monitor.isMonitoring && monitor.hasAccessibilityPermission ? .orange : .green)
                    Text(monitor.hasAccessibilityPermission ? (monitor.isMonitoring ? UiStrings.disabled : UiStrings.enabled) : UiStrings.grantPermission)
                }
            }
            .buttonStyle(.plain)

            // 톤앤매너 선택 메뉴
            Menu {
                ForEach(toneManager.defaultTones) { tone in
                    Button(action: {
                        toneManager.selectedToneId = tone.id
                        toneManager.isTranslationMode = false
                    }) {
                        HStack {
                            Text(tone.icon)
                            Text(tone.name)
                            if !toneManager.isTranslationMode && toneManager.selectedToneId == tone.id {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                HStack {
                    Text(UiStrings.currentTone)
                    Spacer()
                    Text("\(toneManager.currentTone.icon) \(toneManager.currentTone.name)")
                        .foregroundColor(.secondary)
                }
            }

            Divider()

            // 샌드박스 및 설정
            Button(action: onOpenSandbox) {
                HStack {
                    Text(UiStrings.openSandbox)
                    Spacer()
                    Text("⌘S").font(.system(size: 10)).foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)

            Button(action: onOpenSettings) {
                HStack {
                    Text(UiStrings.settings)
                    Spacer()
                    Text("⌘,").font(.system(size: 10)).foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)

            Divider()

            Button(action: {
                NSApplication.shared.terminate(nil)
            }) {
                Text(UiStrings.quit)
                    .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(width: 260)
    }
}
