package de.zemki.metagcompose.data.offline

import de.zemki.metagcompose.data.model.*
import de.zemki.metagcompose.data.storage.TokenStorage
import de.zemki.metagcompose.util.currentTimeMillis
import de.zemki.metagcompose.util.generateUUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class OfflineEntryManager(
    private val tokenStorage: TokenStorage,
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
) {
    private val mutex = Mutex()
    
    suspend fun saveCachedEntries(caseId: Int, entries: List<Entry>) {
        mutex.withLock {
            val cachedData = CachedEntriesData(
                entries = entries,
                pendingEntries = getCachedPendingEntries(), // Preserve pending entries
                lastSyncTimestamp = currentTimeMillis(),
                caseId = caseId
            )
            tokenStorage.saveCachedEntries(caseId, json.encodeToString(cachedData))
        }
    }
    
    suspend fun getCachedEntries(caseId: Int): CachedEntriesData? {
        return mutex.withLock {
            tokenStorage.getCachedEntries(caseId)?.let { jsonString ->
                try {
                    json.decodeFromString<CachedEntriesData>(jsonString)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    suspend fun addPendingEntry(
        begin: String,
        end: String,
        entityId: Int?,
        caseId: Int,
        inputs: JsonObject
    ): PendingEntry {
        val pendingEntry = PendingEntry(
            localId = generateUUID(),
            begin = begin,
            end = end,
            entity_id = entityId,
            case_id = caseId,
            inputs = inputs,
            createdAt = currentTimeMillis()
        )
        
        mutex.withLock {
            val currentPending = getCachedPendingEntries()
            val updatedPending = currentPending + pendingEntry
            tokenStorage.savePendingEntries(json.encodeToString(updatedPending))
        }
        
        return pendingEntry
    }
    
    suspend fun updatePendingEntry(pendingEntry: PendingEntry) {
        mutex.withLock {
            val currentPending = getCachedPendingEntries()
            val updatedPending = currentPending.map { 
                if (it.localId == pendingEntry.localId) pendingEntry else it 
            }
            tokenStorage.savePendingEntries(json.encodeToString(updatedPending))
        }
    }
    
    suspend fun removePendingEntry(localId: String) {
        mutex.withLock {
            val currentPending = getCachedPendingEntries()
            val updatedPending = currentPending.filter { it.localId != localId }
            if (updatedPending.isEmpty()) {
                tokenStorage.clearPendingEntries()
            } else {
                tokenStorage.savePendingEntries(json.encodeToString(updatedPending))
            }
        }
    }
    
    suspend fun getPendingEntries(): List<PendingEntry> {
        return mutex.withLock {
            getCachedPendingEntries()
        }
    }
    
    suspend fun getPendingEntriesForCase(caseId: Int): List<PendingEntry> {
        return getPendingEntries().filter { it.case_id == caseId }
    }
    
    suspend fun getCombinedEntries(caseId: Int): List<EntryWithSyncStatus> {
        val cached = getCachedEntries(caseId)
        val syncedEntries = cached?.entries ?: emptyList()
        val pendingEntries = getPendingEntriesForCase(caseId)
        
        return syncedEntries.map { entry ->
            EntryWithSyncStatus(
                entry = entry,
                pendingEntry = null,
                syncStatus = SyncStatus.SYNCED
            )
        } + pendingEntries.map { pendingEntry ->
            EntryWithSyncStatus(
                entry = null,
                pendingEntry = pendingEntry,
                syncStatus = pendingEntry.syncStatus
            )
        }
    }
    
    suspend fun markEntryForRetry(localId: String, errorMessage: String) {
        val pending = getPendingEntries().find { it.localId == localId } ?: return
        
        updatePendingEntry(
            pending.copy(
                syncStatus = SyncStatus.FAILED,
                lastAttempt = currentTimeMillis(),
                retryCount = pending.retryCount + 1,
                errorMessage = errorMessage
            )
        )
    }
    
    suspend fun clearAllCachedData(caseId: Int) {
        mutex.withLock {
            tokenStorage.clearCachedEntries(caseId)
            // Remove pending entries for this case
            val remainingPending = getCachedPendingEntries().filter { it.case_id != caseId }
            if (remainingPending.isEmpty()) {
                tokenStorage.clearPendingEntries()
            } else {
                tokenStorage.savePendingEntries(json.encodeToString(remainingPending))
            }
        }
    }
    
    private suspend fun getCachedPendingEntries(): List<PendingEntry> {
        return tokenStorage.getPendingEntries()?.let { jsonString ->
            try {
                json.decodeFromString<List<PendingEntry>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
}