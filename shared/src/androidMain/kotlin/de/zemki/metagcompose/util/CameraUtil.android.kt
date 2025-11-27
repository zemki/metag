package de.zemki.metagcompose.util

import android.content.Intent
import android.provider.MediaStore

/**
 * Android implementation: Opens the system camera app.
 * Opens the camera in normal mode (not photo capture) so QR code scanning works.
 * When user scans a QR code, Android will recognize the deep link
 * and offer to open MeTag app if it contains metagapp:// or configured HTTPS URL.
 */
actual fun openCamera() {
    val context = getAppContext() ?: return

    try {
        // Launch the default camera app in normal mode (not capture mode)
        // Most modern Android cameras automatically detect QR codes
        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        cameraIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Check if there's a camera app available
        val packageManager = context.packageManager
        if (cameraIntent.resolveActivity(packageManager) != null) {
            context.startActivity(cameraIntent)
        }
    } catch (e: Exception) {
        // Failed to open camera
    }
}
