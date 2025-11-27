package de.zemki.metagcompose.data.repository

import de.zemki.metagcompose.data.model.*
import de.zemki.metagcompose.data.offline.OfflineEntryManager
import de.zemki.metagcompose.data.storage.TokenStorage
import de.zemki.metagcompose.network.createHttpClient
import de.zemki.metagcompose.util.NetworkMonitor
import de.zemki.metagcompose.util.parseTimestamp
import de.zemki.metagcompose.util.getCurrentTimestamp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import de.zemki.metagcompose.util.AppLogger

class EntriesRepository(
    private val tokenStorage: TokenStorage,
    private val networkMonitor: NetworkMonitor,
    private val offlineManager: OfflineEntryManager
) {
    private val httpClient: HttpClient by lazy { createHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun getApiVersion(): String? {
        return tokenStorage.getApiVersion()
    }
    
    fun getEntries(caseId: Int): Flow<ApiResult<List<Entry>>> = flow {
        emit(ApiResult.Loading)
        
        // Check network connectivity
        val isConnected = networkMonitor.checkConnection()
        
        if (!isConnected) {
            // Return cached entries when offline
            val cached = offlineManager.getCachedEntries(caseId)
            if (cached != null && cached.entries.isNotEmpty()) {
                emit(ApiResult.Success(cached.entries))
            } else {
                emit(ApiResult.Error(AuthError.NetworkError))
            }
            return@flow
        }
        
        try {
            val token = tokenStorage.getToken()
            val baseUrl = tokenStorage.getBaseUrl()
            val apiVersion = tokenStorage.getApiVersion() ?: "v1"
            
            AppLogger.d("Getting entries for case $caseId", tag = "EntriesRepository")
            AppLogger.d("Token available: ${token != null}", tag = "EntriesRepository")
            AppLogger.d("Base URL: $baseUrl", tag = "EntriesRepository")
            AppLogger.d("API Version: $apiVersion", tag = "EntriesRepository")
            
            if (token == null || baseUrl == null) {
                emit(ApiResult.Error(AuthError.Unknown("Not authenticated")))
                return@flow
            }
            
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val fullUrl = "$cleanBaseUrl/api/$apiVersion/entry/$caseId"
            
            AppLogger.d("Fetching from: $fullUrl", tag = "EntriesRepository")
            
            val response: HttpResponse = httpClient.get(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                }
            }
            
            val responseBody = response.bodyAsText()
            AppLogger.d("Response status: ${response.status}", tag = "EntriesRepository")
            AppLogger.d("Response body length: ${responseBody.length}", tag = "EntriesRepository")
            if (responseBody.length < 1000) {
                AppLogger.d("Response body: $responseBody", tag = "EntriesRepository")
            } else {
                AppLogger.d("Response body (first 500 chars): ${responseBody.take(500)}", tag = "EntriesRepository")
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        // Backend returns data wrapped in {"data": [...]} format
                        val entriesResponse = json.decodeFromString<EntriesResponse>(responseBody)
                        // Cache the entries for offline use
                        offlineManager.saveCachedEntries(caseId, entriesResponse.data)
                        emit(ApiResult.Success(entriesResponse.data))
                    } catch (e: Exception) {
                        AppLogger.d("Parse error: ${e.message}", tag = "EntriesRepository")
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse entries: ${e.message}")))
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    tokenStorage.clearToken()
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                else -> {
                    emit(ApiResult.Error(AuthError.Unknown("HTTP ${response.status}: $responseBody")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("Network error: ${e.message}", tag = "EntriesRepository")
            e.printStackTrace()
            // Fall back to cached data on network error
            val cached = offlineManager.getCachedEntries(caseId)
            if (cached != null && cached.entries.isNotEmpty()) {
                emit(ApiResult.Success(cached.entries))
            } else {
                emit(ApiResult.Error(AuthError.NetworkError))
            }
        }
    }
    
    fun createEntry(caseId: Int, request: CreateEntryRequest): Flow<ApiResult<Entry>> = flow {
        emit(ApiResult.Loading)
        
        // Check network connectivity
        val isConnected = networkMonitor.checkConnection()
        
        if (!isConnected) {
            // Save as pending entry when offline
            val pendingEntry = offlineManager.addPendingEntry(
                begin = request.begin.toString(), // Convert Long to String
                end = request.end.toString(),     // Convert Long to String
                entityId = null, // TODO: Handle JsonElement to Int conversion
                caseId = caseId,
                inputs = request.inputs
            )
            
            // Return a temporary Entry object for UI consistency
            val tempEntry = Entry(
                id = -1, // Temporary ID
                begin = pendingEntry.begin,
                end = pendingEntry.end,
                inputs = pendingEntry.inputs.toString(),
                case_id = pendingEntry.case_id,
                media_id = null,
                media_name = null,
                place_id = null,
                created_at = "",
                updated_at = ""
            )
            
            emit(ApiResult.Success(tempEntry))
            return@flow
        }
        
        try {
            val token = tokenStorage.getToken()
            val baseUrl = tokenStorage.getBaseUrl()
            val apiVersion = tokenStorage.getApiVersion() ?: "v1"
            
            if (token == null || baseUrl == null) {
                emit(ApiResult.Error(AuthError.Unknown("Not authenticated")))
                return@flow
            }
            
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val fullUrl = "$cleanBaseUrl/api/$apiVersion/cases/$caseId/entries" // Both v1 and v2 use the same format

            // Check if inputs contains audio data - look for Data URI or base64 encoded audio
            val audioFieldEntry = request.inputs.entries.find { (_, value) ->
                if (value is JsonPrimitive) {
                    val content = value.content
                    // Audio data can be: Data URI format, very long base64, or start with audio format markers
                    content.startsWith("data:audio/") || content.length > 10000 || content.startsWith("AAAA") || content.startsWith("//")
                } else false
            }

            val audioData = audioFieldEntry?.value as? JsonPrimitive

            // Remove audio from inputs if present
            val cleanedInputs = if (audioData != null) {
                kotlinx.serialization.json.buildJsonObject {
                    request.inputs.entries.forEach { (key, value) ->
                        if (key != audioFieldEntry.key) {
                            put(key, value)
                        }
                    }
                }
            } else {
                request.inputs
            }

            // Convert request to appropriate format based on API version
            val requestBody: Any = if (apiVersion == "v1") {
                if (audioData != null) {
                    // Build custom JSON with audio field
                    kotlinx.serialization.json.buildJsonObject {
                        put("begin", JsonPrimitive(request.begin))
                        put("end", JsonPrimitive(request.end))
                        put("case_id", JsonPrimitive(request.case_id))
                        request.entity_id?.let { put("media_id", it) }
                        put("inputs", cleanedInputs)
                        put("audio", audioData)
                    }
                } else {
                    CreateEntryRequestV1(
                        begin = request.begin,
                        end = request.end,
                        case_id = request.case_id,
                        media_id = request.entity_id, // Convert entity_id to media_id for v1
                        inputs = cleanedInputs
                    )
                }
            } else {
                if (audioData != null) {
                    // Build custom JSON with audio field
                    kotlinx.serialization.json.buildJsonObject {
                        put("begin", JsonPrimitive(request.begin))
                        put("end", JsonPrimitive(request.end))
                        put("case_id", JsonPrimitive(request.case_id))
                        request.entity_id?.let { put("entity_id", it) }
                        put("inputs", cleanedInputs)
                        put("audio", audioData)
                    }
                } else {
                    request
                }
            }

            AppLogger.d("Creating entry at URL: $fullUrl", tag = "EntriesRepository")
            AppLogger.d("Has audio data: ${audioData != null}", tag = "EntriesRepository")
            if (audioData != null) {
                AppLogger.d("Audio data length: ${audioData.content.length}", tag = "EntriesRepository")
            }

            // First, try to sync any pending entries
            syncPendingEntries()

            // Get file token if available
            val fileToken = tokenStorage.getFileToken()

            val response: HttpResponse = httpClient.post(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                    // Add x-file-token header if we have audio data and file token
                    if (audioData != null && fileToken != null) {
                        append("x-file-token", fileToken)
                        AppLogger.d("Added x-file-token header", tag = "EntriesRepository")
                    }
                }
                // Handle JsonObject separately - convert to string to avoid serialization issues
                when (requestBody) {
                    is JsonObject -> {
                        setBody(TextContent(requestBody.toString(), ContentType.Application.Json))
                    }
                    else -> {
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                }
            }
            
            val responseBody = response.bodyAsText()
            AppLogger.d("Response status: ${response.status}", tag = "EntriesRepository")
            AppLogger.d("Response body: $responseBody", tag = "EntriesRepository")
            
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    try {
                        val createResponse = json.decodeFromString<CreateEntryResponse>(responseBody)
                        // Backend only returns the ID, so create a minimal Entry object
                        val createdEntry = Entry(
                            id = createResponse.id,
                            begin = request.begin.toString(), // Convert Long to String
                            end = request.end.toString(),     // Convert Long to String
                            inputs = request.inputs.toString(),
                            case_id = request.case_id,
                            media_id = null, // Will be set based on entity_id handling
                            media_name = null,
                            place_id = null,
                            created_at = "",
                            updated_at = ""
                        )
                        emit(ApiResult.Success(createdEntry))
                    } catch (e: Exception) {
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse created entry: ${e.message}")))
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    tokenStorage.clearToken()
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                else -> {
                    emit(ApiResult.Error(AuthError.Unknown("HTTP ${response.status}: $responseBody")))
                }
            }
        } catch (e: Exception) {
            // Save as pending entry on network error
            val pendingEntry = offlineManager.addPendingEntry(
                begin = request.begin.toString(), // Convert Long to String
                end = request.end.toString(),     // Convert Long to String
                entityId = null, // TODO: Handle JsonElement to Int conversion
                caseId = caseId,
                inputs = request.inputs
            )
            
            // Return a temporary Entry object
            val tempEntry = Entry(
                id = -1,
                begin = pendingEntry.begin,
                end = pendingEntry.end,
                inputs = pendingEntry.inputs.toString(),
                case_id = pendingEntry.case_id,
                media_id = null,
                media_name = null,
                place_id = null,
                created_at = "",
                updated_at = ""
            )
            
            emit(ApiResult.Success(tempEntry))
        }
    }
    
    fun updateEntry(caseId: Int, entryId: Int, request: CreateEntryRequest): Flow<ApiResult<Entry>> = flow {
        emit(ApiResult.Loading)
        
        // Check network connectivity
        val isConnected = networkMonitor.checkConnection()
        
        if (!isConnected) {
            // For offline updates, we'd need more complex logic
            // For now, return an error to prevent data inconsistency
            emit(ApiResult.Error(AuthError.NetworkError))
            return@flow
        }
        
        try {
            val token = tokenStorage.getToken()
            val baseUrl = tokenStorage.getBaseUrl()
            val apiVersion = tokenStorage.getApiVersion() ?: "v1"
            
            if (token == null || baseUrl == null) {
                emit(ApiResult.Error(AuthError.Unknown("Not authenticated")))
                return@flow
            }
            
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val fullUrl = "$cleanBaseUrl/api/$apiVersion/cases/$caseId/entries/$entryId" // Both v1 and v2 use the same format

            // Check if inputs contains audio data - look for Data URI or base64 encoded audio
            val audioFieldEntry = request.inputs.entries.find { (_, value) ->
                if (value is JsonPrimitive) {
                    val content = value.content
                    // Audio data can be: Data URI format, very long base64, or start with audio format markers
                    content.startsWith("data:audio/") || content.length > 10000 || content.startsWith("AAAA") || content.startsWith("//")
                } else false
            }

            val audioData = audioFieldEntry?.value as? JsonPrimitive

            // Remove audio from inputs if present
            val cleanedInputs = if (audioData != null) {
                kotlinx.serialization.json.buildJsonObject {
                    request.inputs.entries.forEach { (key, value) ->
                        if (key != audioFieldEntry.key) {
                            put(key, value)
                        }
                    }
                }
            } else {
                request.inputs
            }

            // Convert request to appropriate format based on API version
            val requestBody: Any = if (apiVersion == "v1") {
                if (audioData != null) {
                    // Build custom JSON with audio field
                    kotlinx.serialization.json.buildJsonObject {
                        put("begin", JsonPrimitive(request.begin))
                        put("end", JsonPrimitive(request.end))
                        put("case_id", JsonPrimitive(request.case_id))
                        request.entity_id?.let { put("media_id", it) }
                        put("inputs", cleanedInputs)
                        put("audio", audioData)
                    }
                } else {
                    CreateEntryRequestV1(
                        begin = request.begin,
                        end = request.end,
                        case_id = request.case_id,
                        media_id = request.entity_id, // Convert entity_id to media_id for v1
                        inputs = cleanedInputs
                    )
                }
            } else {
                if (audioData != null) {
                    // Build custom JSON with audio field
                    kotlinx.serialization.json.buildJsonObject {
                        put("begin", JsonPrimitive(request.begin))
                        put("end", JsonPrimitive(request.end))
                        put("case_id", JsonPrimitive(request.case_id))
                        request.entity_id?.let { put("entity_id", it) }
                        put("inputs", cleanedInputs)
                        put("audio", audioData)
                    }
                } else {
                    request
                }
            }

            AppLogger.d("Updating entry at URL: $fullUrl", tag = "EntriesRepository")
            AppLogger.d("Has audio data: ${audioData != null}", tag = "EntriesRepository")
            if (audioData != null) {
                AppLogger.d("Audio data length: ${audioData.content.length}", tag = "EntriesRepository")
            }

            // Get file token if available
            val fileToken = tokenStorage.getFileToken()

            val response: HttpResponse = httpClient.patch(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                    // Add x-file-token header if we have audio data and file token
                    if (audioData != null && fileToken != null) {
                        append("x-file-token", fileToken)
                        AppLogger.d("Added x-file-token header for update", tag = "EntriesRepository")
                    }
                }
                // Handle JsonObject separately - convert to string to avoid serialization issues
                when (requestBody) {
                    is JsonObject -> {
                        setBody(TextContent(requestBody.toString(), ContentType.Application.Json))
                    }
                    else -> {
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                }
            }
            
            val responseBody = response.bodyAsText()
            AppLogger.d("Update response status: ${response.status}", tag = "EntriesRepository")
            AppLogger.d("Update response body: $responseBody", tag = "EntriesRepository")
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        val updateResponse = json.decodeFromString<CreateEntryResponse>(responseBody)
                        // Backend only returns the ID, so create a minimal Entry object
                        val updatedEntry = Entry(
                            id = updateResponse.id,
                            begin = request.begin.toString(), // Convert Long to String
                            end = request.end.toString(),     // Convert Long to String
                            inputs = request.inputs.toString(),
                            case_id = request.case_id,
                            media_id = null, // Will be set based on entity_id handling
                            media_name = null,
                            place_id = null,
                            created_at = "",
                            updated_at = ""
                        )
                        emit(ApiResult.Success(updatedEntry))
                    } catch (e: Exception) {
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse updated entry: ${e.message}")))
                    }
                }
                HttpStatusCode.NotFound -> {
                    emit(ApiResult.Error(AuthError.Unknown("Entry not found")))
                }
                HttpStatusCode.Unauthorized -> {
                    tokenStorage.clearToken()
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                HttpStatusCode.Forbidden -> {
                    emit(ApiResult.Error(AuthError.Unknown("Access denied - entry may be blocked or past case end date")))
                }
                else -> {
                    emit(ApiResult.Error(AuthError.Unknown("HTTP ${response.status}: $responseBody")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("Update error: ${e.message}", tag = "EntriesRepository")
            e.printStackTrace()
            emit(ApiResult.Error(AuthError.NetworkError))
        }
    }
    
    fun deleteEntry(caseId: Int, entryId: Int): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        
        try {
            val token = tokenStorage.getToken()
            val baseUrl = tokenStorage.getBaseUrl()
            val apiVersion = tokenStorage.getApiVersion() ?: "v1"
            
            AppLogger.d("Deleting entry $entryId", tag = "EntriesRepository")
            AppLogger.d("Token available: ${token != null}", tag = "EntriesRepository")
            AppLogger.d("Base URL: $baseUrl", tag = "EntriesRepository")
            AppLogger.d("API Version: $apiVersion", tag = "EntriesRepository")
            
            if (token == null || baseUrl == null) {
                emit(ApiResult.Error(AuthError.Unknown("Not authenticated")))
                return@flow
            }
            
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val fullUrl = "$cleanBaseUrl/api/$apiVersion/cases/$caseId/entries/$entryId" // Both v1 and v2 use the same format
            
            AppLogger.d("DELETE request to: $fullUrl", tag = "EntriesRepository")
            
            val response: HttpResponse = httpClient.delete(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                }
            }
            
            val responseBody = response.bodyAsText()
            AppLogger.d("Delete response status: ${response.status}", tag = "EntriesRepository")
            AppLogger.d("Delete response body: $responseBody", tag = "EntriesRepository")
            
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> {
                    AppLogger.d("Entry $entryId deleted successfully", tag = "EntriesRepository")
                    emit(ApiResult.Success(true))
                }
                HttpStatusCode.NotFound -> {
                    emit(ApiResult.Error(AuthError.Unknown("Entry not found")))
                }
                HttpStatusCode.Unauthorized -> {
                    tokenStorage.clearToken()
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                HttpStatusCode.Forbidden -> {
                    emit(ApiResult.Error(AuthError.Unknown("Access denied - entries may be blocked")))
                }
                else -> {
                    emit(ApiResult.Error(AuthError.Unknown("Failed to delete entry: HTTP ${response.status}")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("Delete error: ${e.message}", tag = "EntriesRepository")
            e.printStackTrace()
            emit(ApiResult.Error(AuthError.NetworkError))
        }
    }
    
    // Get combined entries (synced + pending)
    suspend fun getCombinedEntries(caseId: Int): List<EntryWithSyncStatus> {
        return offlineManager.getCombinedEntries(caseId)
    }
    
    // Sync pending entries when network is available
    suspend fun syncPendingEntries() {
        if (!networkMonitor.checkConnection()) return
        
        val pendingEntries = offlineManager.getPendingEntries()
        for (pending in pendingEntries) {
            if (pending.syncStatus == SyncStatus.FAILED && pending.retryCount >= 3) {
                continue // Skip entries that have failed too many times
            }
            
            try {
                val token = tokenStorage.getToken() ?: continue
                val baseUrl = tokenStorage.getBaseUrl() ?: continue
                val apiVersion = tokenStorage.getApiVersion() ?: "v1"
                
                val request = CreateEntryRequest(
                    begin = parseTimestamp(pending.begin) ?: getCurrentTimestamp(), // Convert String to Long
                    end = parseTimestamp(pending.end) ?: getCurrentTimestamp(),     // Convert String to Long
                    case_id = pending.case_id,
                    entity_id = pending.entity_id?.let { JsonPrimitive(it) },
                    inputs = pending.inputs
                )
                
                // Convert request to appropriate format based on API version
                val requestBody = if (apiVersion == "v1") {
                    CreateEntryRequestV1(
                        begin = request.begin,
                        end = request.end,
                        case_id = request.case_id,
                        media_id = request.entity_id, // Convert entity_id to media_id for v1
                        inputs = request.inputs
                    )
                } else {
                    request
                }
                
                val syncUrl = "$baseUrl/api/$apiVersion/cases/${pending.case_id}/entries" // Both v1 and v2 use the same format
                
                val response: HttpResponse = httpClient.post(syncUrl) {
                    headers {
                        append("Authorization", "Bearer $token")
                        append("Accept", "application/json")
                        append("ngrok-skip-browser-warning", "true")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    // Remove successfully synced entry
                    offlineManager.removePendingEntry(pending.localId)
                } else {
                    // Mark as failed
                    offlineManager.markEntryForRetry(
                        pending.localId,
                        "HTTP ${response.status}"
                    )
                }
            } catch (e: Exception) {
                // Mark as failed with error message
                offlineManager.markEntryForRetry(
                    pending.localId,
                    e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun downloadAudioFile(fileId: Int): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)
        
        try {
            val token = tokenStorage.getToken()
            val baseUrl = tokenStorage.getBaseUrl()
            val apiVersion = tokenStorage.getApiVersion() ?: "v1"
            
            if (token == null || baseUrl == null) {
                emit(ApiResult.Error(AuthError.Unknown("Not authenticated")))
                return@flow
            }
            
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val fullUrl = "$cleanBaseUrl/api/$apiVersion/files/$fileId"

            AppLogger.d("Downloading audio file $fileId from: $fullUrl", tag = "EntriesRepository")

            val response: HttpResponse = httpClient.get(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                }
            }
            
            val responseBody = response.bodyAsText()
            AppLogger.d("Audio download response status: ${response.status}", tag = "EntriesRepository")
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        // Parse JSON response to extract audio data
                        val jsonResponse = json.parseToJsonElement(responseBody)
                        if (jsonResponse is kotlinx.serialization.json.JsonObject) {
                            val dataElement = jsonResponse["data"]
                            if (dataElement is kotlinx.serialization.json.JsonPrimitive) {
                                val audioData = dataElement.content
                                emit(ApiResult.Success(audioData))
                            } else {
                                emit(ApiResult.Error(AuthError.Unknown("Invalid audio file format")))
                            }
                        } else {
                            emit(ApiResult.Error(AuthError.Unknown("Invalid response format")))
                        }
                    } catch (e: Exception) {
                        AppLogger.d("Failed to parse audio response: ${e.message}", tag = "EntriesRepository")
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse audio file")))
                    }
                }
                HttpStatusCode.NotFound -> {
                    emit(ApiResult.Error(AuthError.Unknown("Audio file not found")))
                }
                HttpStatusCode.Unauthorized -> {
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                else -> {
                    emit(ApiResult.Error(AuthError.Unknown("Failed to download audio file")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("Audio download error: ${e.message}", tag = "EntriesRepository")
            emit(ApiResult.Error(AuthError.NetworkError))
        }
    }
}