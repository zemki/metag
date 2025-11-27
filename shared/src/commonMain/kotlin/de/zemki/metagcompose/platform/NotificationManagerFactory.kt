package de.zemki.metagcompose.platform

import androidx.compose.runtime.Composable

/**
 * Factory function to create NotificationManager instance
 * Platform-specific implementations will provide the required dependencies
 */
@Composable
expect fun createNotificationManager(): NotificationManager
