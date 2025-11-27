package de.zemki.metagcompose.util

import android.content.Context

private lateinit var appContext: Context

fun initNetworkMonitor(context: Context) {
    appContext = context.applicationContext
}

actual fun createNetworkMonitor(): NetworkMonitor {
    return NetworkMonitorImpl(appContext)
}