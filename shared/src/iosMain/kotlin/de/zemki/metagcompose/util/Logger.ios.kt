package de.zemki.metagcompose.util

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean {
    // Check for DEBUG environment variable or use debug mode detection
    return Platform.isDebugBinary
}
