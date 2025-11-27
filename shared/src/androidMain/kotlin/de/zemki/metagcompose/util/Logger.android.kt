package de.zemki.metagcompose.util

actual fun isDebugBuild(): Boolean {
    // In Android, check system property
    return try {
        val debugProp = System.getProperty("kotlinx.coroutines.debug")
        debugProp != null || android.os.Build.TYPE.equals("userdebug", ignoreCase = true)
    } catch (e: Exception) {
        false // Default to false (release mode) if unable to determine
    }
}
