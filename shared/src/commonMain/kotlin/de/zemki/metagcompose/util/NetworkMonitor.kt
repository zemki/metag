package de.zemki.metagcompose.util

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isConnected: Flow<Boolean>
    suspend fun checkConnection(): Boolean
}