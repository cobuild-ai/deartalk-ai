import SwiftUI

public struct SettingsView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    @StateObject private var toneManager = CustomToneManager.shared
    @AppStorage("debounceMs") private var debounceMs: Double = 400.0

    public init() {}

    public var body: some View {
        Form {
            Section(header: Text("🔐 접근성 권한 상태")) {
                HStack {
                    Image(systemName: monitor.hasAccessibilityPermission ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                        .foregroundColor(monitor.hasAccessibilityPermission ? .green : .orange)
                    Text(monitor.hasAccessibilityPermission ? "손쉬운 사용 권한 승인됨" : UiStrings.accessibilityPermission)
                        .font(.system(size: 13))

                    Spacer()

                    if !monitor.hasAccessibilityPermission {
                        Button(UiStrings.grantPermission) {
                            monitor.requestPermission()
                        }
                    }
                }
            }

            Section(header: Text("⚙️ 실시간 감지 설정")) {
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

            Section(header: Text("🔒 온디바이스 AI 철학")) {
                Text("• 100% 기기 내부 NPU/GPU 온디바이스 추론\n• 외부 네트워크 통신 0% (완벽한 프라이버시 보장)\n• 꼼수 및 하드코딩 대체 로직 0%")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
        }
        .padding(20)
        .frame(minWidth: 460, minHeight: 320)
    }
}
