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
                // 성공 / 준비 완료 상태
                VStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.green.opacity(0.15))
                            .frame(width: 80, height: 80)
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.green)
                    }

                    Text("🎉 준비 완료!")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)

                    Text("이제 카카오톡, 슬랙, 메모장, 브라우저 어디서든\n글을 쓰시면 실시간으로 AI 교정 패널이 나타납니다.")
                        .font(.system(size: 13))
                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)

                    Button(action: onComplete) {
                        Text("시작하기")
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
                // 권한 요청 안내 상태
                VStack(spacing: 18) {
                    // 상단 헤더
                    HStack(spacing: 10) {
                        Text("✨")
                            .font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("DearTalk 실시간 AI 활성화")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                            Text("타이핑 중 실시간 문맥 분석을 위해 1초 설정이 필요합니다")
                                .font(.system(size: 11))
                                .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        }
                    }

                    Divider()
                        .background(Color.white.opacity(0.1))

                    // 2단계 가이드 카드
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(alignment: .top, spacing: 12) {
                            Text("1")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.black)
                                .frame(width: 22, height: 22)
                                .background(Color.cyan)
                                .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 2) {
                                Text("아래 버튼을 눌러 시스템 설정을 엽니다")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                                Text("macOS 손쉬운 사용(Accessibility) 설정 창이 자동으로 열립니다.")
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
                                Text("목록에서 'DearTalk' 스위치를 껐다 켭니다")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                                Text("스위치를 켜면 이 창이 자동으로 완료 상태로 바뀝니다.")
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(nsColor: .secondaryLabelColor))
                            }
                        }
                    }
                    .padding(14)
                    .background(Color.white.opacity(0.04))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.white.opacity(0.08), lineWidth: 1)
                    )

                    // 원클릭 시스템 설정 열기 버튼
                    Button(action: {
                        monitor.requestPermission()
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "gearshape.fill")
                            Text("손쉬운 사용 설정 열기")
                                .font(.system(size: 13, weight: .bold))
                        }
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                        .cornerRadius(8)
                    }
                    .buttonStyle(.plain)

                    // 실시간 상태 폴링 인디케이터
                    HStack(spacing: 6) {
                        ProgressView()
                            .scaleEffect(0.6)
                        Text("권한 허용을 실시간 감지 중입니다...")
                            .font(.system(size: 11))
                            .foregroundColor(Color(nsColor: .tertiaryLabelColor))
                    }
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
