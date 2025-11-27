package de.zemki.metagcompose.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.zemki.metagcompose.GlobalConfig
import de.zemki.metagcompose.data.model.ApiResult
import de.zemki.metagcompose.data.model.AuthError
import de.zemki.metagcompose.data.model.LoginResponse
import de.zemki.metagcompose.data.repository.AuthRepository
import de.zemki.metagcompose.util.NetworkMonitor
import de.zemki.metagcompose.util.DeepLinkHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import de.zemki.metagcompose.util.AppLogger

expect open class PlatformViewModel()

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope
) : PlatformViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        observeNetworkState()
        checkForQrToken()
        loadSavedEmail()
    }

    private fun loadSavedEmail() {
        coroutineScope.launch {
            val savedEmail = authRepository.getSavedEmail()
            if (!savedEmail.isNullOrEmpty()) {
                uiState = uiState.copy(
                    email = savedEmail,
                    rememberEmail = true
                )
            }
        }
    }

    /**
     * Check if there's a pending QR token from a deep link and auto-login.
     * Can be called manually to re-check for tokens (e.g., when screen becomes visible).
     */
    fun checkForQrToken() {
        AppLogger.d("Checking for QR token...", tag = "LoginViewModel")
        val qrToken = DeepLinkHandler.consumeQrToken()
        if (qrToken != null) {
            AppLogger.d("Found QR token (${qrToken.take(20)}...), attempting auto-login", tag = "LoginViewModel")
            loginWithQr(qrToken)
        } else {
            AppLogger.d("No QR token found", tag = "LoginViewModel")
        }
    }
    
    fun updateEmail(email: String) {
        uiState = uiState.copy(email = email)
    }
    
    fun updatePassword(password: String) {
        uiState = uiState.copy(password = password)
    }
    
    fun updateBaseUrl(baseUrl: String) {
        uiState = uiState.copy(baseUrl = baseUrl)
    }

    fun toggleRememberEmail(remember: Boolean) {
        uiState = uiState.copy(rememberEmail = remember)
        if (!remember) {
            // Clear saved email if user unchecks
            coroutineScope.launch {
                authRepository.clearSavedEmail()
            }
        }
    }

    fun resetMartProjectState() {
        uiState = uiState.copy(isMartProject = false)
    }
    
    fun login() {
        if (!isValidInput()) return

        // Check network connectivity before attempting login
        coroutineScope.launch {
            val isConnected = networkMonitor.checkConnection()
            if (!isConnected) {
                uiState = uiState.copy(
                    errorMessage = "You are currently offline. Please check your internet connection and try again."
                )
                return@launch
            }

            // Convert email to lowercase before sending
            val email = uiState.email.trim().lowercase()

            authRepository.login(
                email = email,
                password = uiState.password,
                baseUrl = uiState.baseUrl
            ).collect { result ->
                handleLoginResult(result)
            }
        }
    }

    /**
     * Login using QR code token from deep link.
     * Automatically called when app opens via QR scan.
     */
    fun loginWithQr(token: String) {
        AppLogger.d("Starting QR login", tag = "LoginViewModel")

        coroutineScope.launch {
            val isConnected = networkMonitor.checkConnection()
            if (!isConnected) {
                uiState = uiState.copy(
                    errorMessage = "You are currently offline. Please check your internet connection and try again."
                )
                return@launch
            }

            authRepository.loginWithQr(
                token = token,
                baseUrl = uiState.baseUrl
            ).collect { result ->
                handleLoginResult(result)
            }
        }
    }

    /**
     * Common handler for login results (both email/password and QR).
     */
    private fun handleLoginResult(result: ApiResult<LoginResponse>) {
        when (result) {
            is ApiResult.Loading -> {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
            }
            is ApiResult.Success -> {
                // Save email if "remember me" is checked
                if (uiState.rememberEmail) {
                    coroutineScope.launch {
                        authRepository.saveEmail(uiState.email.trim().lowercase())
                    }
                }

                uiState = uiState.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    loginResponse = result.data,
                    errorMessage = null
                )
            }
            is ApiResult.Error -> {
                val isMartProject = result.exception is AuthError.MartProject
                AppLogger.d("Error result - isMartProject: $isMartProject, exception: ${result.exception}", tag = "LoginViewModel")
                uiState = uiState.copy(
                    isLoading = false,
                    isMartProject = isMartProject,
                    errorMessage = if (isMartProject) null else getErrorMessage(result.exception)
                )
                AppLogger.d("Updated UI state - isMartProject: ${uiState.isMartProject}", tag = "LoginViewModel")
            }
        }
    }
    
    private fun isValidInput(): Boolean {
        val isEmailValid = uiState.email.isNotBlank() && isValidEmail(uiState.email)
        val isPasswordValid = uiState.password.isNotBlank()
        val isBaseUrlValid = uiState.baseUrl.isNotBlank()
        
        if (!isEmailValid) {
            uiState = uiState.copy(errorMessage = "Please enter a valid email address")
            return false
        }
        
        if (!isPasswordValid) {
            uiState = uiState.copy(errorMessage = "Please enter your password")
            return false
        }
        
        if (!isBaseUrlValid) {
            uiState = uiState.copy(errorMessage = "Please enter the server URL")
            return false
        }
        
        return true
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
    
    private fun getErrorMessage(error: Exception): String {
        return when (error) {
            is AuthError.InvalidCredentials -> "Invalid email or password"
            is AuthError.NoCases -> "No cases assigned to your account"
            is AuthError.RateLimited -> "Too many login attempts. Please try again later"
            is AuthError.NetworkError -> "Network error. Please check your connection"
            is AuthError.MartProject -> "MART_PROJECT_ERROR" // Special marker for UI handling
            is AuthError.QrExpired -> "QR code has expired. Please request a new one"
            is AuthError.QrRevoked -> "QR code access denied or expired. Please request a new one"
            is AuthError.InvalidQrCode -> "Invalid QR code. Please scan a valid MeTag QR code"
            is AuthError.Unknown -> "Login failed: ${error.message}"
            else -> "An unexpected error occurred"
        }
    }
    
    private fun observeNetworkState() {
        networkMonitor.isConnected
            .onEach { isConnected ->
                uiState = uiState.copy(isOnline = isConnected)
            }
            .launchIn(coroutineScope)
    }
}

data class LoginUiState(
    val email: String = if (GlobalConfig.USE_DEV_VALUES) GlobalConfig.DevConfig.DEV_EMAIL else "",
    val password: String = if (GlobalConfig.USE_DEV_VALUES) GlobalConfig.DevConfig.DEV_PASSWORD else "",
    val baseUrl: String = GlobalConfig.SERVER_URL,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loginResponse: LoginResponse? = null,
    val errorMessage: String? = null,
    val isOnline: Boolean = true,
    val isMartProject: Boolean = false,
    val rememberEmail: Boolean = false
)