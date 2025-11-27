package de.zemki.metagcompose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import de.zemki.metagcompose.data.storage.TokenStorageImpl
import kotlinx.coroutines.delay
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
        // Platform layers are now enabled by default in Compose Multiplatform 1.8.0+
        // Use DoNothing to manually control keyboard behavior with WindowInsets.ime
        // This prevents automatic keyboard handling and lets us use imePadding() in Compose
        // Fixes iOS keyboard issues: https://github.com/JetBrains/compose-multiplatform/issues/4016
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    val tokenStorage = TokenStorageImpl()
    
    // Add a small delay to let the view hierarchy settle
    LaunchedEffect(Unit) {
        delay(100)
    }
    
    App(tokenStorage)
}