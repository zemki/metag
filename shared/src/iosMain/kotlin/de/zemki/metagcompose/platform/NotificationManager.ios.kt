package de.zemki.metagcompose.platform

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.resume

/**
 * iOS implementation of NotificationManager using Firebase Cloud Messaging / APNs
 *
 * The token is retrieved by AppDelegate.swift and stored in UserDefaults.
 * This Kotlin code reads the token from UserDefaults.
 */
actual class NotificationManager {

    actual suspend fun initialize() {
        // Firebase initialization happens in iOSApp.swift
    }

    actual suspend fun requestPermission(): Boolean {
        // Permission request happens in AppDelegate.swift automatically on app launch
        // Return true as AppDelegate requests permission on startup
        return true
    }

    actual suspend fun getDeviceToken(): String? {
        // Token is stored in UserDefaults by AppDelegate
        // Try multiple times with delay as token might not be available immediately
        repeat(5) { attempt ->
            val token = NSUserDefaults.standardUserDefaults.stringForKey("fcm_token")
            if (token != null) {
                return token
            }
            delay(1000) // Wait 1 second between attempts
        }

        return null
    }
}
