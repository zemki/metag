package de.zemki.metagcompose.data.repository

import de.zemki.metagcompose.data.model.*
import de.zemki.metagcompose.data.storage.TokenStorage
import de.zemki.metagcompose.network.createHttpClient
import de.zemki.metagcompose.platform.NotificationManager
import de.zemki.metagcompose.util.AppLogger
import de.zemki.metagcompose.util.MartUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val notificationManager: NotificationManager
) {
    private val httpClient: HttpClient by lazy { createHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    
    fun login(email: String, password: String, baseUrl: String): Flow<ApiResult<LoginResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // Initialize notification manager and get device token
            val deviceToken = try {
                notificationManager.initialize()
                notificationManager.getDeviceToken()
            } catch (e: Exception) {
                AppLogger.d("Failed to get device token: ${e.message}", tag = "AuthRepository")
                null
            }

            AppLogger.d("Device token for login: ${deviceToken?.take(20)}...", tag = "AuthRepository")

            val currentTimestamp = de.zemki.metagcompose.util.getCurrentTimeSeconds()
            val loginRequest = LoginRequest(
                email = email,
                password = password,
                deviceID = deviceToken ?: generateDeviceId(),
                datetime = currentTimestamp,
                duration = currentTimestamp
            )

            // Clean baseUrl and auto-upgrade HTTP to HTTPS (defensive against nginx redirects)
            val cleanBaseUrl = baseUrl.trimEnd('/')
                .replace(Regex("^http://"), "https://")
            val fullUrl = "$cleanBaseUrl/api/login"
            AppLogger.d("Attempting login to: $fullUrl", tag = "AuthRepository")
            AppLogger.d("Request body: $loginRequest", tag = "AuthRepository")
            AppLogger.d("Datetime value: ${loginRequest.datetime}", tag = "AuthRepository")
            AppLogger.d("DeviceID value: ${loginRequest.deviceID}", tag = "AuthRepository")


            val jsonBody = json.encodeToString(LoginRequest.serializer(), loginRequest)
            AppLogger.d("JSON body being sent: $jsonBody", tag = "AuthRepository")

            val response: HttpResponse = httpClient.post(fullUrl) {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)  // Keep using the object, not the string
                headers {
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                }
            }
            
            AppLogger.d("Response status: ${response.status}", tag = "AuthRepository")
            val responseBody = response.bodyAsText()
            AppLogger.d("Response body: $responseBody", tag = "AuthRepository")
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        val loginResponse = json.decodeFromString<LoginResponse>(responseBody)
                        
                        // Check if this is a MART project
                        if (MartUtils.isMartProject(loginResponse.case.project)) {
                            AppLogger.d("MART project detected, blocking login", tag = "AuthRepository")
                            emit(ApiResult.Error(AuthError.MartProject))
                            return@flow
                        }
                        
                        tokenStorage.saveToken(loginResponse.token)
                        tokenStorage.saveBaseUrl(baseUrl)
                        // Store API version (default to v1 if not present)
                        val apiVersion = loginResponse.api_version ?: "v1"
                        tokenStorage.saveApiVersion(apiVersion)
                        // Store file token if present
                        loginResponse.file_token?.let { tokenStorage.saveFileToken(it) }
                        // Store case data as JSON
                        val caseDataJson = json.encodeToString(CaseData.serializer(), loginResponse.case)
                        tokenStorage.saveCaseData(caseDataJson)
                        emit(ApiResult.Success(loginResponse))
                    } catch (e: Exception) {
                        AppLogger.d("Failed to parse success response: ${e.message}", tag = "AuthRepository")
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse response: $responseBody")))
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    emit(ApiResult.Error(AuthError.InvalidCredentials))
                }
                HttpStatusCode.TooManyRequests -> {
                    emit(ApiResult.Error(AuthError.RateLimited))
                }
                HttpStatusCode(499, "No Cases") -> {
                    emit(ApiResult.Error(AuthError.NoCases))
                }
                HttpStatusCode.NotFound -> {
                    emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
                }
                HttpStatusCode.InternalServerError, 
                HttpStatusCode.BadGateway, 
                HttpStatusCode.ServiceUnavailable, 
                HttpStatusCode.GatewayTimeout -> {
                    emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
                }
                else -> {
                    AppLogger.d("Error response: $responseBody", tag = "AuthRepository")
                    emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("Login error - ${e::class.simpleName}: ${e.message}", tag = "AuthRepository")
            e.printStackTrace()
            emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
        }
    }
    
    suspend fun logout() {
        tokenStorage.clearToken()
        tokenStorage.clearCaseData()
        tokenStorage.clearFileToken()
    }
    
    suspend fun isLoggedIn(): Boolean {
        return tokenStorage.getToken() != null
    }
    
    suspend fun getStoredBaseUrl(): String? {
        return tokenStorage.getBaseUrl()
    }
    
    suspend fun getStoredCaseData(): CaseData? {
        val caseDataJson = tokenStorage.getCaseData()
        return if (caseDataJson != null) {
            try {
                json.decodeFromString<CaseData>(caseDataJson)
            } catch (e: Exception) {
                AppLogger.d("Failed to parse stored case data: ${e.message}", tag = "AuthRepository")
                null
            }
        } else null
    }
    
    /**
     * Login using QR code token from deep link.
     * The token is encrypted server-side and sent as-is to the backend.
     * Response format is identical to regular login.
     *
     * @param token The encrypted QR token from the deep link
     * @param baseUrl The API base URL
     * @return Flow with login result
     */
    fun loginWithQr(token: String, baseUrl: String): Flow<ApiResult<LoginResponse>> = flow {
        AppLogger.d("loginWithQr() called", tag = "AuthRepository")
        AppLogger.d("BaseURL: $baseUrl", tag = "AuthRepository")
        AppLogger.d("Token length: ${token.length}, first 20 chars: ${token.take(20)}...", tag = "AuthRepository")

        emit(ApiResult.Loading)

        try {
            // Initialize notification manager and get device token for push notifications
            val deviceToken = try {
                notificationManager.initialize()
                notificationManager.getDeviceToken()
            } catch (e: Exception) {
                AppLogger.d("Failed to get device token for QR login: ${e.message}", tag = "AuthRepository")
                null
            }

            AppLogger.d("Device token for QR login: ${deviceToken?.take(20)}...", tag = "AuthRepository")

            // Clean baseUrl and auto-upgrade HTTP to HTTPS (defensive against nginx redirects)
            val cleanBaseUrl = baseUrl.trimEnd('/')
                .replace(Regex("^http://"), "https://")
            val fullUrl = "$cleanBaseUrl/api/qr-login"
            AppLogger.d("Attempting QR login to: $fullUrl (auto-upgraded to HTTPS)", tag = "AuthRepository")

            val requestBody = mutableMapOf<String, String>("token" to token)
            if (deviceToken != null) {
                requestBody["deviceID"] = deviceToken
            } else {
                requestBody["deviceID"] = generateDeviceId()
            }
            AppLogger.d("QR login deviceID: ${requestBody["deviceID"]?.take(20)}...", tag = "AuthRepository")

            val response: HttpResponse = httpClient.post(fullUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
                headers {
                    append("Accept", "application/json")
                    append("ngrok-skip-browser-warning", "true")
                }
            }

            AppLogger.d("QR login response status: ${response.status}", tag = "AuthRepository")
            val responseBody = response.bodyAsText()
            AppLogger.d("QR login response body: $responseBody", tag = "AuthRepository")

            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        val loginResponse = json.decodeFromString<LoginResponse>(responseBody)

                        // Check if this is a MART project
                        if (MartUtils.isMartProject(loginResponse.case.project)) {
                            AppLogger.d("MART project detected, blocking QR login", tag = "AuthRepository")
                            emit(ApiResult.Error(AuthError.MartProject))
                            return@flow
                        }

                        tokenStorage.saveToken(loginResponse.token)
                        tokenStorage.saveBaseUrl(baseUrl)
                        // Store API version (default to v1 if not present)
                        val apiVersion = loginResponse.api_version ?: "v1"
                        tokenStorage.saveApiVersion(apiVersion)
                        // Store file token if present
                        loginResponse.file_token?.let { tokenStorage.saveFileToken(it) }
                        // Store case data as JSON
                        val caseDataJson = json.encodeToString(CaseData.serializer(), loginResponse.case)
                        tokenStorage.saveCaseData(caseDataJson)
                        emit(ApiResult.Success(loginResponse))
                    } catch (e: Exception) {
                        AppLogger.d("Failed to parse QR login success response: ${e.message}", tag = "AuthRepository")
                        emit(ApiResult.Error(AuthError.Unknown("Failed to parse response: $responseBody")))
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    // Try to extract error message from response
                    try {
                        val errorResponse = json.decodeFromString<Map<String, String>>(responseBody)
                        val errorMessage = errorResponse["error"] ?: "Invalid QR code"
                        when {
                            errorMessage.contains("expired", ignoreCase = true) -> {
                                emit(ApiResult.Error(AuthError.QrExpired))
                            }
                            errorMessage.contains("revoked", ignoreCase = true) -> {
                                emit(ApiResult.Error(AuthError.QrRevoked))
                            }
                            errorMessage.contains("invalid", ignoreCase = true) ||
                            errorMessage.contains("tampered", ignoreCase = true) -> {
                                emit(ApiResult.Error(AuthError.InvalidQrCode))
                            }
                            else -> {
                                emit(ApiResult.Error(AuthError.InvalidCredentials))
                            }
                        }
                    } catch (e: Exception) {
                        emit(ApiResult.Error(AuthError.InvalidCredentials))
                    }
                }
                HttpStatusCode.Forbidden -> {
                    // 403 Forbidden - QR code access denied or expired
                    AppLogger.d("QR login 403 Forbidden: $responseBody", tag = "AuthRepository")
                    emit(ApiResult.Error(AuthError.QrRevoked))
                }
                HttpStatusCode.TooManyRequests -> {
                    emit(ApiResult.Error(AuthError.RateLimited))
                }
                HttpStatusCode(499, "No Cases") -> {
                    emit(ApiResult.Error(AuthError.NoCases))
                }
                else -> {
                    AppLogger.d("QR login error response: $responseBody", tag = "AuthRepository")
                    emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
                }
            }
        } catch (e: Exception) {
            AppLogger.d("QR login error - ${e::class.simpleName}: ${e.message}", tag = "AuthRepository")
            e.printStackTrace()
            emit(ApiResult.Error(AuthError.Unknown("Server unreachable")))
        }
    }

    private fun generateDeviceId(): String {
        // Generate a simple device ID for now
        return "mobile_${de.zemki.metagcompose.util.getCurrentTimeSeconds()}"
    }

    suspend fun saveEmail(email: String) {
        tokenStorage.saveEmail(email)
    }

    suspend fun getSavedEmail(): String? {
        return tokenStorage.getEmail()
    }

    suspend fun clearSavedEmail() {
        tokenStorage.clearEmail()
    }
}