package de.zemki.metagcompose.util

import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()

class AndroidAudioPlayer : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null

    override suspend fun play(base64AudioData: String) = withContext(Dispatchers.IO) {
        try {
            // Stop any existing playback
            stop()
            
            // Extract base64 data (remove data:audio/wav;base64, prefix if present)
            val base64Data = if (base64AudioData.startsWith("data:")) {
                base64AudioData.substringAfter("base64,")
            } else {
                base64AudioData
            }
            
            // Decode base64 to byte array
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create temporary file
            tempFile = File.createTempFile("audio_", ".wav").apply {
                deleteOnExit()
            }
            
            // Write audio data to temporary file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes for media playback
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(tempFile!!.absolutePath)
                prepare()
                
                // Log audio info
                val duration = duration
                
                // Set volume to maximum
                setVolume(1.0f, 1.0f)
                
                start()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                }
                mediaPlayer = null
                
                // Clean up temporary file
                tempFile?.let { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
                tempFile = null
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun playWithProgress(
        base64AudioData: String, 
        onProgress: (currentMs: Int, totalMs: Int) -> Unit, 
        onComplete: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // Stop any existing playback
            stop()
            
            // Extract base64 data (remove data:audio/wav;base64, prefix if present)
            val base64Data = if (base64AudioData.startsWith("data:")) {
                base64AudioData.substringAfter("base64,")
            } else {
                base64AudioData
            }
            
            // Decode base64 to byte array
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create temporary file
            tempFile = File.createTempFile("audio_", ".wav").apply {
                deleteOnExit()
            }
            
            // Write audio data to temporary file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes for media playback
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(tempFile!!.absolutePath)
                prepare()
                
                // Log audio info
                val duration = duration
                
                // Set volume to maximum
                setVolume(1.0f, 1.0f)
                
                // Set completion listener
                setOnCompletionListener {
                    onComplete()
                }
                
                start()
            }
            
            // Start progress tracking
            val duration = mediaPlayer?.duration ?: 0
            while (isActive && mediaPlayer?.isPlaying == true) {
                val currentPosition = mediaPlayer?.currentPosition ?: 0
                withContext(Dispatchers.Main) {
                    onProgress(currentPosition, duration)
                }
                delay(100) // Update every 100ms
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    override fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    override fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            
            tempFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            tempFile = null
        } catch (e: Exception) {
        }
    }
}