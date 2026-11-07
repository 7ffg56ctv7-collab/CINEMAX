package com.cinemax.app.plugins

/**
 * Central policy for future executable provider runtimes.
 * Until a sandbox implementation is integrated, JavaScript plugins remain disabled.
 */
object PluginSecurityPolicy {
    const val EXECUTION_ENABLED = false
    const val REQUIRE_USER_CONSENT = true
    const val REQUIRE_SANDBOX = true
    const val NETWORK_TIMEOUT_MILLIS = 15_000L
    const val MAX_RESPONSE_BYTES = 2_000_000L

    fun mayExecute(isSandboxed: Boolean, userConsented: Boolean): Boolean =
        EXECUTION_ENABLED && isSandboxed && userConsented
}
