package de.zemki.metagcompose.util

expect class AudioRecorder() {
    fun startRecording(): Boolean
    fun stopRecording(): String? // Returns base64 audio data
    fun isRecording(): Boolean
    fun hasPermission(): Boolean
    fun requestPermission()
}

enum class RecordingState {
    IDLE, RECORDING, STOPPED, ERROR
}