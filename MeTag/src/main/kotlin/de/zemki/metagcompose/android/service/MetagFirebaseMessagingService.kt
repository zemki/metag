package de.zemki.metagcompose.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.zemki.metagcompose.android.MainActivity
import de.zemki.metagcompose.android.R

/**
 * Firebase Cloud Messaging Service for handling push notifications
 *
 * This service receives push notifications from Firebase and displays them as system notifications.
 * It handles both data messages and notification messages from the backend.
 */
class MetagFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "metag_notifications"
        private const val CHANNEL_NAME = "MeTag Notifications"
        private const val NOTIFICATION_ID = 1
        const val TAG = "MetagFCMService"
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on app install, device restore, or when the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "New FCM token generated: $token")

        // Store token for sending to backend on next login
        // The token will be sent during the login flow via NotificationManager
        getSharedPreferences("metag_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    /**
     * Called when a message is received from Firebase.
     * Handles both notification and data messages.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        android.util.Log.d(TAG, "Message received from: ${message.from}")

        // Get notification title and body
        val title = message.notification?.title ?: message.data["title"] ?: "MeTag"
        val body = message.notification?.body ?: message.data["message"] ?: ""

        android.util.Log.d(TAG, "Notification: title=$title, body=$body")

        // Display notification
        if (body.isNotEmpty()) {
            showNotification(title, body)
        }
    }

    /**
     * Display a system notification with the given title and message
     */
    private fun showNotification(title: String, message: String) {
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()

        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon - can be customized
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        android.util.Log.d(TAG, "Notification displayed successfully")
    }

    /**
     * Create notification channel for Android 8.0+ (Oreo)
     * This is required before posting notifications on newer Android versions
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from MeTag research project"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
