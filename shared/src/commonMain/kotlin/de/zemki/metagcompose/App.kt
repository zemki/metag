package de.zemki.metagcompose

import de.zemki.metagcompose.ui.theme.MetagTheme
import androidx.compose.runtime.*
import de.zemki.metagcompose.data.model.CaseData
import de.zemki.metagcompose.data.model.Entry
import de.zemki.metagcompose.data.offline.OfflineEntryManager
import de.zemki.metagcompose.data.repository.AuthRepository
import de.zemki.metagcompose.data.repository.EntriesRepository
import de.zemki.metagcompose.data.storage.TokenStorage
import de.zemki.metagcompose.platform.createNotificationManager
import de.zemki.metagcompose.ui.entries.ModernEntriesScreen
import de.zemki.metagcompose.ui.entries.AddEntryScreen
import de.zemki.metagcompose.ui.consultation.ConsultationScreen
import de.zemki.metagcompose.ui.login.LoginScreen
import de.zemki.metagcompose.ui.login.LoginViewModel
import de.zemki.metagcompose.ui.main.MainScreen
import de.zemki.metagcompose.ui.mart.MartRedirectScreen
import de.zemki.metagcompose.ui.error.KeychainErrorScreen
import de.zemki.metagcompose.util.createNetworkMonitor
import de.zemki.metagcompose.util.AppLogger
import de.zemki.metagcompose.util.PermissionHandler

@Composable
fun App(tokenStorage: TokenStorage) {
    MetagTheme {
        val coroutineScope = rememberCoroutineScope()
        val networkMonitor = remember { createNetworkMonitor() }
        val offlineManager = remember { OfflineEntryManager(tokenStorage) }
        val notificationManager = createNotificationManager()
        val authRepository = remember(notificationManager) { AuthRepository(tokenStorage, notificationManager) }
        val entriesRepository = EntriesRepository(tokenStorage, networkMonitor, offlineManager)
        var isLoggedIn by remember { mutableStateOf(false) }
        var caseData by remember { mutableStateOf<CaseData?>(null) }
        var selectedEntry by remember { mutableStateOf<Entry?>(null) }
        var showAddEntry by remember { mutableStateOf(false) }
        var editEntry by remember { mutableStateOf<Entry?>(null) }
        var showMartRedirect by remember { mutableStateOf(false) }
        var keychainError by remember { mutableStateOf<Exception?>(null) }
        var retryTrigger by remember { mutableStateOf(0) }

        // Initial login check
        LaunchedEffect(Unit) {
            try {
                isLoggedIn = authRepository.isLoggedIn()
                if (isLoggedIn) {
                    caseData = authRepository.getStoredCaseData()
                }
            } catch (e: Exception) {
                // Catch Keychain access errors
                if (e.message?.contains("Keychain") == true ||
                    e.message?.contains("keychain") == true ||
                    e.message?.contains("locked") == true) {
                    AppLogger.e("Keychain access error: ${e.message}", tag = "App")
                    keychainError = e
                } else {
                    throw e
                }
            }
        }

        // Retry logic for Keychain errors
        LaunchedEffect(retryTrigger) {
            if (retryTrigger > 0) {
                try {
                    isLoggedIn = authRepository.isLoggedIn()
                    if (isLoggedIn) {
                        caseData = authRepository.getStoredCaseData()
                    }
                    // Success - clear error
                    keychainError = null
                } catch (e: Exception) {
                    if (e.message?.contains("Keychain") == true ||
                        e.message?.contains("keychain") == true ||
                        e.message?.contains("locked") == true) {
                        AppLogger.e("Keychain retry failed: ${e.message}", tag = "App")
                        keychainError = e
                    } else {
                        throw e
                    }
                }
            }
        }

        // Show Keychain error screen if access is denied
        if (keychainError != null) {
            KeychainErrorScreen(
                onRetry = {
                    // Trigger retry via state change
                    retryTrigger++
                }
            )
            return@MetagTheme
        }

        // Request notification permissions after successful login
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                val permissionHandler = PermissionHandler()
                if (!permissionHandler.hasNotificationPermission()) {
                    AppLogger.d("Requesting notification permission after login", tag = "App")
                    permissionHandler.requestNotificationPermission { granted ->
                        if (granted) {
                            AppLogger.d("Notification permission granted", tag = "App")
                        } else {
                            AppLogger.w("Notification permission denied", tag = "App")
                        }
                    }
                } else {
                    AppLogger.d("Notification permission already granted", tag = "App")
                }
            }
        }
        
        AppLogger.d("Navigation check - isLoggedIn: $isLoggedIn, showMartRedirect: $showMartRedirect, showAddEntry: $showAddEntry", tag = "App")
        
        when {
            isLoggedIn && caseData != null && showAddEntry -> {
                AddEntryScreen(
                    caseData = caseData!!,
                    entriesRepository = entriesRepository,
                    onBack = {
                        showAddEntry = false
                    },
                    onEntryCreated = {
                        showAddEntry = false
                        // Refresh entries by staying on the entries screen
                    }
                )
            }
            isLoggedIn && caseData != null && editEntry != null -> {
                AddEntryScreen(
                    caseData = caseData!!,
                    entriesRepository = entriesRepository,
                    editingEntry = editEntry,
                    onBack = {
                        editEntry = null
                    },
                    onEntryCreated = {
                        editEntry = null
                        // Refresh entries by staying on the entries screen
                    }
                )
            }
            isLoggedIn && caseData != null && selectedEntry != null -> {
                ConsultationScreen(
                    entry = selectedEntry!!,
                    caseData = caseData!!,
                    entriesRepository = entriesRepository,
                    onBack = {
                        selectedEntry = null
                    }
                )
            }
            isLoggedIn && caseData != null -> {
                ModernEntriesScreen(
                    caseData = caseData!!,
                    authRepository = authRepository,
                    entriesRepository = entriesRepository,
                    tokenStorage = tokenStorage,
                    onLogout = {
                        isLoggedIn = false
                        caseData = null
                        selectedEntry = null
                        showAddEntry = false
                        editEntry = null
                        showMartRedirect = false
                    },
                    onAddEntry = {
                        showAddEntry = true
                    },
                    onEntryClick = { entry ->
                        selectedEntry = entry
                    },
                    onEditEntry = { entry ->
                        editEntry = entry
                    }
                )
            }
            isLoggedIn && caseData == null -> {
                // This shouldn't happen in normal flow, but show main screen as fallback
                MainScreen(
                    authRepository = authRepository,
                    onLogout = {
                        isLoggedIn = false
                        caseData = null
                        selectedEntry = null
                        showAddEntry = false
                        editEntry = null
                        showMartRedirect = false
                    }
                )
            }
            showMartRedirect -> {
                MartRedirectScreen(
                    onBackToLogin = {
                        showMartRedirect = false
                    }
                )
            }
            else -> {
                val loginViewModel = remember { LoginViewModel(authRepository, networkMonitor, coroutineScope) }
                
                // Monitor for MART project detection
                LaunchedEffect(loginViewModel.uiState.isMartProject) {
                    AppLogger.d("LoginViewModel MART state changed: ${loginViewModel.uiState.isMartProject}", tag = "App")
                    if (loginViewModel.uiState.isMartProject) {
                        AppLogger.d("Setting showMartRedirect to true", tag = "App")
                        showMartRedirect = true
                    }
                }
                
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { loginResponse ->
                        isLoggedIn = true
                        caseData = loginResponse.case
                    }
                )
            }
        }
    }
}