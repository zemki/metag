# Data Models

## Authentication

### LoginRequest
```kotlin
// data/model/LoginModels.kt
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceID: String,
    val datetime: Long,
    val duration: Long
)
```

### LoginResponse
```kotlin
@Serializable
data class LoginResponse(
    val token: String,
    val case: CaseData,
    val notstarted: Boolean,
    val api_version: String? = null  // "v1" or "v2"
)
```

## Case & Project

### CaseData
```kotlin
// data/model/CaseModels.kt
@Serializable
data class CaseData(
    val id: Int,
    val project: Project,
    val firstDay: String? = null,
    val lastDay: String? = null
)
```

### Project
```kotlin
@Serializable
data class Project(
    val id: Int,
    val name: String,
    val inputs: String  // JSON string of InputsConfig
)
```

## Entries

### Entry (from API)
```kotlin
// data/model/EntryModels.kt
@Serializable
data class Entry(
    val id: Int,
    val begin: String,           // Unix timestamp as string
    val end: String,
    val case_id: Int,
    val inputs: String,          // JSON string of field values
    val media_id: Int? = null,   // v1 API
    val media_name: String? = null,
    val entity_id: Int? = null,  // v2 API
    val entity_name: String? = null
)
```

### CreateEntryRequest (to API)
```kotlin
@Serializable
data class CreateEntryRequest(
    val begin: Long,             // Unix timestamp
    val end: Long,
    val case_id: Int,
    val entity_id: JsonElement? = null,
    val inputs: JsonObject
)
```

## Input Fields

### InputDefinition
```kotlin
// data/model/InputFieldModels.kt
data class InputDefinition(
    val name: String,
    val type: InputFieldType,
    val mandatory: Boolean,
    val numberOfAnswers: Int,
    val answers: List<String>
)

enum class InputFieldType {
    TEXT, ONE_CHOICE, MULTIPLE_CHOICE, SCALE, AUDIO_RECORDING
}
```

## Key Utilities

| Function | File | Purpose |
|----------|------|---------|
| `parseTimestamp()` | TimeUtil.kt | String → Long timestamp |
| `formatTimestampForDisplay()` | TimeUtil.kt | Long → "26.11.2024 10:00" |
| `parseCustomInputs()` | InputFieldParser.kt | JSON → List<InputDefinition> |
