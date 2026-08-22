import Foundation
import AppKit
import Combine

/// Manages on-device AI runtime dependencies (llama.cpp / llama-server, Homebrew, model files) on macOS.
/// Provides automatic diagnosis, one-click installation via Homebrew, and daemon lifecycle management.
public final class RuntimeSetupManager: ObservableObject {
    public static let shared = RuntimeSetupManager()

    @Published public private(set) var isChecking: Bool = false
    @Published public private(set) var hasModelFile: Bool = false
    @Published public private(set) var modelFilePath: String? = nil
    @Published public private(set) var modelFileSizeMB: Int64 = 0

    @Published public private(set) var hasRuntimeBinary: Bool = false
    @Published public private(set) var runtimeBinaryPath: String? = nil

    @Published public private(set) var hasHomebrew: Bool = false
    @Published public private(set) var homebrewPath: String? = nil

    @Published public private(set) var isDaemonRunning: Bool = false
    @Published public private(set) var daemonPort: Int = 11435

    @Published public private(set) var isInstalling: Bool = false
    @Published public private(set) var installProgressMessage: String = ""
    @Published public private(set) var lastInstallError: String? = nil
    @Published public private(set) var isInstallCompleted: Bool = false

    private var installProcess: Process?

    public init() {
        Task {
            await diagnoseEnvironment()
        }
    }

    /// Comprehensive environment diagnosis across model file, llama-server binary, and daemon health
    @MainActor
    public func diagnoseEnvironment() async {
        self.isChecking = true
        defer { self.isChecking = false }

        let fileManager = FileManager.default
        let homeDir = fileManager.homeDirectoryForCurrentUser.path

        // 1. Diagnose Model File
        var modelFound = false
        let modelCandidates = [
            Bundle.main.resourcePath.map { "\($0)/models/model.gguf" },
            Bundle.main.resourcePath.map { "\($0)/model.gguf" },
            "\(homeDir)/Library/Application Support/DearTalk/models/model.gguf",
            "\(homeDir)/Library/Application Support/DearTalk/models/gemma-2b-it.gguf",
            "\(homeDir)/.deartalk/models/model.gguf",
            "\(homeDir)/.deartalk/models/gemma-2b-it.gguf"
        ].compactMap { $0 }

        for path in modelCandidates {
            if fileManager.fileExists(atPath: path),
               let attrs = try? fileManager.attributesOfItem(atPath: path),
               let size = attrs[.size] as? Int64, size > 0 {
                self.hasModelFile = true
                self.modelFilePath = path
                self.modelFileSizeMB = size / (1024 * 1024)
                modelFound = true
                break
            }
        }
        if !modelFound {
            self.hasModelFile = false
            self.modelFilePath = nil
            self.modelFileSizeMB = 0
        }

        // 2. Diagnose Homebrew
        let brewCandidates = [
            "/opt/homebrew/bin/brew",
            "/usr/local/bin/brew"
        ]
        if let brew = brewCandidates.first(where: { fileManager.fileExists(atPath: $0) }) {
            self.hasHomebrew = true
            self.homebrewPath = brew
        } else {
            self.hasHomebrew = false
            self.homebrewPath = nil
        }

        // 3. Diagnose llama-server Binary
        let serverCandidates = [
            "/opt/homebrew/bin/llama-server",
            "/usr/local/bin/llama-server",
            "\(homeDir)/.local/bin/llama-server",
            "\(homeDir)/.deartalk/bin/llama-server",
            Bundle.main.bundleURL.appendingPathComponent("Contents/Helpers/llama-server").path
        ]
        if let server = serverCandidates.first(where: { fileManager.fileExists(atPath: $0) }) {
            self.hasRuntimeBinary = true
            self.runtimeBinaryPath = server
        } else {
            self.hasRuntimeBinary = false
            self.runtimeBinaryPath = nil
        }

        // 4. Diagnose Daemon Health
        self.isDaemonRunning = await checkDaemonHealth(port: daemonPort)
    }

    private func checkDaemonHealth(port: Int) async -> Bool {
        guard let url = URL(string: "http://127.0.0.1:\(port)/health") else { return false }
        var request = URLRequest(url: url)
        request.timeoutInterval = 0.6
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
    }

    /// Automatically installs llama.cpp via Homebrew in the background with progress reporting
    public func installLlamaCppViaBrew() {
        guard !isInstalling else { return }

        guard let brew = homebrewPath ?? (FileManager.default.fileExists(atPath: "/opt/homebrew/bin/brew") ? "/opt/homebrew/bin/brew" : nil) else {
            DispatchQueue.main.async {
                self.lastInstallError = UiStrings.isKo ? "Homebrew가 설치되어 있지 않습니다. 터미널 명령어를 복사하여 직접 설치해 주세요." : "Homebrew not found. Please copy command and install manually."
            }
            return
        }

        DispatchQueue.main.async {
            self.isInstalling = true
            self.isInstallCompleted = false
            self.lastInstallError = nil
            self.installProgressMessage = UiStrings.isKo ? "Homebrew를 통해 llama.cpp 설치 중... (brew install llama.cpp)" : "Installing llama.cpp via Homebrew..."
        }

        DearTalkLogger.info("⚡ Starting background brew install llama.cpp using \(brew)", category: "RuntimeSetup")

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            let process = Process()
            process.executableURL = URL(fileURLWithPath: brew)
            process.arguments = ["install", "llama.cpp"]

            // Provide proper environment
            var env = ProcessInfo.processInfo.environment
            env["PATH"] = "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
            process.environment = env

            let pipe = Pipe()
            process.standardOutput = pipe
            process.standardError = pipe

            self?.installProcess = process

            do {
                try process.run()
                process.waitUntilExit()

                let status = process.terminationStatus
                DispatchQueue.main.async {
                    self?.isInstalling = false
                    if status == 0 {
                        self?.isInstallCompleted = true
                        self?.installProgressMessage = UiStrings.isKo ? "llama.cpp 설치 완료! 온디바이스 엔진을 초기화합니다." : "llama.cpp installed successfully! Initializing engine."
                        DearTalkLogger.info("✅ brew install llama.cpp completed successfully", category: "RuntimeSetup")
                        Task {
                            await self?.diagnoseEnvironment()
                            DearTalkIntentEngine.shared.detectAndInitOnDeviceModel()
                        }
                    } else {
                        let data = pipe.fileHandleForReading.readDataToEndOfFile()
                        let output = String(data: data, encoding: .utf8) ?? "Unknown error"
                        self?.lastInstallError = UiStrings.isKo ? "설치 중 오류 발생 (코드: \(status)): \(output.prefix(100))" : "Install error (code \(status)): \(output.prefix(100))"
                        DearTalkLogger.error("❌ brew install failed: \(output)", category: "RuntimeSetup")
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self?.isInstalling = false
                    self?.lastInstallError = error.localizedDescription
                    DearTalkLogger.error("❌ Process run failed: \(error.localizedDescription)", category: "RuntimeSetup", error: error)
                }
            }
        }
    }

    /// Copies the Homebrew install command to clipboard for manual execution
    public func copyBrewInstallCommand() {
        let cmd = "brew install llama.cpp"
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(cmd, forType: .string)
        DearTalkLogger.info("📋 Copied brew install command to clipboard", category: "RuntimeSetup")
    }
}
