package de.zemki.metagcompose.platform

/**
 * Cross-platform notification manager for handling push notifications
 *
 * This expect class provides a platform-agnostic interface for:
 * - Initializing push notification services
 * - Requesting notification permissions
 * - Getting device tokens for push notifications
 */
expect class NotificationManager {
    /**
     * Initialize the notification system.
     * On Android: Initializes Firebase
     * On iOS: Configures APNs through Firebase
     */
    suspend fun initialize()

    /**
     * Request notification permissions from the user.
     * On Android 13+: Shows runtime permission dialog
     * On iOS: Shows iOS notification permission alert
     *
     * @return true if permission granted, false otherwise
     */
    suspend fun requestPermission(): Boolean

    /**
     * Get the device token for push notifications.
     * On Android: Returns FCM token
     * On iOS: Returns APNS token through Firebase
     *
     * @return Device token string, or null if unavailable
     */
    suspend fun getDeviceToken(): String?
}
