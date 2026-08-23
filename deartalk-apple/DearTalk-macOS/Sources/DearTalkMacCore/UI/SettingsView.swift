import SwiftUI

public struct SettingsView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    @StateObject private var toneManager = CustomToneManager.shared
    @AppStorage("debounceMs") private var debounceMs: Double = 400.0

    public init() {}

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    private var buildVersion: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }

    private var buildTimestamp: String {
        if let path = Bundle.main.path(forResource: "Info", ofType: "plist"),
           let attributes = try? FileManager.default.attributesOfItem(atPath: path),
           let modificationDate = attributes[.modificationDate] as? Date {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            return formatter.string(from: modificationDate)
        }
        return "2026-08-22 01:14:24"
    }

    public var body: some View {
        TabView {
            // Tab 1: General Settings
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
            }
            .padding(20)
            .tabItem {
                Label(UiStrings.settingsTabGeneral, systemImage: "gearshape")
            }

            // Tab 2: About & Help
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Text("✨")
                            .font(.system(size: 32))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(UiStrings.appName)
                                .font(.headline)
                            Text("\(UiStrings.appVersionLabel): \(appVersion) (\(buildVersion))")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Text("\(UiStrings.buildTimestampLabel): \(buildTimestamp)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.bottom, 8)

                    Divider()

                    VStack(alignment: .leading, spacing: 8) {
                        Text(UiStrings.userGuideTitle)
                            .font(.headline)
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text(UiStrings.userGuideHowToUseTitle)
                                .font(.subheadline)
                                .fontWeight(.bold)
                            Text(UiStrings.userGuideHowToUseContent)
                                .font(.system(size: 12))
                                .lineSpacing(4)
                                .foregroundColor(.secondary)
                        }
                        .padding(12)
                        .background(Color.primary.opacity(0.04))
                        .cornerRadius(8)
                    }

                    Divider()

                    // Environment Diagnosis Section
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(UiStrings.envDiagnosisTitle)
                                .font(.headline)
                            Spacer()
                            Button(action: {
                                Task {
                                    await RuntimeSetupManager.shared.diagnoseEnvironment()
                                    DearTalkIntentEngine.shared.detectAndInitOnDeviceModel()
                                }
                            }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "arrow.clockwise")
                                        .font(.system(size: 10))
                                    Text(UiStrings.envRefreshDiagnosis)
                                        .font(.system(size: 11))
                                }
                            }
                            .buttonStyle(.plain)
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("• \(UiStrings.envModelStatus):")
                                    .font(.system(size: 11, weight: .semibold))
                                Spacer()
                                Text(RuntimeSetupManager.shared.hasModelFile ? "\(UiStrings.envStatusInstalled) (\(RuntimeSetupManager.shared.modelFileSizeMB) MB)" : UiStrings.envStatusMissing)
                                    .font(.system(size: 11))
                                    .foregroundColor(RuntimeSetupManager.shared.hasModelFile ? .green : .orange)
                            }
                            if let modelPath = RuntimeSetupManager.shared.modelFilePath {
                                Text("  📁 \(modelPath)")
                                    .font(.system(size: 9))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                            }

                            HStack {
                                Text("• \(UiStrings.envRuntimeStatus):")
                                    .font(.system(size: 11, weight: .semibold))
                                Spacer()
                                Text(RuntimeSetupManager.shared.hasRuntimeBinary ? UiStrings.envStatusInstalled : UiStrings.envStatusMissing)
                                    .font(.system(size: 11))
                                    .foregroundColor(RuntimeSetupManager.shared.hasRuntimeBinary ? .green : .orange)
                            }
                            if let binPath = RuntimeSetupManager.shared.runtimeBinaryPath {
                                Text("  ⚡ \(binPath)")
                                    .font(.system(size: 9))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                            }

                            HStack {
                                Text("• \(UiStrings.envDaemonStatus):")
                                    .font(.system(size: 11, weight: .semibold))
                                Spacer()
                                Text(DearTalkIntentEngine.shared.isModelLoaded || RuntimeSetupManager.shared.isDaemonRunning ? UiStrings.envStatusRunning : UiStrings.envStatusStopped)
                                    .font(.system(size: 11))
                                    .foregroundColor(DearTalkIntentEngine.shared.isModelLoaded || RuntimeSetupManager.shared.isDaemonRunning ? .cyan : .gray)
                            }
                        }
                        .padding(12)
                        .background(Color.primary.opacity(0.04))
                        .cornerRadius(8)
                    }

                    Divider()

                    VStack(alignment: .leading, spacing: 8) {
                        Text(UiStrings.settingsSectionPhilosophy)
                            .font(.headline)
                        Text(UiStrings.settingsPhilosophyContent)
                            .font(.system(size: 12))
                            .lineSpacing(4)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(20)
            }
            .tabItem {
                Label(UiStrings.settingsTabAbout, systemImage: "info.circle")
            }
        }
        .frame(width: 500, height: 440)
    }
}

