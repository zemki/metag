package de.zemki.metagcompose.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class Entry(
    val id: Int,
    val begin: String,
    val end: String,
    val inputs: String, // Backend returns JSON string, not object
    val case_id: Int,
    val media_id: Int? = null,
    val media_name: String? = null,
    val place_id: Int? = null,
    val created_at: String,
    val updated_at: String
) {
    // Helper to parse inputs as JSON object
    fun getParsedInputs(): JsonObject? {
        return try {
            kotlinx.serialization.json.Json.parseToJsonElement(inputs).let {
                if (it is JsonObject) it else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class EntriesResponse(
    val data: List<Entry>,
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class CreateEntryRequest(
    val begin: Long,  // Unix timestamp (seconds since epoch)
    val end: Long,    // Unix timestamp (seconds since epoch)
    val case_id: Int,
    val entity_id: JsonElement? = null, // Can be JsonPrimitive(Int) or JsonPrimitive(String)
    val inputs: JsonObject
)

@Serializable
data class CreateEntryRequestV1(
    val begin: Long,  // Unix timestamp (seconds since epoch)
    val end: Long,    // Unix timestamp (seconds since epoch)
    val case_id: Int,
    val media_id: JsonElement? = null, // Can be JsonPrimitive(Int) or JsonPrimitive(String)
    val inputs: JsonObject
)

@Serializable
data class CreateEntryResponse(
    val id: Int
)