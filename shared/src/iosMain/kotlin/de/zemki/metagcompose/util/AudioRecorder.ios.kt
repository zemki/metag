package de.zemki.metagcompose.util

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class AudioRecorder {
    private var audioRecorder: AVAudioRecorder? = null
    private var recordingURL: NSURL? = null
    private var isRecordingFlag = false
    
    actual fun startRecording(): Boolean {
        return try {
            // Stop any existing recording
            stopRecording()
            
            // Configure audio session for recording
            val audioSession = AVAudioSession.sharedInstance()
            
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                
                // Set category to PlayAndRecord
                audioSession.setCategory(
                    category = AVAudioSessionCategoryPlayAndRecord,
                    mode = AVAudioSessionModeDefault,
                    options = AVAudioSessionCategoryOptionDefaultToSpeaker,
                    error = errorPtr.ptr
                )
                
                if (errorPtr.value != null) {
                }
                
                // Activate session
                audioSession.setActive(true, error = errorPtr.ptr)
                
                if (errorPtr.value != null) {
                }
            }
            
            // Create temporary file URL for recording
            val tempDir = NSTemporaryDirectory()
            val fileName = "recording_${NSDate().timeIntervalSince1970}.aac"
            recordingURL = NSURL.fileURLWithPath("$tempDir/$fileName")
            
            // Configure recording settings using NSDictionary (optimized for smaller size)
            val settings = mapOf<Any?, Any?>(
                "AVFormatIDKey" to NSNumber(1633772320), // kAudioFormatMPEG4AAC as number
                "AVSampleRateKey" to NSNumber(16000.0), // Reduce from 44100 to 16000 Hz
                "AVNumberOfChannelsKey" to NSNumber(1), // Mono
                "AVEncoderAudioQualityKey" to NSNumber(AVAudioQualityLow.toInt()), // Low quality for smaller files
                "AVEncoderBitRateKey" to NSNumber(32000) // 32 kbps bitrate
            )
            
            // Create and prepare recorder
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                audioRecorder = AVAudioRecorder(
                    uRL = recordingURL!!,
                    settings = settings,
                    error = errorPtr.ptr
                )
                
                if (errorPtr.value != null) {
                    return false
                }
            }
            
            audioRecorder?.prepareToRecord()
            
            // Start recording
            val started = audioRecorder?.record() ?: false
            isRecordingFlag = started
            
            if (started) {
            } else {
            }
            
            started
        } catch (e: Exception) {
            false
        }
    }
    
    actual fun stopRecording(): String? {
        return try {
            audioRecorder?.stop()
            isRecordingFlag = false
            
            val url = recordingURL
            if (url != null && NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")) {
                // Check file size before converting to base64
                val audioData = NSData.dataWithContentsOfFile(url.path ?: "")
                if (audioData != null) {
                    val fileSizeBytes = audioData.length.toLong()
                    
                    // Limit file size to 500KB (same as Android)
                    if (fileSizeBytes > 500 * 1024) {
                        // Clean up the temporary file
                        NSFileManager.defaultManager.removeItemAtPath(url.path ?: "", null)
                        return "AUDIO_FILE_TOO_LARGE"
                    }
                    
                    val base64String = audioData.base64EncodedStringWithOptions(0u)
                    
                    // Clean up the temporary file
                    NSFileManager.defaultManager.removeItemAtPath(url.path ?: "", null)
                    
                    base64String
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            audioRecorder = null
            recordingURL = null
        }
    }
    
    actual fun isRecording(): Boolean = isRecordingFlag && (audioRecorder?.recording ?: false)
    
    actual fun hasPermission(): Boolean {
        return when (AVAudioSession.sharedInstance().recordPermission) {
            AVAudioSessionRecordPermissionGranted -> true
            AVAudioSessionRecordPermissionDenied -> false
            AVAudioSessionRecordPermissionUndetermined -> false
            else -> false
        }
    }
    
    actual fun requestPermission() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
        }
    }
}