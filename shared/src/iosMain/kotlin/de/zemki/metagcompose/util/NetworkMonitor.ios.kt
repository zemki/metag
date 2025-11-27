package de.zemki.metagcompose.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitorImpl : NetworkMonitor {
    
    private val _isConnected = MutableStateFlow(true) // Assume connected initially on iOS
    override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
    
    override suspend fun checkConnection(): Boolean {
        // For now, assume always connected on iOS
        // This can be enhanced later with proper Network framework integration
        return true
    }
}