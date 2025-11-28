package de.zemki.metagcompose.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.zemki.metagcompose.App
import de.zemki.metagcompose.data.storage.TokenStorageImpl
import de.zemki.metagcompose.util.initNetworkMonitor
import de.zemki.metagcompose.util.setAppContext
import de.zemki.metagcompose.util.setPermissionLauncher
import de.zemki.metagcompose.util.handlePermissionResult
import de.zemki.metagcompose.util.DeepLinkHandler

class MainActivity : ComponentActivity() {
    
    // Register permission launcher early in activity lifecycle
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initNetworkMonitor(this)
        setAppContext(this)
        setPermissionLauncher(permissionLauncher)

        // Handle deep link if app was opened via QR code
        handleDeepLink(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val tokenStorage = TokenStorageImpl(this@MainActivity)
                    App(tokenStorage)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        handleDeepLink(intent)
    }

    /**
     * Extract QR token from deep link and store it for LoginViewModel.
     * Handles custom scheme: metagapp://login?token=xxx
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "metagapp" && data.host == "login") {
            val token = data.getQueryParameter("token")
            if (token != null) {
                DeepLinkHandler.setQrToken(token)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        setPermissionLauncher(null)
    }
}

