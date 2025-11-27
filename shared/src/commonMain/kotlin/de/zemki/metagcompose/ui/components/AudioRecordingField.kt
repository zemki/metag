package de.zemki.metagcompose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*
import de.zemki.metagcompose.util.AudioRecorder
import de.zemki.metagcompose.util.AudioPlayer
import de.zemki.metagcompose.util.PermissionHandler
import de.zemki.metagcompose.util.RecordingState
import de.zemki.metagcompose.util.createAudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import de.zemki.metagcompose.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecordingField(
    onAudioRecorded: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialAudioData: String? = null,
    fieldName: String = "Audio Recording",
    isMandatory: Boolean = false
) {
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var recordingDuration by remember { mutableStateOf(0) }
    var hasRecording by remember { mutableStateOf(initialAudioData != null && initialAudioData.isNotBlank()) }
    var recordedAudioData by remember { mutableStateOf(initialAudioData) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }
    var audioDuration by remember { mutableStateOf(0) }
    var audioError by remember { mutableStateOf<String?>(null) }
    
    val audioRecorder = remember { AudioRecorder() }
    val audioPlayer = remember { createAudioPlayer() }
    val permissionHandler = remember { PermissionHandler() }
    val coroutineScope = rememberCoroutineScope()
    
    // React to changes in initialAudioData
    LaunchedEffect(initialAudioData) {
        if (initialAudioData != null && initialAudioData.isNotBlank()) {
            AppLogger.d("Received initial audio data, length: ${initialAudioData.length}", tag = "AudioRecordingField")
            hasRecording = true
            recordedAudioData = initialAudioData
        } else {
            AppLogger.d("No initial audio data", tag = "AudioRecordingField")
        }
    }
    
    fun startRecording() {
        // Stop any ongoing playback first
        if (isPlayingPreview) {
            coroutineScope.launch {
                audioPlayer.stop()
            }
            isPlayingPreview = false
            audioProgress = 0f
        }
        
        if (!permissionHandler.hasAudioPermission()) {
            permissionHandler.requestAudioPermission { granted ->
                if (granted) {
                    startRecording() // Retry after permission granted
                } else {
                    recordingState = RecordingState.ERROR
                }
            }
            return
        }
        
        if (audioRecorder.startRecording()) {
            recordingState = RecordingState.RECORDING
            recordingDuration = 0
            
            // Start duration counter
            coroutineScope.launch {
                while (recordingState == RecordingState.RECORDING) {
                    delay(1000)
                    recordingDuration++
                }
            }
        } else {
            recordingState = RecordingState.ERROR
        }
    }
    
    fun stopRecording() {
        val audioData = audioRecorder.stopRecording()
        
        if (audioData == "AUDIO_FILE_TOO_LARGE") {
            recordingState = RecordingState.ERROR
            // Show user-friendly error message about file size
            AppLogger.d("Recording too long - maximum duration is ~5 minutes", tag = "AudioRecordingField")
            hasRecording = false
            recordedAudioData = null
        } else if (audioData != null) {
            hasRecording = true
            recordedAudioData = audioData
            recordingState = RecordingState.IDLE // Go back to IDLE to show controls
            onAudioRecorded(audioData)
        } else {
            recordingState = RecordingState.ERROR
        }
    }
    
    fun deleteRecording() {
        // Stop any ongoing playback
        if (isPlayingPreview) {
            coroutineScope.launch {
                audioPlayer.stop()
            }
            isPlayingPreview = false
            audioProgress = 0f
        }
        
        hasRecording = false
        recordedAudioData = null
        recordingState = RecordingState.IDLE
        audioDuration = 0
        onAudioRecorded("") // Clear the audio data
    }
    
    fun playPreview() {
        if (recordedAudioData.isNullOrBlank()) return
        
        if (isPlayingPreview) {
            // Stop current playback
            coroutineScope.launch {
                audioPlayer.stop()
            }
            isPlayingPreview = false
            audioProgress = 0f
        } else {
            // Start playback
            coroutineScope.launch {
                try {
                    isPlayingPreview = true
                    audioProgress = 0f
                    
                    audioPlayer.playWithProgress(
                        recordedAudioData!!,
                        onProgress = { currentMs, totalMs ->
                            audioDuration = totalMs / 1000 // Convert to seconds
                            audioProgress = if (totalMs > 0) currentMs.toFloat() / totalMs else 0f
                        },
                        onComplete = {
                            isPlayingPreview = false
                            audioProgress = 0f
                        }
                    )
                    audioError = null // Clear any previous errors
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Expected when pausing - don't show error
                    AppLogger.d("Playback cancelled (pause)", tag = "AudioRecordingField")
                } catch (e: Exception) {
                    isPlayingPreview = false
                    audioProgress = 0f
                    audioError = "Audio playback failed: ${e.message ?: "Unknown error"}"
                    AppLogger.d("Error during audio playback: ${e.message}", tag = "AudioRecordingField")
                }
            }
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Field name with mandatory indicator (matching other input fields)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fieldName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isMandatory) {
                    Text(
                        text = "*",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            when (recordingState) {
                RecordingState.IDLE -> {
                    if (hasRecording) {
                        // Show modern audio player interface
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Audio player header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings, // Audio wave icon
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = fieldName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isPlayingPreview) stringResource(Res.string.audio_playing) else stringResource(Res.string.audio_ready),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Progress bar (actual progress)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                ) {
                                    // Background track
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(3.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ) {}
                                    
                                    // Progress fill
                                    if (isPlayingPreview && audioProgress > 0f) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(audioProgress),
                                            shape = RoundedCornerShape(3.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {}
                                    }
                                }
                                
                                // Main audio controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Delete button
                                    IconButton(
                                        onClick = { deleteRecording() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(24.dp))
                                    
                                    // Main play/pause button
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        onClick = { playPreview() }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingPreview) Icons.Default.Close else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlayingPreview) stringResource(Res.string.audio_stop) else stringResource(Res.string.audio_play),
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(24.dp))
                                    
                                    // Re-record button
                                    IconButton(
                                        onClick = { 
                                            hasRecording = false
                                            recordedAudioData = null
                                            recordingState = RecordingState.IDLE
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-record",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                // Time display (actual duration)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatDuration((audioProgress * audioDuration).toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatDuration(audioDuration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Show error message if any
                                audioError?.let { error ->
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Show modern record button
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFE53E3E), // Red recording color
                                    onClick = { startRecording() }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = Color.White
                                        ) {}
                                    }
                                }
                                
                                Text(
                                    text = "Tap to start recording",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                RecordingState.RECORDING -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Recording indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(16.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFF44336)
                                ) {}
                                
                                Text(
                                    text = "Recording ${recordingDuration}s",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF44336)
                                )
                            }
                            
                            // Animated recording waveform
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                repeat(15) { index ->
                                    val heights = listOf(16.dp, 32.dp, 24.dp, 48.dp, 20.dp, 36.dp, 28.dp, 44.dp, 18.dp, 40.dp, 26.dp, 52.dp, 22.dp, 38.dp, 30.dp)
                                    Surface(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(heights[index]),
                                        shape = RoundedCornerShape(1.5.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = if ((recordingDuration + index) % 3 == 0) 1f else 0.4f
                                        )
                                    ) {}
                                }
                            }
                            
                            // Stop button
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = Color(0xFFF44336),
                                onClick = { stopRecording() }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White
                                    ) {}
                                }
                            }
                        }
                    }
                }
                
                RecordingState.ERROR -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.audio_recording_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            TextButton(
                                onClick = { 
                                    recordingState = RecordingState.IDLE
                                }
                            ) {
                                Text("Try again")
                            }
                        }
                    }
                }
                
                RecordingState.STOPPED -> {
                    // This state is temporary, will transition to IDLE
                }
            }
            
            Text(
                text = if (recordingState == RecordingState.IDLE && !hasRecording) {
                    "Tap to record audio for this entry"
                } else {
                    "Audio will be uploaded with the entry"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Cleanup when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Stop any ongoing playback
            if (isPlayingPreview) {
                coroutineScope.launch {
                    try {
                        audioPlayer.stop()
                    } catch (e: Exception) {
                        AppLogger.d("Error stopping audio on dispose: ${e.message}", tag = "AudioRecordingField")
                    }
                }
            }
            
            // Stop any ongoing recording
            if (recordingState == RecordingState.RECORDING) {
                try {
                    audioRecorder.stopRecording()
                } catch (e: Exception) {
                    AppLogger.d("Error stopping recording on dispose: ${e.message}", tag = "AudioRecordingField")
                }
            }
            
            // Release audio player resources
            audioPlayer.release()
        }
    }
}

// Helper function to format duration in mm:ss format
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}