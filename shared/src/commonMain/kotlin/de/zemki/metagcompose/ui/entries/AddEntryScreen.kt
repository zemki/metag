package de.zemki.metagcompose.ui.entries

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.chaintech.kmp_date_time_picker.ui.datetimepicker.WheelDateTimePickerView
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*
import network.chaintech.kmp_date_time_picker.utils.DateTimePickerView
import network.chaintech.kmp_date_time_picker.utils.TimeFormat
import network.chaintech.kmp_date_time_picker.utils.WheelPickerDefaults
import network.chaintech.kmp_date_time_picker.utils.dateTimeToString
import de.zemki.metagcompose.data.model.*
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.ui.components.AudioRecordingField
import de.zemki.metagcompose.ui.theme.MetagColors
import de.zemki.metagcompose.util.getCurrentTimestamp
import de.zemki.metagcompose.util.getTimestampOneHourFromNow
import de.zemki.metagcompose.util.formatTimestampForDisplay
import de.zemki.metagcompose.util.parseTimestamp
import de.zemki.metagcompose.util.isDatePast
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import de.zemki.metagcompose.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    caseData: CaseData,
    entriesRepository: EntriesRepository,
    onBack: () -> Unit,
    onEntryCreated: () -> Unit,
    editingEntry: Entry? = null
) {
    var beginTime by remember { mutableStateOf(editingEntry?.begin ?: getCurrentTimestamp().toString()) }
    var endTime by remember { mutableStateOf(editingEntry?.end ?: getTimestampOneHourFromNow().toString()) }
    var selectedEntityId by remember { mutableStateOf(editingEntry?.media_id) }
    var selectedEntityName by remember { mutableStateOf(editingEntry?.media_name ?: "") }
    var audioData by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorDismissed by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    
    // Date picker states
    var showBeginDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Available entities from the project
    val availableEntities = caseData.project.media ?: emptyList()
    
    // Parse custom inputs to check for audio fields
    val customInputs = remember {
        try {
            de.zemki.metagcompose.data.model.InputFieldParser.parseCustomInputs(caseData.project.inputs)
        } catch (e: Exception) {
            AppLogger.d("Failed to parse custom inputs: ${e.message}")
            emptyList()
        }
    }
    val hasAudioInput = customInputs.any { it.getInputFieldType() == InputFieldType.AUDIO_RECORDING }

    // Track custom input values
    val customInputValues = remember { mutableStateMapOf<String, JsonElement>() }
    
    // Initialize all input values from existing entry if editing
    LaunchedEffect(editingEntry) {
        if (editingEntry != null) {
            val parsedInputs = editingEntry.getParsedInputs()
            parsedInputs?.let { inputs ->
                AppLogger.d("Initializing entry values from: $inputs", tag = "AddEntryScreen")

                // Check if there's a stored audio file (file ID in inputs->file)
                val fileId = (inputs["file"] as? JsonPrimitive)?.content?.toIntOrNull()
                if (fileId != null && hasAudioInput) {
                    // Set placeholder so AudioRecordingField shows "has recording" state
                    audioData = "LOADING_FILE"
                    AppLogger.d("Found stored audio file ID: $fileId, downloading...", tag = "AddEntryScreen")
                    entriesRepository.downloadAudioFile(fileId).collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                audioData = result.data
                                AppLogger.d("Downloaded audio data, length: ${result.data.length}", tag = "AddEntryScreen")
                            }
                            is ApiResult.Error -> {
                                AppLogger.d("Failed to download audio: ${result.exception.message}", tag = "AddEntryScreen")
                                // Keep placeholder so field still shows as "has recording"
                            }
                            is ApiResult.Loading -> {
                                AppLogger.d("Downloading audio file...", tag = "AddEntryScreen")
                            }
                        }
                    }
                }

                // Initialize all custom input values
                customInputs.forEach { inputDef ->
                    val fieldName = inputDef.name
                    val existingValue = inputs[fieldName]

                    when (inputDef.getInputFieldType()) {
                        InputFieldType.SCALE -> {
                            if (existingValue is JsonPrimitive) {
                                val scaleValue = existingValue.content.toIntOrNull()
                                if (scaleValue != null && scaleValue in 1..5) {
                                    customInputValues[fieldName] = JsonPrimitive(scaleValue)
                                    AppLogger.d("Initialized scale '$fieldName' = $scaleValue", tag = "AddEntryScreen")
                                }
                            }
                        }
                        InputFieldType.TEXT -> {
                            if (existingValue is JsonPrimitive) {
                                customInputValues[fieldName] = existingValue
                                AppLogger.d("Initialized text '$fieldName' = '${existingValue.content}'", tag = "AddEntryScreen")
                            }
                        }
                        InputFieldType.ONE_CHOICE -> {
                            if (existingValue is JsonPrimitive) {
                                customInputValues[fieldName] = existingValue
                                AppLogger.d("Initialized choice '$fieldName' = '${existingValue.content}'", tag = "AddEntryScreen")
                            }
                        }
                        InputFieldType.MULTIPLE_CHOICE -> {
                            if (existingValue is JsonArray) {
                                customInputValues[fieldName] = existingValue
                                AppLogger.d("Initialized multiple choice '$fieldName' = $existingValue", tag = "AddEntryScreen")
                            }
                        }
                        InputFieldType.AUDIO_RECORDING -> {
                            // Check for raw audio data (old format, before file storage)
                            if (existingValue is JsonPrimitive) {
                                val audioContent = existingValue.content
                                AppLogger.d("Found raw audio data '$fieldName', length: ${audioContent.length}", tag = "AddEntryScreen")
                                if (audioContent.isNotBlank() && audioContent.length > 10) {
                                    audioData = audioContent
                                    AppLogger.d("Set audioData for editing (raw format)", tag = "AddEntryScreen")
                                }
                            }
                            // File ID is handled above, before the loop
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    
    // Get API version to determine entity vs media terminology
    var apiVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        apiVersion = entriesRepository.getApiVersion()
    }
    
    // Extract entity name from JSON config (use "media" for v1)
    val actualEntityName = remember(apiVersion) {
        if (apiVersion == "v1") {
            "media"
        } else {
            de.zemki.metagcompose.data.model.InputFieldParser.extractEntityName(caseData.project.inputs)
                ?: caseData.project.entity_name ?: "entity"
        }
    }

    // Get useEntity flag from project (default to 1 for backward compatibility)
    val useEntity = caseData.project.use_entity ?: 1

    // Debug log - only runs when useEntity value changes
    LaunchedEffect(useEntity) {
        AppLogger.d("useEntity flag = $useEntity (from backend: ${caseData.project.use_entity})", tag = "AddEntryScreen")
    }

    fun createEntry() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            // Check if deadline has passed
            val lastDay = caseData.getLastDay()
            if (isDatePast(lastDay)) {
                isLoading = false
                errorMessage = "Submission deadline has passed. You can no longer create or edit entries."
                isErrorDismissed = false
                AppLogger.d("Entry submission blocked - deadline ($lastDay) has passed", tag = "AddEntryScreen")
                return@launch
            }

            // Validate start and end times
            val beginTimestamp = parseTimestamp(beginTime) ?: getCurrentTimestamp()
            val endTimestamp = parseTimestamp(endTime) ?: getCurrentTimestamp()
            if (beginTimestamp > endTimestamp) {
                isLoading = false
                errorMessage = "Start time cannot be after end time."
                isErrorDismissed = false
                return@launch
            }

            // Validate mandatory fields first
            val missingFields = mutableListOf<String>()
            
            customInputs.forEach { inputDef ->
                if (inputDef.mandatory) {
                    val fieldValue = customInputValues[inputDef.name]
                    val isEmpty = when (inputDef.getInputFieldType()) {
                        InputFieldType.TEXT -> {
                            (fieldValue as? JsonPrimitive)?.content?.isBlank() ?: true
                        }
                        InputFieldType.SCALE -> {
                            val scaleValue = (fieldValue as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                            scaleValue == 0 || scaleValue !in 1..5
                        }
                        InputFieldType.ONE_CHOICE -> {
                            (fieldValue as? JsonPrimitive)?.content?.isBlank() ?: true
                        }
                        InputFieldType.MULTIPLE_CHOICE -> {
                            val choices = fieldValue as? JsonArray
                            choices?.isEmpty() ?: true
                        }
                        InputFieldType.AUDIO_RECORDING -> {
                            // When editing, check if entry has a file ID stored
                            val fileId = if (editingEntry != null) {
                                (editingEntry.getParsedInputs()?.get("file") as? JsonPrimitive)?.content?.toIntOrNull()
                            } else null
                            // Field is empty if no audio data AND no file ID
                            audioData.isNullOrBlank() && fileId == null
                        }
                        else -> false
                    }
                    
                    if (isEmpty) {
                        missingFields.add(inputDef.name)
                    }
                }
            }
            
            // Check if entity/media field is required and empty (only for v1 API AND when useEntity is enabled)
            // Note: V1 API always requires media_id field regardless of available entities
            if (useEntity == 1 && apiVersion == "v1" && selectedEntityName.isBlank() && selectedEntityId == null) {
                missingFields.add(actualEntityName)
            }
            
            // If there are missing mandatory fields, show error and return
            if (missingFields.isNotEmpty()) {
                isLoading = false
                errorMessage = "Please fill in required fields: ${missingFields.joinToString(", ")}"
                isErrorDismissed = false // Reset dismissed state for new error
                return@launch
            }
            
            val inputs = buildJsonObject {
                // Add all custom input values
                customInputValues.forEach { (key, value) ->
                    put(key, value)
                }
                
                // Add audio data if available
                if (hasAudioInput && audioData != null) {
                    // Find the audio field name from customInputs
                    val audioFieldName = customInputs.find {
                        it.getInputFieldType() == InputFieldType.AUDIO_RECORDING
                    }?.name ?: "register audio"
                    // Wrap in Data URI format for backend compatibility (use mp3 to match Flutter/backend)
                    put(audioFieldName, "data:audio/mp3;base64,$audioData")
                }
            }
            
            // Determine entity_id: only send if useEntity is enabled
            val entityIdValue: JsonElement? = if (useEntity == 1) {
                when {
                    selectedEntityId != null -> JsonPrimitive(selectedEntityId)
                    selectedEntityName.isNotBlank() -> JsonPrimitive(selectedEntityName)
                    else -> null // Don't send empty entity_id (validation should catch this for V1 API)
                }
            } else {
                null // Don't send entity_id when useEntity is disabled
            }

            // Use timestamps already parsed earlier for validation

            val createRequest = CreateEntryRequest(
                begin = beginTimestamp,
                end = endTimestamp,
                case_id = caseData.id,
                entity_id = entityIdValue,
                inputs = inputs
            )
            
            if (editingEntry != null) {
                AppLogger.d("Updating entry ${editingEntry.id} for case ${caseData.id}", tag = "AddEntryScreen")
                AppLogger.d("Request: $createRequest", tag = "AddEntryScreen")
                
                entriesRepository.updateEntry(caseData.id, editingEntry.id, createRequest).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            isLoading = true
                        }
                        is ApiResult.Success -> {
                            isLoading = false
                            onEntryCreated() // This callback name is misleading but works for both create and update
                        }
                        is ApiResult.Error -> {
                            isLoading = false
                            val errorDetails = when (result.exception) {
                                is AuthError.NetworkError -> "Network error - check internet connection"
                                is AuthError.InvalidCredentials -> "Authentication error - please login again"
                                is AuthError.Unknown -> "Error: ${result.exception.message}"
                                else -> "Error: ${result.exception.message ?: "Unknown error"}"
                            }
                            errorMessage = "Failed to update entry: $errorDetails"
                            AppLogger.d("Entry update failed: $errorDetails", tag = "AddEntryScreen")
                        }
                    }
                }
            } else {
                AppLogger.d("Creating entry for case ${caseData.id}", tag = "AddEntryScreen")
                AppLogger.d("Request: $createRequest", tag = "AddEntryScreen")
                
                entriesRepository.createEntry(caseData.id, createRequest).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            isLoading = true
                        }
                        is ApiResult.Success -> {
                            isLoading = false
                            onEntryCreated()
                        }
                        is ApiResult.Error -> {
                            isLoading = false
                            val errorDetails = when (result.exception) {
                                is AuthError.NetworkError -> "Network error - check internet connection"
                                is AuthError.InvalidCredentials -> "Authentication error - please login again"
                                is AuthError.Unknown -> "Error: ${result.exception.message}"
                                else -> "Error: ${result.exception.message ?: "Unknown error"}"
                            }
                            errorMessage = "Failed to create entry: $errorDetails"
                            AppLogger.d("Entry creation failed: $errorDetails", tag = "AddEntryScreen")
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime),
            topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (editingEntry != null) 
                                stringResource(Res.string.edit_entry_title) 
                            else 
                                stringResource(Res.string.add_entry_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                // Error message in the header area - fixed height to prevent layout shifts
                val showError = errorMessage != null && !isErrorDismissed
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (showError) 120.dp else 0.dp)
                        .clickable(enabled = showError) {
                            isErrorDismissed = true
                        },
                    color = if (showError) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    if (showError) {
                        errorMessage?.let { error ->
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.error_dismiss),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AddEntryContent(
            paddingValues = paddingValues,
            beginTime = beginTime,
            endTime = endTime,
            showBeginDatePicker = showBeginDatePicker,
            showEndDatePicker = showEndDatePicker,
            onBeginTimeChange = { beginTime = it },
            onEndTimeChange = { endTime = it },
            onShowBeginDatePicker = { showBeginDatePicker = it },
            onShowEndDatePicker = { showEndDatePicker = it },
            availableEntities = availableEntities,
            actualEntityName = actualEntityName,
            selectedEntityName = selectedEntityName,
            selectedEntityId = selectedEntityId,
            onEntityNameChange = { selectedEntityName = it },
            onEntityIdChange = { selectedEntityId = it },
            useEntity = useEntity,
            customInputs = customInputs,
            customInputValues = customInputValues,
            hasAudioInput = hasAudioInput,
            audioData = audioData,
            onAudioDataChange = { audioData = it },
            errorMessage = errorMessage,
            isLoading = isLoading,
            editingEntry = editingEntry,
            onSave = { createEntry() },
            scrollState = scrollState,
            focusManager = focusManager
        )
    }
    }
}

