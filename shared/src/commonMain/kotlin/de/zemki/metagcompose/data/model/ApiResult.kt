package de.zemki.metagcompose.data.model

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

sealed class AuthError : Exception() {
    data object InvalidCredentials : AuthError()
    data object NoCases : AuthError()
    data object RateLimited : AuthError()
    data object NetworkError : AuthError()
    data object MartProject : AuthError()
    data object QrExpired : AuthError()
    data object QrRevoked : AuthError()
    data object InvalidQrCode : AuthError()
    data class Unknown(override val message: String) : AuthError()
}