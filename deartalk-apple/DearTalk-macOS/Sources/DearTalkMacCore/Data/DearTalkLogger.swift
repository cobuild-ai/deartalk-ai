import Foundation
import OSLog

/// DearTalk 전용 통합 로거 (터미널 콘솔 + 파일 로깅 ~/.deartalk/deartalk.log + Apple os_log)
public final class DearTalkLogger {
    public static let shared = DearTalkLogger()
    private let osLogger = Logger(subsystem: "ai.deartalk.mac", category: "DearTalk")
    private let logFileUrl: URL

    private let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return formatter
    }()

    private init() {
        let homeDir = FileManager.default.homeDirectoryForCurrentUser
        let deartalkDir = homeDir.appendingPathComponent(".deartalk", isDirectory: true)
        try? FileManager.default.createDirectory(at: deartalkDir, withIntermediateDirectories: true)
        self.logFileUrl = deartalkDir.appendingPathComponent("deartalk.log")
    }

    public static func info(_ message: String, category: String = "App") {
        shared.log(level: "INFO", category: category, message: message)
    }

    public static func debug(_ message: String, category: String = "App") {
        shared.log(level: "DEBUG", category: category, message: message)
    }

    public static func warning(_ message: String, category: String = "App") {
        shared.log(level: "WARN", category: category, message: message)
    }

    public static func error(_ message: String, category: String = "App", error: Error? = nil) {
        let fullMsg = error != nil ? "\(message) | Error: \(error!.localizedDescription)" : message
        shared.log(level: "ERROR", category: category, message: fullMsg)
    }

    private func log(level: String, category: String, message: String) {
        let timestamp = dateFormatter.string(from: Date())
        let formatted = "[\(timestamp)] [\(level)] [\(category)] \(message)"

        // 1. 터미널 표준 출력 (stdout)
        print(formatted)
        fflush(stdout)

        // 2. Apple OSLog 연동
        switch level {
        case "ERROR":
            osLogger.error("[\(category)] \(message, privacy: .public)")
        case "WARN":
            osLogger.warning("[\(category)] \(message, privacy: .public)")
        case "DEBUG":
            osLogger.debug("[\(category)] \(message, privacy: .public)")
        default:
            osLogger.info("[\(category)] \(message, privacy: .public)")
        }

        // 3. 파일(~/.deartalk/deartalk.log) 기록
        appendToFile(formatted + "\n")
    }

    private func appendToFile(_ text: String) {
        guard let data = text.data(using: .utf8) else { return }
        if FileManager.default.fileExists(atPath: logFileUrl.path) {
            if let fileHandle = try? FileHandle(forWritingTo: logFileUrl) {
                fileHandle.seekToEndOfFile()
                fileHandle.write(data)
                try? fileHandle.close()
            }
        } else {
            try? data.write(to: logFileUrl, options: .atomic)
        }
    }
}
