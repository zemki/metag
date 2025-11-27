package de.zemki.metagcompose.ui.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import de.zemki.metagcompose.data.model.ApiResult
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.ui.theme.MetagColors
import de.zemki.metagcompose.util.AudioPlayer
import de.zemki.metagcompose.util.createAudioPlayer
import kotlinx.coroutines.launch

enum class AudioPlayerState {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    ERROR
}

@Composable
fun AudioPlayerComponent(
    fileId: Int,
    entriesRepository: EntriesRepository,
    modifier: Modifier = Modifier
) {
    var playerState by remember { mutableStateOf(AudioPlayerState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioData by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val audioPlayer = remember { createAudioPlayer() }
    
    // Load audio data when component is first displayed
    LaunchedEffect(fileId) {
        playerState = AudioPlayerState.LOADING
        entriesRepository.downloadAudioFile(fileId).collect { result ->
            when (result) {
                is ApiResult.Success -> {
                    audioData = result.data
                    playerState = AudioPlayerState.READY
                    errorMessage = null
                }
                is ApiResult.Error -> {
                    playerState = AudioPlayerState.ERROR
                    errorMessage = "Failed to load audio"
                }
                is ApiResult.Loading -> {
                    playerState = AudioPlayerState.LOADING
                }
            }
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = Color(0xFF9C27B0).copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play/Pause Button
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when (playerState) {
                    AudioPlayerState.PLAYING -> Color(0xFF9C27B0)
                    AudioPlayerState.READY, AudioPlayerState.PAUSED -> Color(0xFF9C27B0)
                    AudioPlayerState.LOADING -> MaterialTheme.colorScheme.surfaceVariant
                    AudioPlayerState.ERROR -> MaterialTheme.colorScheme.error
                    AudioPlayerState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                },
                onClick = {
                    when (playerState) {
                        AudioPlayerState.READY, AudioPlayerState.PAUSED -> {
                            audioData?.let { data ->
                                coroutineScope.launch {
                                    try {
                                        audioPlayer.play(data)
                                        playerState = AudioPlayerState.PLAYING
                                    } catch (e: Exception) {
                                        playerState = AudioPlayerState.ERROR
                                        errorMessage = "Playback failed" // TODO: Extract to stringResource when context allows
                                    }
                                }
                            }
                        }
                        AudioPlayerState.PLAYING -> {
                            coroutineScope.launch {
                                audioPlayer.pause()
                                playerState = AudioPlayerState.PAUSED
                            }
                        }
                        AudioPlayerState.ERROR -> {
                            // Retry loading
                            coroutineScope.launch {
                                playerState = AudioPlayerState.LOADING
                                entriesRepository.downloadAudioFile(fileId).collect { result ->
                                    when (result) {
                                        is ApiResult.Success -> {
                                            audioData = result.data
                                            playerState = AudioPlayerState.READY
                                            errorMessage = null
                                        }
                                        is ApiResult.Error -> {
                                            playerState = AudioPlayerState.ERROR
                                            errorMessage = "Failed to load audio"
                                        }
                                        is ApiResult.Loading -> {
                                            playerState = AudioPlayerState.LOADING
                                        }
                                    }
                                }
                            }
                        }
                        else -> {} // Do nothing for other states
                    }
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (playerState) {
                        AudioPlayerState.LOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp
                            )
                        }
                        AudioPlayerState.PLAYING -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.audio_stop),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AudioPlayerState.READY, AudioPlayerState.PAUSED -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(Res.string.audio_play),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AudioPlayerState.ERROR -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AudioPlayerState.IDLE -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(Res.string.audio_play),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Audio Info
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = when (playerState) {
                        AudioPlayerState.LOADING -> "Loading Audio..."
                        AudioPlayerState.PLAYING -> stringResource(Res.string.audio_playing_state)
                        AudioPlayerState.PAUSED -> "Audio Paused"
                        AudioPlayerState.READY -> "Audio Ready"
                        AudioPlayerState.ERROR -> "Audio Error"
                        AudioPlayerState.IDLE -> "Audio Recording"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (playerState == AudioPlayerState.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF9C27B0)
                    }
                )
                Text(
                    text = when (playerState) {
                        AudioPlayerState.LOADING -> "Downloading audio file..."
                        AudioPlayerState.PLAYING -> "Tap to pause"
                        AudioPlayerState.PAUSED -> "Tap to resume"
                        AudioPlayerState.READY -> "Tap to play"
                        AudioPlayerState.ERROR -> errorMessage ?: "Tap to retry"
                        AudioPlayerState.IDLE -> "Loading..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // Cleanup when component is disposed
    DisposableEffect(audioPlayer) {
        onDispose {
            audioPlayer.release()
        }
    }
}