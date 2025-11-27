package de.zemki.metagcompose.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

actual class PermissionHandler {
    actual fun hasAudioPermission(): Boolean {
        val context = getAppContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual fun requestAudioPermission(onResult: (Boolean) -> Unit) {
        val launcher = getPermissionLauncher()
        if (launcher == null) {
            onResult(false)
            return
        }

        // Set the callback for this request
        setPermissionCallback(onResult)

        try {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    actual fun hasNotificationPermission(): Boolean {
        val context = getAppContext()

        // For Android 13+ (API 33+), need to check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        // For Android 12 and below, notification permissions are granted by default
        return true
    }

    actual fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        val context = getAppContext()

        // For Android 12 and below, permissions are granted by default
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
            return
        }

        val launcher = getPermissionLauncher()
        if (launcher == null) {
            onResult(false)
            return
        }

        // Set the callback for this request
        setPermissionCallback(onResult)

        try {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (e: Exception) {
            onResult(false)
        }
    }
}

// Global permission management
private var permissionLauncher: ActivityResultLauncher<String>? = null
private var permissionCallback: ((Boolean) -> Unit)? = null

fun setPermissionLauncher(launcher: ActivityResultLauncher<String>?) {
    permissionLauncher = launcher
}

fun getPermissionLauncher(): ActivityResultLauncher<String>? = permissionLauncher

fun setPermissionCallback(callback: (Boolean) -> Unit) {
    permissionCallback = callback
}

fun handlePermissionResult(granted: Boolean) {
    permissionCallback?.invoke(granted)
    permissionCallback = null
}