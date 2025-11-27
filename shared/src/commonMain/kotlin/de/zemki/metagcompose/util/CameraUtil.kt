package de.zemki.metagcompose.util

/**
 * Opens the native camera app on the device.
 * Used for QR code scanning with system camera.
 *
 * The camera app will recognize QR codes containing deep links
 * (metagapp://login?token=xxx or https://domain/qr-login?token=xxx)
 * and automatically open the MeTag app for login.
 */
expect fun openCamera()