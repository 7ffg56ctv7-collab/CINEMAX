package com.cinemax.app.addons

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createCinemaxHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    // Do not silently follow addon-controlled redirects to an unvalidated host or scheme.
    followRedirects = false
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
