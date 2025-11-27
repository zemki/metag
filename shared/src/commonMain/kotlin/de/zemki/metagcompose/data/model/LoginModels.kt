package de.zemki.metagcompose.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceID: String? = null,
    val datetime: Long = de.zemki.metagcompose.util.getCurrentTimeSeconds() / 1000,
    val duration: Long = de.zemki.metagcompose.util.getCurrentTimeSeconds() / 1000
)

@Serializable
data class LoginResponse(
    val inputs: InputsData,
    val case: CaseData,
    val token: String,
    val file_token: String? = null,
    val duration: String,
    val custominputs: String, // PHP sends JSON string instead of array
    val notstarted: Boolean,
    val api_version: String? = null
)

@Serializable
data class InputsData(
    val entity: List<EntityInput>? = null,
    val media: List<EntityInput>? = null, // For v1 compatibility
    val entityName: String = "entity",
    val useEntity: Int = 1, // PHP sends 1/0 instead of true/false
    val custominputs: String = "" // PHP sends JSON string instead of array
)

@Serializable
data class EntityInput(
    val id: Int,
    val name: String,
    val type: String? = null,
    val description: String? = null
)

@Serializable
data class CaseData(
    val id: Int,
    val name: String,
    val duration: String,
    val project_id: Int,
    val user_id: Int,
    val created_at: String,
    val updated_at: String,
    val entries_count: Int,
    val project: ProjectData
) {
    fun getLastDay(): String? {
        // Parse duration string format: "value:X|firstDay:DD.MM.YYYY|lastDay:DD.MM.YYYY|startDay:DD.MM.YYYY|"
        val parts = duration.split("|")
        val lastDayPart = parts.find { it.startsWith("lastDay:") }
        return lastDayPart?.removePrefix("lastDay:")
    }
    
    fun getFirstDay(): String? {
        val parts = duration.split("|")
        val firstDayPart = parts.find { it.startsWith("firstDay:") }
        return firstDayPart?.removePrefix("firstDay:")
    }
    
    fun getStartDay(): String? {
        val parts = duration.split("|")
        val startDayPart = parts.find { it.startsWith("startDay:") }
        return startDayPart?.removePrefix("startDay:")
    }
}

@Serializable
data class CustomInput(
    val id: Int,
    val name: String,
    val type: String,
    val required: Boolean = false,
    val options: List<String>? = null
)

@Serializable
data class ProjectData(
    val id: Int,
    val name: String,
    val description: String,
    val inputs: String, // JSON string
    val entity_name: String? = null,
    val use_entity: Int? = null, // PHP boolean as int
    val created_by: Int,
    val is_locked: Int, // PHP boolean as int
    val created_at: String,
    val updated_at: String,
    val media: List<EntityInput>? = null
)

@Serializable
data class LoginError(
    val error: String? = null,
    val case: String? = null
)