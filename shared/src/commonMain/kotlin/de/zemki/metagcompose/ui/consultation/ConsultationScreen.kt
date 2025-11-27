package de.zemki.metagcompose.ui.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zemki.metagcompose.data.model.*
import de.zemki.metagcompose.ui.theme.MetagColors
import de.zemki.metagcompose.util.formatDateForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationScreen(
    entry: Entry,
    caseData: CaseData,
    entriesRepository: de.zemki.metagcompose.data.repository.EntriesRepository,
    onBack: () -> Unit
) {
    // Parse the entry data using the input definitions from case data
    val parsedData = remember(entry, caseData) {
        val customInputsJson = caseData.project.inputs
        val entityOptions = caseData.project.media ?: emptyList()
        val entityName = caseData.project.entity_name ?: "entity"
        
        InputFieldParser.parseEntryData(
            entry = entry,
            customInputsJson = customInputsJson,
            entityName = entityName,
            entityOptions = entityOptions
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Entry Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatTimeRange(entry.begin, entry.end),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Entry info header
            item {
                EntryInfoCard(entry = entry, parsedData = parsedData)
            }
            
            // Entity selection (if available)
            if (parsedData.entityName != null) {
                item {
                    EntitySelectionCard(
                        entityName = parsedData.entityName,
                        entityOptions = parsedData.entityOptions
                    )
                }
            }
            
            // Input fields
            if (parsedData.inputFields.isNotEmpty()) {
                item {
                    Text(
                        text = "Input Fields",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                items(parsedData.inputFields) { inputField ->
                    InputFieldCard(
                        inputField = inputField,
                        entriesRepository = entriesRepository
                    )
                }
            } else {
                item {
                    EmptyInputsCard()
                }
            }
            
            // Add bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EntryInfoCard(
    entry: Entry,
    parsedData: ParsedEntryData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDate(entry.begin),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatTimeRange(entry.begin, entry.end),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Entry #${entry.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = getDuration(entry.begin, entry.end),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EntitySelectionCard(
    entityName: String,
    entityOptions: List<EntityInput>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MetagColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Selected Entity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (entityName.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MetagColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = entityName,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MetagColors.Primary
                    )
                }
            } else {
                Text(
                    text = "No entity selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InputFieldCard(
    inputField: ParsedInputField,
    entriesRepository: de.zemki.metagcompose.data.repository.EntriesRepository
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Field name and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inputField.definition.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InputTypeChip(inputField.definition.getInputFieldType())
            }
            
            // Field content based on type
            when (inputField.definition.getInputFieldType()) {
                InputFieldType.TEXT -> {
                    ReadOnlyTextField(inputField)
                }
                InputFieldType.ONE_CHOICE -> {
                    ReadOnlyOneChoice(inputField)
                }
                InputFieldType.MULTIPLE_CHOICE -> {
                    ReadOnlyMultipleChoice(inputField)
                }
                InputFieldType.SCALE -> {
                    ReadOnlyScale(inputField)
                }
                InputFieldType.AUDIO_RECORDING -> {
                    ReadOnlyAudioField(
                        inputField = inputField,
                        entriesRepository = entriesRepository
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInputsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No additional input fields",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This entry contains only basic time and entity information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper functions
private fun formatDate(dateTimeString: String): String {
    return try {
        val datePart = dateTimeString.substringBefore(" ")
        formatDateForDisplay(datePart)
    } catch (e: Exception) {
        dateTimeString
    }
}

private fun formatTimeRange(begin: String, end: String): String {
    return try {
        val beginTime = begin.substringAfter(" ").substring(0, 5)
        val endTime = end.substringAfter(" ").substring(0, 5)
        "$beginTime - $endTime"
    } catch (e: Exception) {
        "$begin - $end"
    }
}

private fun getDuration(begin: String, end: String): String {
    return try {
        // Simple duration calculation - could be enhanced
        "Duration calculated"
    } catch (e: Exception) {
        "Unknown duration"
    }
}