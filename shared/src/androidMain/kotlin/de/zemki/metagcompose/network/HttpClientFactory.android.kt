package de.zemki.metagcompose.network

import io.ktor.client.*
import io.ktor.client.engine.android.*

actual fun createHttpClient(): HttpClient {
    return createBaseHttpClient().config {
        engine {
            // Android-specific configuration
        }
    }
}