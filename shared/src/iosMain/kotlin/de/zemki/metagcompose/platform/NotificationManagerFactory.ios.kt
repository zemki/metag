package de.zemki.metagcompose.platform

import androidx.compose.runtime.Composable

/**
 * iOS implementation of NotificationManager factory
 * No context needed for iOS
 */
@Composable
actual fun createNotificationManager(): NotificationManager {
    return NotificationManager()
}
