package de.zemki.metagcompose.ui.entries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*
import de.zemki.metagcompose.data.model.CaseData
import de.zemki.metagcompose.data.model.Entry
import de.zemki.metagcompose.data.model.EntryWithSyncStatus
import de.zemki.metagcompose.data.model.SyncStatus
import de.zemki.metagcompose.data.repository.AuthRepository
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.util.NetworkMonitor
import de.zemki.metagcompose.util.createNetworkMonitor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesScreen(
    caseData: CaseData,
    authRepository: AuthRepository,
    entriesRepository: EntriesRepository,
    onLogout: () -> Unit,
    onAddEntry: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val networkMonitor = remember { createNetworkMonitor() }
    val entriesViewModel = remember {
        EntriesViewModel(entriesRepository, networkMonitor, coroutineScope, caseData.id, caseData.project.inputs)
    }
    val uiState by entriesViewModel.uiState
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.entries_title))
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!uiState.isOnline) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "OFFLINE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = caseData.project.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                            onLogout()
                        }
                    }) {
                        Text("Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorMessage(
                        message = uiState.errorMessage!!,
                        onRetry = { entriesViewModel.loadEntries() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.entriesWithStatus.isEmpty() -> {
                    EmptyEntriesMessage(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entriesWithStatus) { entryWithStatus ->
                            EntryWithStatusCard(entryWithStatus = entryWithStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntryCard(entry: Entry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDateTime(entry.begin),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "to ${formatTime(entry.end)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (entry.media_name != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Entity: ${entry.media_name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            val parsedInputs = entry.getParsedInputs()
            if (!parsedInputs.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                parsedInputs.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$key:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (value) {
                                is kotlinx.serialization.json.JsonPrimitive -> value.content
                                else -> value.toString()
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun EntryWithStatusCard(entryWithStatus: EntryWithSyncStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = when (entryWithStatus.syncStatus) {
            SyncStatus.PENDING -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            SyncStatus.FAILED -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
            else -> CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDateTime(entryWithStatus.begin),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "to ${formatTime(entryWithStatus.end)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Sync status indicator
                when (entryWithStatus.syncStatus) {
                    SyncStatus.PENDING -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "PENDING",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    SyncStatus.FAILED -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "FAILED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                    SyncStatus.SYNCED -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "SYNCED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Show entity name if available
            val entityName = entryWithStatus.entry?.media_name
            if (entityName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Entity: $entityName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Show inputs if available
            val parsedInputs = entryWithStatus.entry?.getParsedInputs() 
                ?: entryWithStatus.pendingEntry?.inputs?.let { 
                    try { 
                        kotlinx.serialization.json.Json.parseToJsonElement(it.toString())
                            .let { element -> 
                                if (element is kotlinx.serialization.json.JsonObject) element else null 
                            }
                    } catch (e: Exception) { null }
                }
                
            if (!parsedInputs.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                parsedInputs.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$key:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (value) {
                                is kotlinx.serialization.json.JsonPrimitive -> value.content
                                else -> value.toString()
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            // Show error message for failed entries
            if (entryWithStatus.syncStatus == SyncStatus.FAILED) {
                val errorMessage = entryWithStatus.pendingEntry?.errorMessage
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyEntriesMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No entries yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add your first entry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun formatDateTime(dateTime: String): String {
    // Simple formatting - can be enhanced with proper date parsing
    return dateTime.substringBefore(" ")
}

private fun formatTime(dateTime: String): String {
    // Extract time portion
    val timePart = dateTime.substringAfter(" ").substringBefore(".")
    return timePart.substring(0, 5) // HH:mm
}