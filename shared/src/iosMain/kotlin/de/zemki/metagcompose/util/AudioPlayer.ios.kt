package de.zemki.metagcompose.util

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.experimental.ExperimentalNativeApi

actual fun createAudioPlayer(): AudioPlayer = IOSAudioPlayer()

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
class IOSAudioPlayer : AudioPlayer {
    private var audioPlayer: AVAudioPlayer? = null
    private var progressJob: Job? = null
    private var currentAudioData: String? = null
    
    override suspend fun play(base64AudioData: String) {
        withContext(Dispatchers.Main) {
            try {
                stop() // Stop any existing playback
                
                // Decode base64 to NSData
                val audioData = decodeBase64ToNSData(base64AudioData)
                    ?: throw Exception("Failed to decode base64 audio data")
                
                // Create AVAudioPlayer with the audio data
                val error = memScoped {
                    val errorPtr: ObjCObjectVar<NSError?> = alloc()
                    val player = AVAudioPlayer(data = audioData, error = errorPtr.ptr)
                    
                    errorPtr.value?.let { nsError ->
                        throw Exception("Failed to create audio player: ${nsError.localizedDescription}")
                    }
                    
                    player
                }
                
                audioPlayer = error
                audioPlayer?.prepareToPlay()
                
                // Configure audio session for playback
                val audioSession = AVAudioSession.sharedInstance()
                
                // Set category with options for better simulator compatibility
                val categoryError = memScoped {
                    val errorPtr: ObjCObjectVar<NSError?> = alloc()
                    audioSession.setCategory(
                        category = AVAudioSessionCategoryPlayAndRecord,
                        mode = AVAudioSessionModeDefault,
                        options = AVAudioSessionCategoryOptionDefaultToSpeaker,
                        error = errorPtr.ptr
                    )
                    errorPtr.value
                }
                
                if (categoryError != null) {
                    // Failed to set audio category
                }
                
                // Activate the session
                val activationError = memScoped {
                    val errorPtr: ObjCObjectVar<NSError?> = alloc()
                    audioSession.setActive(true, error = errorPtr.ptr)
                    errorPtr.value
                }
                
                if (activationError != null) {
                    // Failed to activate audio session
                }
                
                // Force audio output to speaker for simulator
                try {
                    audioSession.overrideOutputAudioPort(AVAudioSessionPortOverrideSpeaker, error = null)
                } catch (e: Exception) {
                    // Could not override output port
                }
                
                // Start playback
                audioPlayer?.play()
                currentAudioData = base64AudioData
            } catch (e: Exception) {
                // Error playing audio
                throw e
            }
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            try {
                audioPlayer?.pause()
            } catch (e: Exception) {
                // Error pausing audio
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            try {
                progressJob?.cancel()
                progressJob = null
                
                audioPlayer?.stop()
                audioPlayer = null
                currentAudioData = null
                
            } catch (e: Exception) {
                // Error stopping audio
            }
        }
    }

    override suspend fun playWithProgress(
        base64AudioData: String, 
        onProgress: (currentMs: Int, totalMs: Int) -> Unit, 
        onComplete: () -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                // First, play the audio
                play(base64AudioData)
                
                // Cancel any existing progress job
                progressJob?.cancel()
                
                // Start progress monitoring
                progressJob = launch {
                    while (isActive) {
                        val player = audioPlayer
                        if (player != null && player.playing) {
                            val currentTimeMs = (player.currentTime * 1000).toInt()
                            val durationMs = (player.duration * 1000).toInt()
                            
                            dispatch_async(dispatch_get_main_queue()) {
                                onProgress(currentTimeMs, durationMs)
                            }
                            
                            // Check if playback completed
                            if (currentTimeMs >= durationMs - 100) { // Allow 100ms tolerance
                                dispatch_async(dispatch_get_main_queue()) {
                                    onComplete()
                                }
                                break
                            }
                        } else {
                            // Player stopped or not playing
                            dispatch_async(dispatch_get_main_queue()) {
                                onComplete()
                            }
                            break
                        }
                        
                        delay(100) // Update every 100ms
                    }
                }
                
            } catch (e: Exception) {
                // Error playing audio with progress
                throw e
            }
        }
    }
    
    override fun getCurrentPosition(): Int {
        val player = audioPlayer ?: return 0
        return (player.currentTime * 1000).toInt()
    }
    
    override fun getDuration(): Int {
        val player = audioPlayer ?: return 0
        return (player.duration * 1000).toInt()
    }
    
    override fun isPlaying(): Boolean {
        return audioPlayer?.playing ?: false
    }

    override fun release() {
        try {
            // Cancel progress job first
            progressJob?.cancel()
            progressJob = null
            
            // Stop and release audio player synchronously
            audioPlayer?.stop()
            audioPlayer = null
            currentAudioData = null
            
        } catch (e: Exception) {
            // Error releasing resources
        }
    }
    
    @OptIn(BetaInteropApi::class)
    private fun decodeBase64ToNSData(base64String: String): NSData? {
        return try {
            // Remove any whitespace and newlines
            val cleanedBase64 = base64String.replace("\\s".toRegex(), "")
            
            // Decode base64 to NSData
            NSData.create(base64Encoding = cleanedBase64)
        } catch (e: Exception) {
            // Error decoding base64
            null
        }
    }
}