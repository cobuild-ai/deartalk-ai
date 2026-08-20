import Foundation
import OSLog

public enum DearTalkLogger {
    private static let subsystem = "ai.deartalk.ios"

    public static func info(_ message: String, category: String = "General") {
        let logger = Logger(subsystem: subsystem, category: category)
        logger.info("\(message, privacy: .public)")
        #if DEBUG
        print("[\(category)] ℹ️ \(message)")
        #endif
    }

    public static func debug(_ message: String, category: String = "General") {
        let logger = Logger(subsystem: subsystem, category: category)
        logger.debug("\(message, privacy: .public)")
        #if DEBUG
        print("[\(category)] 🐛 \(message)")
        #endif
    }

    public static func error(_ message: String, category: String = "General") {
        let logger = Logger(subsystem: subsystem, category: category)
        logger.error("\(message, privacy: .public)")
        #if DEBUG
        print("[\(category)] ❌ \(message)")
        #endif
    }
}
