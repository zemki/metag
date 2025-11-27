package de.zemki.metagcompose.util

import de.zemki.metagcompose.util.AppLogger
/**
 * Manages QR code tokens from deep links.
 *
 * When the app is opened via a deep link (metagapp://login?token=xxx),
 * the platform-specific code extracts the token and stores it here.
 * The LoginViewModel then consumes it to perform auto-login.
 */
object DeepLinkHandler {
    private var _qrToken: String? = null
    private var _tokenVersion: Int = 0

    /**
     * Store a QR token from a deep link.
     * Called by MainActivity (Android) or iOSApp (iOS).
     *
     * @param token The encrypted QR token from the URL
     */
    fun setQrToken(token: String) {
        AppLogger.d("QR token set: ${token.take(20)}...", tag = "DeepLinkHandler")
        _qrToken = token
        _tokenVersion++ // Increment version so UI can detect new tokens
        AppLogger.d("Token version now: $_tokenVersion", tag = "DeepLinkHandler")
    }

    /**
     * Get the current token version.
     * This increments each time a new token is set.
     * UI can observe this to detect when new tokens arrive.
     */
    fun getTokenVersion(): Int = _tokenVersion

    /**
     * Get and consume the QR token.
     * Returns the token once, then clears it to prevent reuse.
     * Called by LoginViewModel on app launch.
     *
     * @return The QR token if available, null otherwise
     */
    fun consumeQrToken(): String? {
        val token = _qrToken
        if (token != null) {
            AppLogger.d("QR token consumed", tag = "DeepLinkHandler")
        }
        _qrToken = null // Clear after reading (but keep version for tracking)
        return token
    }

    /**
     * Check if a QR token is available without consuming it.
     * Useful for debugging or conditional UI.
     *
     * @return true if a QR token is waiting to be consumed
     */
    fun hasQrToken(): Boolean {
        return _qrToken != null
    }

    /**
     * Clear the QR token without consuming it.
     * Useful for error scenarios or cancellation.
     */
    fun clearQrToken() {
        AppLogger.d("QR token cleared", tag = "DeepLinkHandler")
        _qrToken = null
    }
}
