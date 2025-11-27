package de.zemki.metagcompose.util

actual fun createNetworkMonitor(): NetworkMonitor {
    return NetworkMonitorImpl()
}