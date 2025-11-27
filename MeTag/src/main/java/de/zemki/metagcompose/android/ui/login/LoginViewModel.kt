package de.zemki.metagcompose.android.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.zemki.metagcompose.data.model.ApiResult
import de.zemki.metagcompose.data.model.AuthError
import de.zemki.metagcompose.data.model.LoginResponse
import de.zemki.metagcompose.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    var uiState by mutableStateOf(LoginUiState())
        private set
    
    fun updateEmail(email: String) {
        uiState = uiState.copy(email = email)
    }
    
    fun updatePassword(password: String) {
        uiState = uiState.copy(password = password)
    }
    
    fun updateBaseUrl(baseUrl: String) {
        uiState = uiState.copy(baseUrl = baseUrl)
    }
    
    fun login() {
        if (!isValidInput()) return
        
        viewModelScope.launch {
            authRepository.login(
                email = uiState.email,
                password = uiState.password,
                baseUrl = uiState.baseUrl
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        uiState = uiState.copy(isLoading = true, errorMessage = null)
                    }
                    is ApiResult.Success -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null
                        )
                    }
                    is ApiResult.Error -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = getErrorMessage(result.exception)
                        )
                    }
                }
            }
        }
    }
    
    private fun isValidInput(): Boolean {
        val isEmailValid = uiState.email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(uiState.email).matches()
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
    
    private fun getErrorMessage(error: Exception): String {
        return when (error) {
            is AuthError.InvalidCredentials -> "Invalid email or password"
            is AuthError.NoCases -> "No cases assigned to your account"
            is AuthError.RateLimited -> "Too many login attempts. Please try again later"
            is AuthError.NetworkError -> "Network error. Please check your connection"
            is AuthError.Unknown -> "Login failed: ${error.message}"
            else -> "An unexpected error occurred"
        }
    }
}

data class LoginUiState(
    val email: String = if (de.zemki.metagcompose.GlobalConfig.USE_DEV_VALUES) de.zemki.metagcompose.GlobalConfig.DevConfig.DEV_EMAIL else "",
    val password: String = if (de.zemki.metagcompose.GlobalConfig.USE_DEV_VALUES) de.zemki.metagcompose.GlobalConfig.DevConfig.DEV_PASSWORD else "",
    val baseUrl: String = if (de.zemki.metagcompose.GlobalConfig.USE_DEV_VALUES) de.zemki.metagcompose.GlobalConfig.DevConfig.DEV_SERVER_URL else "https://",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)