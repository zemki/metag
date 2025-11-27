package de.zemki.metagcompose.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter

/**
 * Application logger that uses Kermit for cross-platform logging.
 *
 * Debug logs are automatically stripped in release builds.
 *
 * Usage:
 * ```
 * AppLogger.d("Debug message")
 * AppLogger.i("Info message")
 * AppLogger.w("Warning message")
 * AppLogger.e("Error message", throwable)
 * ```
 */
object AppLogger {
    private val logger = Logger(
        config = StaticConfig(
            // In release builds, set minSeverity to Info to strip debug logs
            minSeverity = if (isDebugBuild()) Severity.Debug else Severity.Info
        ),
        tag = "MeTag"
    )

    /**
     * Log a debug message.
     * These are stripped in release builds.
     */
    fun d(message: String, tag: String? = null, throwable: Throwable? = null) {
        logger.d(tag = tag ?: "MeTag", throwable = throwable) { message }
    }

    /**
     * Log an info message.
     * These appear in both debug and release builds.
     */
    fun i(message: String, tag: String? = null, throwable: Throwable? = null) {
        logger.i(tag = tag ?: "MeTag", throwable = throwable) { message }
    }

    /**
     * Log a warning message.
     */
    fun w(message: String, tag: String? = null, throwable: Throwable? = null) {
        logger.w(tag = tag ?: "MeTag", throwable = throwable) { message }
    }

    /**
     * Log an error message.
     */
    fun e(message: String, tag: String? = null, throwable: Throwable? = null) {
        logger.e(tag = tag ?: "MeTag", throwable = throwable) { message }
    }

    /**
     * Log a verbose message (lower priority than debug).
     * These are stripped in release builds.
     */
    fun v(message: String, tag: String? = null, throwable: Throwable? = null) {
        logger.v(tag = tag ?: "MeTag", throwable = throwable) { message }
    }
}

/**
 * Check if this is a debug build.
 * This is expected to be implemented per platform.
 */
expect fun isDebugBuild(): Boolean
