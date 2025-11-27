package de.zemki.metagcompose.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import de.zemki.metagcompose.util.AppLogger

enum class InputFieldType {
    TEXT,
    MULTIPLE_CHOICE,
    ONE_CHOICE,
    SCALE,
    AUDIO_RECORDING
}

@Serializable
data class InputFieldDefinition(
    val name: String,
    val type: String,
    val numberofanswer: Int = 0,
    val mandatory: Boolean = false,
    val answers: List<String> = emptyList()
) {
    fun getInputFieldType(): InputFieldType {
        return when (type.lowercase()) {
            "text" -> InputFieldType.TEXT
            "multiple choice" -> InputFieldType.MULTIPLE_CHOICE
            "one choice" -> InputFieldType.ONE_CHOICE
            "scale" -> InputFieldType.SCALE
            "audio recording" -> InputFieldType.AUDIO_RECORDING
            else -> InputFieldType.TEXT
        }
    }
    
    fun getAvailableOptions(): List<String> {
        return when (getInputFieldType()) {
            InputFieldType.SCALE -> listOf("1", "2", "3", "4", "5")
            InputFieldType.AUDIO_RECORDING -> listOf("File", "No File")
            InputFieldType.MULTIPLE_CHOICE, InputFieldType.ONE_CHOICE -> 
                answers.filter { it.isNotEmpty() }
            InputFieldType.TEXT -> emptyList()
        }
    }
}

@Serializable
data class ParsedInputField(
    val definition: InputFieldDefinition,
    val userValue: JsonElement?
) {
    fun getDisplayValue(): String {
        return when (definition.getInputFieldType()) {
            InputFieldType.TEXT -> {
                userValue?.jsonPrimitive?.contentOrNull ?: ""
            }
            InputFieldType.ONE_CHOICE -> {
                userValue?.jsonPrimitive?.contentOrNull ?: "Not selected"
            }
            InputFieldType.MULTIPLE_CHOICE -> {
                val values = userValue?.jsonArray?.mapNotNull { 
                    it.jsonPrimitive?.contentOrNull 
                } ?: emptyList()
                if (values.isEmpty()) "None selected" else values.joinToString(", ")
            }
            InputFieldType.SCALE -> {
                val value = userValue?.jsonPrimitive?.intOrNull ?: 0
                if (value in 1..5) "$value/5" else "Not rated"
            }
            InputFieldType.AUDIO_RECORDING -> {
                val fileId = userValue?.jsonPrimitive?.intOrNull
                if (fileId != null && fileId > 0) "Audio recording available" else "No audio"
            }
        }
    }
    
    fun getSelectedOptions(): List<String> {
        return when (definition.getInputFieldType()) {
            InputFieldType.MULTIPLE_CHOICE -> {
                userValue?.jsonArray?.mapNotNull { 
                    it.jsonPrimitive?.contentOrNull 
                } ?: emptyList()
            }
            InputFieldType.ONE_CHOICE -> {
                val value = userValue?.jsonPrimitive?.contentOrNull
                if (value != null) listOf(value) else emptyList()
            }
            else -> emptyList()
        }
    }
    
    fun getScaleValue(): Int? {
        return if (definition.getInputFieldType() == InputFieldType.SCALE) {
            userValue?.jsonPrimitive?.intOrNull
        } else null
    }
    
    fun hasAudioFile(): Boolean {
        return if (definition.getInputFieldType() == InputFieldType.AUDIO_RECORDING) {
            val fileId = userValue?.jsonPrimitive?.intOrNull
            fileId != null && fileId > 0
        } else false
    }
    
    fun getAudioFileId(): Int? {
        return if (definition.getInputFieldType() == InputFieldType.AUDIO_RECORDING) {
            userValue?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
        } else null
    }
}

data class ParsedEntryData(
    val entry: Entry,
    val entityName: String?,
    val entityOptions: List<EntityInput>,
    val inputFields: List<ParsedInputField>
)

object InputFieldParser {
    fun parseCustomInputs(customInputsJson: String): List<InputFieldDefinition> {
        return try {
            // First try the expected format
            Json.decodeFromString<List<InputFieldDefinition>>(customInputsJson)
        } catch (e: Exception) {
            try {
                // Try alternative format: [{"name": "...", "type": "...", "mandatory": true/false}]
                val jsonArray = Json.parseToJsonElement(customInputsJson).jsonArray
                jsonArray.mapNotNull { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content
                    val type = obj["type"]?.jsonPrimitive?.content
                    val mandatory = obj["mandatory"]?.jsonPrimitive?.booleanOrNull ?: false
                    val numberofanswer = obj["numberofanswer"]?.jsonPrimitive?.intOrNull ?: 0
                    val answers = obj["answers"]?.jsonArray?.mapNotNull { 
                        it.jsonPrimitive?.contentOrNull 
                    } ?: emptyList()
                    
                    // Skip entity config objects
                    val isEntityConfig = obj["isEntityConfig"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (name != null && type != null && !isEntityConfig) {
                        InputFieldDefinition(
                            name = name,
                            type = type,
                            numberofanswer = numberofanswer,
                            mandatory = mandatory,
                            answers = answers
                        )
                    } else null
                }
            } catch (e2: Exception) {
                AppLogger.d("Failed to parse custom inputs in both formats: ${e.message} and ${e2.message}")
                emptyList()
            }
        }
    }
    
    fun extractEntityName(customInputsJson: String): String? {
        return try {
            val jsonArray = Json.parseToJsonElement(customInputsJson).jsonArray
            jsonArray.firstNotNullOfOrNull { element ->
                val obj = element.jsonObject
                val isEntityConfig = obj["isEntityConfig"]?.jsonPrimitive?.booleanOrNull ?: false
                if (isEntityConfig) {
                    obj["entityName"]?.jsonPrimitive?.content
                } else null
            }
        } catch (e: Exception) {
            AppLogger.d("Failed to extract entity name: ${e.message}")
            null
        }
    }
    
    fun parseEntryInputs(entry: Entry, inputDefinitions: List<InputFieldDefinition>): List<ParsedInputField> {
        val entryInputs = entry.getParsedInputs() ?: return emptyList()
        
        return inputDefinitions.map { definition ->
            val userValue = entryInputs[definition.name]
            ParsedInputField(definition, userValue)
        }
    }
    
    fun parseEntryData(
        entry: Entry, 
        customInputsJson: String,
        entityName: String,
        entityOptions: List<EntityInput>
    ): ParsedEntryData {
        val inputDefinitions = parseCustomInputs(customInputsJson)
        val inputFields = parseEntryInputs(entry, inputDefinitions)
        
        return ParsedEntryData(
            entry = entry,
            entityName = if (entry.media_name != null) entry.media_name else null,
            entityOptions = entityOptions,
            inputFields = inputFields
        )
    }
}