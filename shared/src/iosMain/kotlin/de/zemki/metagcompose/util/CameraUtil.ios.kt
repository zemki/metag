package de.zemki.metagcompose.util

import platform.Foundation.NSNotificationCenter

/**
 * iOS implementation: Opens the in-app QR scanner.
 * Posts a notification that Swift's QRScannerManager observes to present
 * the DataScannerViewController for QR code scanning.
 *
 * When a valid MeTag QR code is scanned, the token is extracted and
 * passed to DeepLinkHandler for automatic login.
 */
actual fun openCamera() {
    // Post notification to trigger QR scanner presentation in Swift
    // QRScannerManager in Swift observes this notification
    // NSNotificationName is a type alias for NSString in Kotlin/Native
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = "com.metag.showQRScanner",
        `object` = null
    )
    AppLogger.d("Posted showQRScanner notification", tag = "CameraUtil")
}
