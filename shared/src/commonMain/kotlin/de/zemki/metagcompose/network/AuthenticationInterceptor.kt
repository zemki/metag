package de.zemki.metagcompose.network

import de.zemki.metagcompose.data.storage.TokenStorage
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

class AuthenticationInterceptor(private val tokenStorage: TokenStorage) {
    
    fun install(client: HttpClient) {
        client.plugin(HttpSend).intercept { request ->
            val token = tokenStorage.getToken()
            if (token != null) {
                request.headers.append("Authorization", "Bearer $token")
            }
            execute(request)
        }
    }
}