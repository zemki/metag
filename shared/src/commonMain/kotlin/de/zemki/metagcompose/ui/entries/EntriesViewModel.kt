package de.zemki.metagcompose.ui.entries

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.zemki.metagcompose.data.model.ApiResult
import de.zemki.metagcompose.data.model.AuthError
import de.zemki.metagcompose.data.model.Entry
import de.zemki.metagcompose.data.model.EntryWithSyncStatus
import de.zemki.metagcompose.data.model.InputFieldParser
import de.zemki.metagcompose.data.model.InputFieldType
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.util.NetworkMonitor
import de.zemki.metagcompose.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

class EntriesViewModel(
    private val entriesRepository: EntriesRepository,
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope,
    private val caseId: Int,
    private val customInputsJson: String
) {
    private val _uiState = mutableStateOf(EntriesUiState())
    val uiState: State<EntriesUiState> = _uiState

    private val customInputs = try {
        InputFieldParser.parseCustomInputs(customInputsJson)
    } catch (e: Exception) {
        AppLogger.d("Failed to parse custom inputs: ${e.message}", tag = "EntriesViewModel")
        emptyList()
    }

    init {
        loadEntries()
        observeNetworkState()
    }
    
    fun loadEntries() {
        coroutineScope.launch {
            // Always load combined entries (synced + pending)
            val combinedEntries = entriesRepository.getCombinedEntries(caseId)
            _uiState.value = _uiState.value.copy(
                entries = combinedEntries.mapNotNull { it.entry }, // Extract only non-null Entry objects for UI
                entriesWithStatus = combinedEntries
            )
            
            // Then try to fetch from server
            entriesRepository.getEntries(caseId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is ApiResult.Success -> {
                        // Reload combined entries after successful sync
                        val updatedEntries = entriesRepository.getCombinedEntries(caseId)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            entries = result.data, // Add the regular entries list
                            entriesWithStatus = updatedEntries,
                            errorMessage = null
                        )
                        // Pre-load audio files for all entries
                        preloadAudioFiles(result.data)
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = if (result.exception is AuthError.NetworkError && _uiState.value.entriesWithStatus.isNotEmpty()) {
                                "Offline mode - showing cached entries"
                            } else {
                                getErrorMessage(result.exception)
                            }
                        )
                    }
                }
            }
        }
    }
    
    fun deleteEntry(entryId: Int) {
        coroutineScope.launch {
            entriesRepository.deleteEntry(caseId, entryId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is ApiResult.Success -> {
                        // Refresh entries after successful delete
                        loadEntries()
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = getErrorMessage(result.exception)
                        )
                    }
                }
            }
        }
    }

    private fun preloadAudioFiles(entries: List<Entry>) {
        coroutineScope.launch {
            // Check if project has audio input fields
            val hasAudioInput = customInputs.any {
                it.getInputFieldType() == InputFieldType.AUDIO_RECORDING
            }

            if (!hasAudioInput) {
                AppLogger.d("Project has no audio fields, skipping pre-load", tag = "EntriesViewModel")
                return@launch
            }

            // Find entries with audio files
            val entriesWithAudio = mutableListOf<Pair<Int, Int>>() // entryId to fileId

            entries.forEach { entry ->
                val parsedInputs = try {
                    entry.getParsedInputs()
                } catch (e: Exception) {
                    AppLogger.d("Failed to parse inputs for entry ${entry.id}: ${e.message}", tag = "EntriesViewModel")
                    null
                }

                if (parsedInputs != null) {
                    // Check for file ID (new format)
                    val fileId = (parsedInputs["file"] as? JsonPrimitive)?.content?.toIntOrNull()
                    if (fileId != null) {
                        entriesWithAudio.add(entry.id to fileId)
                    } else {
                        // Check for inline audio (old format - already loaded)
                        var hasInlineAudio = false
                        customInputs.forEach { inputDef ->
                            if (inputDef.getInputFieldType() == InputFieldType.AUDIO_RECORDING) {
                                val value = parsedInputs[inputDef.name] as? JsonPrimitive
                                if (value != null && value.content.length > 1000) {
                                    // Already has inline audio, cache it
                                    _uiState.value = _uiState.value.copy(
                                        audioCache = _uiState.value.audioCache + (entry.id to value.content),
                                        audioLoadingStatus = _uiState.value.audioLoadingStatus + (entry.id to AudioLoadStatus.SUCCESS)
                                    )
                                    hasInlineAudio = true
                                    AppLogger.d("Entry ${entry.id} has inline audio", tag = "EntriesViewModel")
                                }
                            }
                        }
                        if (!hasInlineAudio) {
                            // Mark as not needed if no audio found
                            _uiState.value = _uiState.value.copy(
                                audioLoadingStatus = _uiState.value.audioLoadingStatus + (entry.id to AudioLoadStatus.NOT_NEEDED)
                            )
                        }
                    }
                } else {
                    // Mark as not needed if can't parse
                    _uiState.value = _uiState.value.copy(
                        audioLoadingStatus = _uiState.value.audioLoadingStatus + (entry.id to AudioLoadStatus.NOT_NEEDED)
                    )
                }
            }

            if (entriesWithAudio.isEmpty()) {
                AppLogger.d("No entries with audio files to download", tag = "EntriesViewModel")
                return@launch
            }

            // Initialize progress
            _uiState.value = _uiState.value.copy(
                audioLoadProgress = AudioLoadProgress(
                    totalCount = entriesWithAudio.size,
                    loadedCount = 0,
                    failedCount = 0
                )
            )

            AppLogger.d("Starting audio pre-load for ${entriesWithAudio.size} entries", tag = "EntriesViewModel")

            // Download all audio files concurrently
            entriesWithAudio.forEach { (entryId, fileId) ->
                coroutineScope.launch {
                    downloadAudioForEntry(entryId, fileId)
                }
            }
        }
    }

    private suspend fun downloadAudioForEntry(entryId: Int, fileId: Int) {
        // Mark as loading
        _uiState.value = _uiState.value.copy(
            audioLoadingStatus = _uiState.value.audioLoadingStatus + (entryId to AudioLoadStatus.LOADING)
        )

        entriesRepository.downloadAudioFile(fileId).collect { result ->
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        audioCache = _uiState.value.audioCache + (entryId to result.data),
                        audioLoadingStatus = _uiState.value.audioLoadingStatus + (entryId to AudioLoadStatus.SUCCESS),
                        audioLoadProgress = _uiState.value.audioLoadProgress?.let {
                            it.copy(loadedCount = it.loadedCount + 1)
                        }
                    )
                    AppLogger.d("Audio loaded for entry $entryId, file $fileId", tag = "EntriesViewModel")
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        audioLoadingStatus = _uiState.value.audioLoadingStatus + (entryId to AudioLoadStatus.FAILED),
                        audioLoadProgress = _uiState.value.audioLoadProgress?.let {
                            it.copy(failedCount = it.failedCount + 1)
                        }
                    )
                    AppLogger.d("Audio load failed for entry $entryId, file $fileId: ${result.exception.message}", tag = "EntriesViewModel")
                }
                is ApiResult.Loading -> {
                    // Already marked as loading
                }
            }
        }
    }

    fun retryAudioDownload(entryId: Int) {
        coroutineScope.launch {
            // Find the entry and its file ID
            val entry = _uiState.value.entries.find { it.id == entryId }
            if (entry == null) {
                AppLogger.d("Entry $entryId not found for retry", tag = "EntriesViewModel")
                return@launch
            }

            val parsedInputs = try {
                entry.getParsedInputs()
            } catch (e: Exception) {
                AppLogger.d("Failed to parse inputs for retry: ${e.message}", tag = "EntriesViewModel")
                return@launch
            }

            val fileId = (parsedInputs?.get("file") as? JsonPrimitive)?.content?.toIntOrNull()
            if (fileId == null) {
                AppLogger.d("No file ID found for entry $entryId", tag = "EntriesViewModel")
                return@launch
            }

            AppLogger.d("Retrying audio download for entry $entryId, file $fileId", tag = "EntriesViewModel")

            // Decrement failed count before retry to prevent accumulation
            _uiState.value = _uiState.value.copy(
                audioLoadProgress = _uiState.value.audioLoadProgress?.let {
                    it.copy(failedCount = maxOf(0, it.failedCount - 1))
                }
            )

            downloadAudioForEntry(entryId, fileId)
        }
    }

    private fun observeNetworkState() {
        networkMonitor.isConnected
            .onEach { isConnected ->
                _uiState.value = _uiState.value.copy(isOnline = isConnected)
                if (isConnected) {
                    // Try to sync pending entries when coming back online
                    coroutineScope.launch {
                        entriesRepository.syncPendingEntries()
                        loadEntries() // Reload to update sync status
                    }
                }
            }
            .launchIn(coroutineScope)
    }
    
    private fun getErrorMessage(error: Exception): String {
        return when (error) {
            is AuthError.InvalidCredentials -> "Session expired. Please login again."
            is AuthError.NetworkError -> "Network error. Please check your connection."
            else -> "Failed to load entries: ${error.message}"
        }
    }
}

data class EntriesUiState(
    val entries: List<Entry> = emptyList(), // Keep for backward compatibility
    val entriesWithStatus: List<EntryWithSyncStatus> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOnline: Boolean = true,
    val audioCache: Map<Int, String> = emptyMap(), // entryId -> base64 audio
    val audioLoadingStatus: Map<Int, AudioLoadStatus> = emptyMap(), // entryId -> status
    val audioLoadProgress: AudioLoadProgress? = null
)

enum class AudioLoadStatus {
    LOADING,
    SUCCESS,
    FAILED,
    NOT_NEEDED
}

data class AudioLoadProgress(
    val totalCount: Int,
    val loadedCount: Int,
    val failedCount: Int
)