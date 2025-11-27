package de.zemki.metagcompose.util

import platform.AVFAudio.*

actual class PermissionHandler {
    actual fun hasAudioPermission(): Boolean {
        return when (AVAudioSession.sharedInstance().recordPermission) {
            AVAudioSessionRecordPermissionGranted -> true
            AVAudioSessionRecordPermissionDenied -> false
            AVAudioSessionRecordPermissionUndetermined -> false
            else -> false
        }
    }

    actual fun requestAudioPermission(onResult: (Boolean) -> Unit) {
        when (AVAudioSession.sharedInstance().recordPermission) {
            AVAudioSessionRecordPermissionGranted -> {
                // Already granted
                onResult(true)
            }
            AVAudioSessionRecordPermissionDenied -> {
                // Already denied, can't request again
                onResult(false)
            }
            AVAudioSessionRecordPermissionUndetermined -> {
                // Request permission
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    onResult(granted)
                }
            }
            else -> {
                onResult(false)
            }
        }
    }

    actual fun hasNotificationPermission(): Boolean {
        // On iOS, notification permissions are handled by AppDelegate
        // Return true as permissions are requested on app launch
        return true
    }

    actual fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        // On iOS, notification permissions are requested by AppDelegate on app launch
        // Just return true as permissions are already handled
        onResult(true)
    }
}