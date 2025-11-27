package de.zemki.metagcompose.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation of NotificationManager using Firebase Cloud Messaging
 */
actual class NotificationManager(private val context: Context) {

    actual suspend fun initialize() {
        // Initialize Firebase if not already initialized
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                android.util.Log.d("NotificationManager", "Firebase initialized")
            } else {
                android.util.Log.d("NotificationManager", "Firebase already initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to initialize Firebase", e)
            throw e
        }
    }

    actual suspend fun requestPermission(): Boolean {
        // For Android 13+ (API 33+), need to request POST_NOTIFICATIONS permission at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                android.util.Log.d("NotificationManager", "Notification permission already granted")
                return true
            } else {
                android.util.Log.w("NotificationManager", "Notification permission not granted. Request it from the Activity.")
                // Permission should be requested from the Activity using ActivityResultContract
                // For now, return false - the UI should handle the permission request
                return false
            }
        }

        // For Android 12 and below, notification permissions are granted by default
        android.util.Log.d("NotificationManager", "Notification permission granted by default (Android < 13)")
        return true
    }

    actual suspend fun getDeviceToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            // Get token from shared preferences first (if available from previous session)
            val prefs = context.getSharedPreferences("metag_prefs", Context.MODE_PRIVATE)
            val cachedToken = prefs.getString("fcm_token", null)

            if (cachedToken != null) {
                android.util.Log.d("NotificationManager", "Using cached FCM token")
                continuation.resume(cachedToken)
                return@suspendCancellableCoroutine
            }

            // If not cached, get fresh token from Firebase
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    android.util.Log.d("NotificationManager", "FCM token retrieved: ${token.take(20)}...")

                    // Cache the token
                    prefs.edit()
                        .putString("fcm_token", token)
                        .apply()

                    continuation.resume(token)
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("NotificationManager", "Failed to get FCM token", exception)
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Exception getting device token", e)
            continuation.resumeWithException(e)
        }
    }
}
