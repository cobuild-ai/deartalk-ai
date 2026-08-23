import SwiftUI
import AppKit
import Combine
import DearTalkMacCore
import Darwin

@main
struct DearTalkMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        Settings {
            SettingsView()
        }
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem?
    private var popover: NSPopover?
    private var sandboxWindow: NSWindow?
    private var lockFileDescriptor: Int32 = -1
    private var diffCancellable: AnyCancellable?
    private let singleInstanceNotificationName = "ai.deartalk.mac.bringToFront"

    func applicationDidFinishLaunching(_ notification: Notification) {
        DearTalkLogger.info("🚀 DearTalk Mac 애플리케이션 기동 시작...", category: "App")

        // 🔒 1. 단일 인스턴스 (싱글톤 프로세스) 락 획득 검증
        guard acquireSingleInstanceLock() else {
            DearTalkLogger.warning("⚠️ 이미 실행 중인 DearTalk 인스턴스가 존재합니다. 기존 인스턴스 창을 활성화하고 새 프로세스를 종료합니다.", category: "App")
            // 이미 실행 중인 인스턴스가 존재함 -> 기존 인스턴스 창 활성화 요청 후 즉시 종료
            DistributedNotificationCenter.default().postNotificationName(
                NSNotification.Name(singleInstanceNotificationName),
                object: nil,
                userInfo: nil,
                deliverImmediately: true
            )
            DispatchQueue.main.async {
                NSApp.terminate(nil)
            }
            return
        }

        DearTalkLogger.info("🔒 단일 인스턴스 락 획득 완료", category: "App")
        NSApp.setActivationPolicy(.regular)

        setupSingleInstanceObserver()
        setupMenuBar()
        AccessibilityMonitor.shared.startMonitoring()
        observeDiffEvents()

        // 권한 미허용 시 Raycast 스타일 대화형 온보딩 마법사 표시
        if !AccessibilityMonitor.shared.hasAccessibilityPermission {
            OnboardingWindowController.shared.showOnboarding()
        }

        // 앱 실행 시 메인 샌드박스 윈도우를 화면 중앙에 즉시 띄우기
        DispatchQueue.main.async {
            self.openSandboxWindow()
            DearTalkLogger.info("🖥️ 샌드박스 윈도우 활성화 완료", category: "App")
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        DearTalkLogger.info("🛑 DearTalk Mac 종료 처리 중...", category: "App")
        if lockFileDescriptor != -1 {
            flock(lockFileDescriptor, LOCK_UN)
            close(lockFileDescriptor)
            lockFileDescriptor = -1
        }
    }

    /// 파일 디스크립터 기반 커널 락(flock)을 통한 완벽한 싱글톤 프로세스 보호
    private func acquireSingleInstanceLock() -> Bool {
        let lockDir = (NSHomeDirectory() as NSString).appendingPathComponent(".deartalk")
        try? FileManager.default.createDirectory(atPath: lockDir, withIntermediateDirectories: true)
        let lockPath = (lockDir as NSString).appendingPathComponent("deartalk.lock")

        lockFileDescriptor = open(lockPath, O_CREAT | O_RDWR, 0o666)
        guard lockFileDescriptor != -1 else {
            return true
        }

        // 논블로킹 배타적 락(LOCK_EX | LOCK_NB) 시도
        if flock(lockFileDescriptor, LOCK_EX | LOCK_NB) != 0 {
            close(lockFileDescriptor)
            lockFileDescriptor = -1
            return false // 이미 다른 인스턴스가 실행 중
        }

        return true
    }

    /// 다른 인스턴스 실행 시도 시 기존 창을 앞으로 띄우는 옵저버
    private func setupSingleInstanceObserver() {
        DistributedNotificationCenter.default().addObserver(
            forName: NSNotification.Name(singleInstanceNotificationName),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.openSandboxWindow()
        }
    }

    private func setupMenuBar() {
        guard statusItem == nil else { return }

        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)

        if let button = statusItem?.button {
            button.title = "✨ DearTalk"
            button.action = #selector(togglePopover)
            button.target = self
        }

        let popover = NSPopover()
        popover.contentSize = NSSize(width: 260, height: 260)
        popover.behavior = .transient
        popover.contentViewController = NSHostingController(
            rootView: MenuBarView(
                onOpenSandbox: { [weak self] in
                    self?.openSandboxWindow()
                    self?.popover?.performClose(nil)
                },
                onOpenSettings: { [weak self] in
                    self?.openSettingsWindow()
                    self?.popover?.performClose(nil)
                }
            )
        )
        self.popover = popover
    }

    @objc private func togglePopover() {
        guard let button = statusItem?.button, let popover = popover else { return }
        if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        }
    }

    private func observeDiffEvents() {
        diffCancellable = AccessibilityMonitor.shared.$latestDiffResult
            .removeDuplicates()
            .receive(on: DispatchQueue.main)
            .sink { diffResult in
                if let diff = diffResult, diff.hasChanges {
                    let pt = AccessibilityMonitor.shared.cursorInfo?.screenPoint
                    OverlayPanelController.shared.show(diffResult: diff, near: pt)
                } else {
                    OverlayPanelController.shared.hideIfAutomatic()
                }
            }
    }

    public func openSandboxWindow() {
        if sandboxWindow == nil {
            let window = NSWindow(
                contentRect: NSRect(x: 0, y: 0, width: 620, height: 560),
                styleMask: [.titled, .closable, .miniaturizable, .resizable],
                backing: .buffered,
                defer: false
            )
            window.title = UiStrings.sandboxTitle
            window.center()
            window.contentView = NSHostingView(rootView: SandboxView())
            window.isReleasedWhenClosed = false
            self.sandboxWindow = window
        }
        sandboxWindow?.makeKeyAndOrderFront(nil)
        NSApp.activate()
    }

    public func openSettingsWindow() {
        NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
        NSApp.activate()
    }
}
