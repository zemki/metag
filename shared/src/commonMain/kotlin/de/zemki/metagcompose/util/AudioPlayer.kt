package de.zemki.metagcompose.util

interface AudioPlayer {
    suspend fun play(base64AudioData: String)
    suspend fun playWithProgress(base64AudioData: String, onProgress: (currentMs: Int, totalMs: Int) -> Unit, onComplete: () -> Unit)
    suspend fun pause()
    suspend fun stop()
    fun release()
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun isPlaying(): Boolean
}

expect fun createAudioPlayer(): AudioPlayer