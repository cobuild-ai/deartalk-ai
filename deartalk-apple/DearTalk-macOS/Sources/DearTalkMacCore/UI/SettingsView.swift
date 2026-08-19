import SwiftUI

public struct SettingsView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    @StateObject private var toneManager = CustomToneManager.shared
    @AppStorage("debounceMs") private var debounceMs: Double = 400.0

    public init() {}

    public var body: some View {
        Form {
            Section(header: Text(UiStrings.settingsSectionPermission)) {
                HStack {
                    Image(systemName: monitor.hasAccessibilityPermission ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                        .foregroundColor(monitor.hasAccessibilityPermission ? .green : .orange)
                    Text(monitor.hasAccessibilityPermission ? UiStrings.settingsPermissionGranted : UiStrings.accessibilityPermission)
                        .font(.system(size: 13))

                    Spacer()

                    if !monitor.hasAccessibilityPermission {
                        Button(UiStrings.grantPermission) {
                            monitor.requestPermission()
                        }
                    }
                }
            }

            Section(header: Text(UiStrings.settingsSectionDetection)) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(UiStrings.debounceSensitivity)
                        Spacer()
                        Text("\(Int(debounceMs)) ms")
                            .foregroundColor(.secondary)
                    }
                    Slider(value: $debounceMs, in: 200...1000, step: 50)
                }
            }

            Section(header: Text(UiStrings.settingsSectionPhilosophy)) {
                Text(UiStrings.settingsPhilosophyContent)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
        }
        .padding(20)
        .frame(minWidth: 460, minHeight: 320)
    }
}
