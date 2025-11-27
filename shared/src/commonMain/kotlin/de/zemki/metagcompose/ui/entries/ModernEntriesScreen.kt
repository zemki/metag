package de.zemki.metagcompose.ui.entries

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*
import de.zemki.metagcompose.data.model.CaseData
import de.zemki.metagcompose.data.model.Entry
import de.zemki.metagcompose.data.repository.AuthRepository
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.data.storage.TokenStorage
import de.zemki.metagcompose.ui.theme.MetagColors
import de.zemki.metagcompose.util.createNetworkMonitor
import de.zemki.metagcompose.util.formatDateForDisplay as utilFormatDateForDisplay
import de.zemki.metagcompose.util.isDatePast
import de.zemki.metagcompose.util.parseTimestamp
import kotlinx.coroutines.launch
import de.zemki.metagcompose.util.AppLogger
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

data class DayGroup(
    val date: String,
    val displayDate: String,
    val entries: List<Entry>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ModernEntriesScreen(
    caseData: CaseData,
    authRepository: AuthRepository,
    entriesRepository: EntriesRepository,
    tokenStorage: TokenStorage,
    onLogout: () -> Unit,
    onAddEntry: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onEditEntry: (Entry) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val networkMonitor = remember { createNetworkMonitor() }
    val entriesViewModel = remember {
        EntriesViewModel(
            entriesRepository,
            networkMonitor,
            coroutineScope,
            caseData.id,
            caseData.project.inputs
        )
    }
    val uiState by entriesViewModel.uiState

    // Message system state
    var showInitialMessage by remember { mutableStateOf(false) }
    
    // Check if we need to show initial message
    LaunchedEffect(caseData.id) {
        if (!tokenStorage.hasInitialMessageBeenShown(caseData.id)) {
            showInitialMessage = true
        }
    }

    // Calculate blocking status
    val lastDay = caseData.getLastDay()
    val isBlocked = isDatePast(lastDay)
    val lastDayFormatted = lastDay?.let { formatDateForDisplay(it) }

    // Delete confirmation state
    var entryToDelete by remember { mutableStateOf<Entry?>(null) }

    // Parse custom inputs once for total field count
    val totalNonAudioFields = remember {
        try {
            val customInputs = de.zemki.metagcompose.data.model.InputFieldParser.parseCustomInputs(caseData.project.inputs)
            customInputs.size
        } catch (e: Exception) {
            AppLogger.d("Failed to parse custom inputs: ${e.message}", tag = "ModernEntriesScreen")
            0
        }
    }

    // Group entries by day
    val groupedEntries = remember(uiState.entries) {
        uiState.entries.groupBy { entry ->
            // Extract date part from timestamp
            val timestamp = parseTimestamp(entry.begin) ?: 0L
            val instant = Instant.fromEpochSeconds(timestamp)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')}"
        }.map { (date, entries) ->
            DayGroup(
                date = date,
                displayDate = formatDateForDisplay(date),
                entries = entries.sortedByDescending { parseTimestamp(it.begin) ?: 0L }
            )
        }.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.entries_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.logout()
                                onLogout()
                            }
                        }
                    ) {
                        Text(
                            "Logout",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!isBlocked) {
                ExtendedFloatingActionButton(
                    onClick = onAddEntry,
                    icon = { 
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add Entry"
                        ) 
                    },
                    text = { Text("Add Entry") },
                    containerColor = MetagColors.Primary,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MetagColors.Primary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Loading entries...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.errorMessage != null -> {
                    ModernErrorMessage(
                        message = uiState.errorMessage!!,
                        onRetry = { entriesViewModel.loadEntries() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                groupedEntries.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Project info header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = caseData.project.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            lastDayFormatted?.let { dateStr ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBlocked) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isBlocked) MaterialTheme.colorScheme.error else MetagColors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.last_day_submit) + dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBlocked) MaterialTheme.colorScheme.error else MetagColors.Primary
                                    )
                                }
                            }
                        }
                        // Centered empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ModernEmptyState(
                                onAddEntry = onAddEntry,
                                isBlocked = isBlocked,
                                lastDayFormatted = lastDayFormatted
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Project info header
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = caseData.project.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                lastDayFormatted?.let { dateStr ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBlocked) Icons.Default.Warning else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isBlocked) MaterialTheme.colorScheme.error else MetagColors.Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.last_day_submit) + dateStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isBlocked) MaterialTheme.colorScheme.error else MetagColors.Primary
                                        )
                                    }
                                }
                            }
                        }
                        items(groupedEntries) { dayGroup ->
                            SimpleDaySection(
                                dayGroup = dayGroup,
                                onDeleteEntry = { entry ->
                                    entryToDelete = entry
                                },
                                onEntryClick = onEntryClick,
                                onEditEntry = onEditEntry,
                                isBlocked = isBlocked,
                                totalNonAudioFields = totalNonAudioFields
                            )
                        }
                        
                        // Add bottom padding for FAB
                        item {
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Initial message dialog
    if (showInitialMessage) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MetagColors.Primary
                    )
                    Text(
                        text = "Important Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                lastDayFormatted?.let { dateStr ->
                    Text(
                        text = if (isBlocked) {
                            "Your entries are now blocked as of $dateStr. You can consult your existing entries but cannot edit or delete them."
                        } else {
                            "Your entries will be blocked on $dateStr. After that date, you will only be able to consult them."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            tokenStorage.saveInitialMessageShown(caseData.id)
                            showInitialMessage = false
                        }
                    }
                ) {
                    Text(
                        text = "Got it",
                        color = MetagColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
    
    // Delete confirmation dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { 
                entryToDelete = null
                // Reset any swipe states - this would require more complex state management
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MetagColors.Error
                    )
                    Text(
                        text = "Delete Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Are you sure you want to delete this entry?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatTimeRange(entry.begin, entry.end),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.media_name != null) {
                                Text(
                                    text = entry.media_name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MetagColors.Primary
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetagColors.Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        entriesViewModel.deleteEntry(entry.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MetagColors.Error
                    )
                ) {
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        entryToDelete = null
                        // Reset any swipe states would happen here
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }
}

@Composable
fun SimpleDaySection(
    dayGroup: DayGroup,
    onDeleteEntry: (Entry) -> Unit,
    onEntryClick: (Entry) -> Unit,
    onEditEntry: (Entry) -> Unit,
    isBlocked: Boolean = false,
    totalNonAudioFields: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Day header - full width with header background
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header background image
            Image(
                painter = painterResource(Res.drawable.header),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentScale = ContentScale.Crop
            )
            
            // Overlay with semi-transparent background for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            
            // Content over the background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayGroup.displayDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(Res.string.entries_count).replace("%d", dayGroup.entries.size.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // Entries for this day - full width
        dayGroup.entries.forEach { entry ->
            SimpleSwipeToDeleteEntry(
                entry = entry,
                onDelete = { onDeleteEntry(entry) },
                onEntryClick = {
                    if (isBlocked) {
                        onEntryClick(entry) // View only
                    } else {
                        onEditEntry(entry) // Edit mode
                    }
                },
                isBlocked = isBlocked,
                totalNonAudioFields = totalNonAudioFields
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSwipeToDeleteEntry(
    entry: Entry,
    onDelete: () -> Unit,
    onEntryClick: () -> Unit,
    isBlocked: Boolean = false,
    totalNonAudioFields: Int
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart && !isBlocked) {
                // Don't immediately delete, show confirmation dialog instead
                onDelete()
                // Return false to prevent the item from being dismissed automatically
                false
            } else false
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isBlocked,
        backgroundContent = {
            // Only show background when swiping
            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MetagColors.Error),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                        Text(
                            "Delete",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    ) {
        SimpleEntryCard(
            entry = entry,
            onClick = onEntryClick,
            totalNonAudioFields = totalNonAudioFields
        )
    }
}

@Composable
fun SimpleEntryCard(
    entry: Entry,
    onClick: () -> Unit,
    totalNonAudioFields: Int
) {
    val entityName = entry.media_name
    val preview = getEntryPreview(entry)
    val filledFieldCount = getFilledFieldCount(entry)
    val hasAudio = hasAudioRecording(entry)
    val duration = calculateDuration(entry.begin, entry.end)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Time range with duration badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTime(entry.begin)} - ${formatTime(entry.end)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Duration badge
                duration?.let {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MetagColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MetagColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MetagColors.Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Entity name (if available)
            if (entityName != null && entityName.isNotBlank()) {
                Text(
                    text = entityName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MetagColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Entry preview (show field preview when available)
            preview?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Badges row (audio + field count)
            if (hasAudio || totalNonAudioFields > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio badge
                    if (hasAudio) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Field count badge
                    if (totalNonAudioFields > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$filledFieldCount/$totalNonAudioFields",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Fallback text when no meaningful content
            if (entityName.isNullOrBlank() && preview == null && filledFieldCount == 0 && !hasAudio) {
                Text(
                    text = "Empty entry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun ModernEmptyState(
    modifier: Modifier = Modifier,
    onAddEntry: () -> Unit,
    isBlocked: Boolean = false,
    lastDayFormatted: String? = null
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isBlocked) {
            // Show deadline passed message instead of add button
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Deadline passed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "Submission deadline passed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = "The deadline for submitting entries was ${lastDayFormatted ?: ""}. You can no longer add new entries for this case.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            // Show add entry button
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MetagColors.PrimaryContainer,
                modifier = Modifier
                    .size(72.dp)
                    .clickable { onAddEntry() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Entry",
                        tint = MetagColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "No entries yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Start tracking your activities by adding your first entry. To edit an existing entry, just tap on it. To delete an entry, swipe it to the left.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun ModernErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MetagColors.Error.copy(alpha = 0.1f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MetagColors.Error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MetagColors.Primary
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

// Helper functions for entry preview and metadata
private fun getEntryPreview(entry: Entry): String? {
    val parsedInputs = entry.getParsedInputs() ?: return null

    // Find first meaningful non-audio field
    for ((key, value) in parsedInputs.entries) {
        // Skip if it looks like an audio field
        if (key.contains("audio", ignoreCase = true) ||
            key.contains("recording", ignoreCase = true)) {
            continue
        }

        when (value) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                val content = value.content
                // Skip base64 audio data and empty values
                if (content.isNotBlank() &&
                    !(content.length > 50 && content.matches(Regex("^[A-Za-z0-9+/]+=*$")))) {
                    // Truncate long text
                    val preview = if (content.length > 40) "${content.take(37)}..." else content
                    return "$key: $preview"
                }
            }
            is kotlinx.serialization.json.JsonArray -> {
                if (value.isNotEmpty()) {
                    val items = value.mapNotNull {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                    if (items.isNotEmpty()) {
                        val preview = items.take(2).joinToString(", ")
                        val more = if (items.size > 2) "..." else ""
                        return "$key: $preview$more"
                    }
                }
            }
            else -> {}
        }
    }
    return null
}

private fun getFilledFieldCount(entry: Entry): Int {
    val parsedInputs = entry.getParsedInputs() ?: return 0
    return parsedInputs.entries.count { (key, value) ->
        when (value) {
            is kotlinx.serialization.json.JsonPrimitive -> value.content.isNotBlank()
            is kotlinx.serialization.json.JsonArray -> value.isNotEmpty()
            else -> false
        }
    }
}

private fun hasAudioRecording(entry: Entry): Boolean {
    val parsedInputs = entry.getParsedInputs() ?: return false
    return parsedInputs.entries.any { (key, value) ->
        (key.contains("audio", ignoreCase = true) ||
         key.contains("recording", ignoreCase = true)) &&
        value is kotlinx.serialization.json.JsonPrimitive &&
        value.content.isNotBlank()
    }
}

private fun calculateDuration(begin: String, end: String): String? {
    try {
        // Parse Unix timestamps
        val beginTimestamp = parseTimestamp(begin) ?: return null
        val endTimestamp = parseTimestamp(end) ?: return null

        val durationSeconds = endTimestamp - beginTimestamp
        if (durationSeconds <= 0) return null

        val durationMinutes = durationSeconds / 60
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    } catch (e: Exception) {
        return null
    }
}

private fun formatDateForDisplay(date: String): String {
    // Convert YYYY-MM-DD to more readable format
    val parts = date.split("-")
    if (parts.size == 3) {
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]

        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        val monthIndex = month.toIntOrNull()?.minus(1)
        val monthName = if (monthIndex != null && monthIndex in 0..11) {
            monthNames[monthIndex]
        } else month

        return "$day $monthName $year"
    }
    return date
}

@OptIn(ExperimentalTime::class)
private fun formatTime(dateTime: String): String {
    // Handle Unix timestamp
    val timestamp = parseTimestamp(dateTime) ?: return "Invalid"
    val instant = Instant.fromEpochSeconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun formatTimeRange(begin: String, end: String): String {
    return "${formatTime(begin)} - ${formatTime(end)}"
}