@Composable
private fun AddEntryContent(
    paddingValues: PaddingValues,
    beginTime: String,
    endTime: String,
    showBeginDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onBeginTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onShowBeginDatePicker: (Boolean) -> Unit,
    onShowEndDatePicker: (Boolean) -> Unit,
    availableEntities: List<EntityInput>,
    actualEntityName: String,
    selectedEntityName: String,
    selectedEntityId: Int?,
    onEntityNameChange: (String) -> Unit,
    onEntityIdChange: (Int?) -> Unit,
    useEntity: Int,
    customInputs: List<InputFieldDefinition>,
    customInputValues: SnapshotStateMap<String, JsonElement>,
    hasAudioInput: Boolean,
    audioData: String?,
    onAudioDataChange: (String?) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    editingEntry: Entry?,
    onSave: () -> Unit,
    scrollState: ScrollState,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        TimePickerSection(
            beginTime = beginTime,
            endTime = endTime,
            showBeginDatePicker = showBeginDatePicker,
            showEndDatePicker = showEndDatePicker,
            onShowBeginDatePicker = onShowBeginDatePicker,
            onShowEndDatePicker = onShowEndDatePicker,
            onBeginTimeChange = onBeginTimeChange,
            onEndTimeChange = onEndTimeChange
        )

        // Only show entity selection if useEntity is enabled (1)
        if (useEntity == 1) {
            EntitySelectionSection(
                availableEntities = availableEntities,
                actualEntityName = actualEntityName,
                selectedEntityName = selectedEntityName,
                onEntitySelected = { name, id ->
                    onEntityNameChange(name)
                    onEntityIdChange(id)
                }
            )
        }
        
        CustomInputsSection(
            customInputs = customInputs,
            customInputValues = customInputValues
        )

        if (hasAudioInput) {
            // Find the audio input definition from customInputs
            val audioInputDef = customInputs.firstOrNull {
                it.getInputFieldType() == InputFieldType.AUDIO_RECORDING
            }

            if (audioInputDef != null) {
                AudioRecordingField(
                    fieldName = audioInputDef.name,
                    isMandatory = audioInputDef.mandatory,
                    initialAudioData = audioData,
                    onAudioRecorded = { base64Data ->
                        onAudioDataChange(base64Data)
                        AppLogger.d("Audio recorded, length: ${base64Data?.length ?: 0}", tag = "AddEntryScreen")
                    }
                )
            }
        }
        
        SaveButtonSection(
            isLoading = isLoading,
            editingEntry = editingEntry,
            onSave = onSave
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TimePickerSection(
    beginTime: String,
    endTime: String,
    showBeginDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onShowBeginDatePicker: (Boolean) -> Unit,
    onShowEndDatePicker: (Boolean) -> Unit,
    onBeginTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Time Period", // TODO: Add to string resources
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            TimeFieldInput(
                label = stringResource(Res.string.entry_start_time),
                value = formatTimestampForDisplay(beginTime),
                onClick = { onShowBeginDatePicker(true) }
            )
            
            TimeFieldInput(
                label = stringResource(Res.string.entry_end_time),
                value = formatTimestampForDisplay(endTime),
                onClick = { onShowEndDatePicker(true) }
            )
            
        }
    }
    
    DateTimePickerDialog(
        showBeginPicker = showBeginDatePicker,
        showEndPicker = showEndDatePicker,
        onBeginTimeChange = onBeginTimeChange,
        onEndTimeChange = onEndTimeChange,
        onDismissBegin = { onShowBeginDatePicker(false) },
        onDismissEnd = { onShowEndDatePicker(false) }
    )
}

