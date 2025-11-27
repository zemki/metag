package de.zemki.metagcompose.util

expect class PermissionHandler() {
    fun hasAudioPermission(): Boolean
    fun requestAudioPermission(onResult: (Boolean) -> Unit)
    fun hasNotificationPermission(): Boolean
    fun requestNotificationPermission(onResult: (Boolean) -> Unit)
}