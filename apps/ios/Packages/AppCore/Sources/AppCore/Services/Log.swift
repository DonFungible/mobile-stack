import Foundation
import os

public enum Log {
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "app", category: "app")

    public static func debug(_ message: String) {
        logger.debug("\(message, privacy: .public)")
    }

    public static func info(_ message: String) {
        logger.info("\(message, privacy: .public)")
    }

    public static func error(_ message: String) {
        logger.error("\(message, privacy: .public)")
    }
}
