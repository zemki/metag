package de.zemki.metagcompose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of NotificationManager factory
 * Uses LocalContext to get the application context
 */
@Composable
actual fun createNotificationManager(): NotificationManager {
    val context = LocalContext.current.applicationContext
    return NotificationManager(context)
}