@Composable
private fun TimeFieldInput(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit $label",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimePickerDialog(
    showBeginPicker: Boolean,
    showEndPicker: Boolean,
    onBeginTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onDismissBegin: () -> Unit,
    onDismissEnd: () -> Unit
) {
    if (showBeginPicker) {
        BeginDateTimePicker(
            onTimeChange = onBeginTimeChange,
            onDismiss = onDismissBegin
        )
    }
    
    if (showEndPicker) {
        EndDateTimePicker(
            onTimeChange = onEndTimeChange,
            onDismiss = onDismissEnd
        )
    }
}

@Composable
private fun BeginDateTimePicker(
    onTimeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Capture theme colors explicitly for iOS dark mode compatibility
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val selectedColor = MaterialTheme.colorScheme.primary

    // Use DIALOG_VIEW to prevent accidental dragging, with explicit text colors for dark mode compatibility
    WheelDateTimePickerView(
        showDatePicker = true,
        title = stringResource(Res.string.time_picker_start_title),
        doneLabel = stringResource(Res.string.time_picker_save_close),
        timeFormat = TimeFormat.HOUR_24,
        height = 300.dp,
        rowCount = 5,
        containerColor = surfaceColor,
        dateTimePickerView = DateTimePickerView.DIALOG_VIEW,
        selectorProperties = WheelPickerDefaults.selectorProperties(
            borderColor = selectedColor.copy(alpha = 0.3f)
        ),
        titleStyle = TextStyle(
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        ),
        doneLabelStyle = TextStyle(
            color = selectedColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        selectedDateTextStyle = TextStyle(
            color = selectedColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        ),
        defaultDateTextStyle = TextStyle(
            color = textColor,
            fontSize = 16.sp
        ),
        onDoneClick = { dateTime ->
            // Save the selected date/time and close
            val formattedDateTime = dateTimeToString(dateTime, "yyyy-MM-dd HH:mm:ss")
            onTimeChange("$formattedDateTime.000000")
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun EndDateTimePicker(
    onTimeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Capture theme colors explicitly for iOS dark mode compatibility
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val selectedColor = MaterialTheme.colorScheme.primary

    // Use DIALOG_VIEW to prevent accidental dragging, with explicit text colors for dark mode compatibility
    WheelDateTimePickerView(
        showDatePicker = true,
        title = stringResource(Res.string.time_picker_end_title),
        doneLabel = stringResource(Res.string.time_picker_save_close),
        timeFormat = TimeFormat.HOUR_24,
        height = 300.dp,
        rowCount = 5,
        containerColor = surfaceColor,
        dateTimePickerView = DateTimePickerView.DIALOG_VIEW,
        selectorProperties = WheelPickerDefaults.selectorProperties(
            borderColor = selectedColor.copy(alpha = 0.3f)
        ),
        titleStyle = TextStyle(
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        ),
        doneLabelStyle = TextStyle(
            color = selectedColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        selectedDateTextStyle = TextStyle(
            color = selectedColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        ),
        defaultDateTextStyle = TextStyle(
            color = textColor,
            fontSize = 16.sp
        ),
        onDoneClick = { dateTime ->
            // Save the selected date/time and close
            val formattedDateTime = dateTimeToString(dateTime, "yyyy-MM-dd HH:mm:ss")
            onTimeChange("$formattedDateTime.000000")
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun EntitySelectionSection(
    availableEntities: List<EntityInput>,
    actualEntityName: String,
    selectedEntityName: String,
    onEntitySelected: (String, Int?) -> Unit
) {
    // Entity is only required in v1 API (when it's called "media")
    val isRequired = actualEntityName.lowercase() == "media"
    if (availableEntities.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = actualEntityName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    // Show required indicator only for v1 API (media field)
                    if (isRequired) {
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                OutlinedTextField(
                    value = selectedEntityName,
                    onValueChange = { newValue ->
                        onEntitySelected(newValue, null)
                    },
                    label = { 
                        Text("${actualEntityName} ${if (isRequired) 
                            "(${stringResource(Res.string.validation_required)})" 
                        else 
                            "(${stringResource(Res.string.validation_optional)})"
                        }")
                    },
                    placeholder = { Text("${stringResource(Res.string.entity_enter)} ${actualEntityName.lowercase()}") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                EntitySuggestionsList(
                    availableEntities = availableEntities,
                    onEntitySelected = onEntitySelected
                )
            }
        }
    }
}

@Composable
private fun EntitySuggestionsList(
    availableEntities: List<EntityInput>,
    onEntitySelected: (String, Int?) -> Unit
) {
    var showSuggestions by remember { mutableStateOf(false) }
    val validEntities = availableEntities.filter { it.name.isNotBlank() }
    
    if (validEntities.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.entity_custom_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(
                onClick = { showSuggestions = !showSuggestions }
            ) {
                Text(
                    text = if (showSuggestions) 
                        stringResource(Res.string.entity_suggestions_hide) 
                    else 
                        stringResource(Res.string.entity_suggestions_show),
                    style = MaterialTheme.typography.bodySmall
                )
                Icon(
                    imageVector = if (showSuggestions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        if (showSuggestions) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                validEntities.forEach { entity ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEntitySelected(entity.name, entity.id)
                                showSuggestions = false
                            },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = entity.name,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = "Type custom name for this entry",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomInputsSection(
    customInputs: List<InputFieldDefinition>,
    customInputValues: SnapshotStateMap<String, JsonElement>
) {
    customInputs.filter { it.getInputFieldType() != InputFieldType.AUDIO_RECORDING }.forEach { inputDef ->
        CustomInputCard(
            inputDef = inputDef,
            customInputValues = customInputValues
        )
    }
}

@Composable
private fun CustomInputCard(
    inputDef: InputFieldDefinition,
    customInputValues: SnapshotStateMap<String, JsonElement>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = inputDef.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (inputDef.mandatory) {
                    Text(
                        text = "*",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            when (inputDef.getInputFieldType()) {
                InputFieldType.SCALE -> {
                    ScaleInput(
                        value = (customInputValues[inputDef.name] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                        onValueChange = { value ->
                            customInputValues[inputDef.name] = JsonPrimitive(value)
                        }
                    )
                }
                InputFieldType.TEXT -> {
                    OutlinedTextField(
                        value = (customInputValues[inputDef.name] as? JsonPrimitive)?.content ?: "",
                        onValueChange = { value ->
                            customInputValues[inputDef.name] = JsonPrimitive(value)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter ${inputDef.name}") }
                    )
                }
                InputFieldType.ONE_CHOICE -> {
                    SingleChoiceInput(
                        options = inputDef.getAvailableOptions(),
                        selectedOption = (customInputValues[inputDef.name] as? JsonPrimitive)?.content,
                        onOptionSelected = { option ->
                            customInputValues[inputDef.name] = JsonPrimitive(option)
                        }
                    )
                }
                InputFieldType.MULTIPLE_CHOICE -> {
                    MultipleChoiceInput(
                        options = inputDef.getAvailableOptions(),
                        selectedOptions = (customInputValues[inputDef.name] as? JsonArray)?.mapNotNull {
                            (it as? JsonPrimitive)?.content
                        } ?: emptyList(),
                        onSelectionChange = { selected ->
                            customInputValues[inputDef.name] = buildJsonArray {
                                selected.forEach { add(JsonPrimitive(it)) }
                            }
                        }
                    )
                }
                else -> {} // Audio handled separately
            }
        }
    }
}


@Composable
private fun SaveButtonSection(
    isLoading: Boolean,
    editingEntry: Entry?,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MetagColors.Primary,
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Text(
                    if (editingEntry != null) 
                        stringResource(Res.string.entry_updating) 
                    else 
                        stringResource(Res.string.entry_creating),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    if (editingEntry != null) 
                        stringResource(Res.string.entry_update_button) 
                    else 
                        stringResource(Res.string.entry_create_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ScaleInput(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display current value
        Text(
            text = if (value in 1..5)
                "$value / 5"
            else
                stringResource(Res.string.scale_select_rating),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value in 1..5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Scale buttons (1-5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (1..5).forEach { rating ->
                FilledTonalButton(
                    onClick = { onValueChange(rating) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (value == rating) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (value == rating)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = rating.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (value == rating) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleChoiceInput(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) }
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun MultipleChoiceInput(
    options: List<String>,
    selectedOptions: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newSelection = if (option in selectedOptions) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        onSelectionChange(newSelection)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option in selectedOptions,
                    onCheckedChange = { checked ->
                        val newSelection = if (checked) {
                            selectedOptions + option
                        } else {
                            selectedOptions - option
                        }
                        onSelectionChange(newSelection)
                    }
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}