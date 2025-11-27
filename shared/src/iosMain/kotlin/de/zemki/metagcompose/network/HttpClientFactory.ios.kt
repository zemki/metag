package de.zemki.metagcompose.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(): HttpClient {
    return createBaseHttpClient().config {
        engine {
            // iOS-specific configuration
        }
    }
}