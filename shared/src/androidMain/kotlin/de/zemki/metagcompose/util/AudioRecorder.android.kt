package de.zemki.metagcompose.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

actual class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isCurrentlyRecording = false
    
    actual fun startRecording(): Boolean {
        return try {
            val context = getAppContext()
            if (!hasPermission()) {
                return false
            }
            
            // Create temporary file for recording
            outputFile = File.createTempFile("audio_recording", ".aac", context.cacheDir)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS) // Pure audio format (not video container)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                
                // Optimize for smaller file size
                setAudioSamplingRate(16000) // Reduce from default 44100 to 16000 Hz
                setAudioEncodingBitRate(32000) // Low bitrate for smaller files (32 kbps)
                setAudioChannels(1) // Mono instead of stereo
                
                prepare()
                start()
            }
            
            isCurrentlyRecording = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    
    actual fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isCurrentlyRecording = false
            
            // Convert file to base64 with size validation
            outputFile?.let { file ->
                if (file.exists()) {
                    val fileSizeBytes = file.length()
                    
                    // Limit file size to 500KB (which becomes ~650KB base64)
                    // This should safely fit in most database TEXT columns
                    if (fileSizeBytes > 500 * 1024) {
                        file.delete()
                        return "AUDIO_FILE_TOO_LARGE"
                    }
                    
                    val bytes = file.readBytes()
                    file.delete() // Clean up temporary file
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP) // NO_WRAP for smaller size
                    base64
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    actual fun isRecording(): Boolean = isCurrentlyRecording
    
    actual fun hasPermission(): Boolean {
        val context = getAppContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    actual fun requestPermission() {
        // This method is deprecated in favor of using PermissionHandler
    }
}

// Get application context - this should be set by the Android app
private lateinit var appContext: Context

fun setAppContext(context: Context) {
    appContext = context.applicationContext
}

fun getAppContext(): Context = appContext