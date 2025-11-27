package de.zemki.metagcompose.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}

@Serializable
data class PendingEntry(
    val localId: String, // UUID for local identification
    val begin: String,
    val end: String,
    val entity_id: Int? = null,
    val case_id: Int,
    val inputs: JsonObject,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long, // Will be set when creating instances
    val lastAttempt: Long? = null,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

@Serializable
data class EntryWithSyncStatus(
    val entry: Entry? = null, // null for pending entries not yet synced
    val pendingEntry: PendingEntry? = null, // null for synced entries
    val syncStatus: SyncStatus
) {
    val id: String get() = entry?.id?.toString() ?: pendingEntry?.localId ?: ""
    val begin: String get() = entry?.begin ?: pendingEntry?.begin ?: ""
    val end: String get() = entry?.end ?: pendingEntry?.end ?: ""
    val isPending: Boolean get() = syncStatus == SyncStatus.PENDING
    val isFailed: Boolean get() = syncStatus == SyncStatus.FAILED
}

@Serializable
data class CachedEntriesData(
    val entries: List<Entry> = emptyList(),
    val pendingEntries: List<PendingEntry> = emptyList(),
    val lastSyncTimestamp: Long? = null,
    val caseId: Int
)