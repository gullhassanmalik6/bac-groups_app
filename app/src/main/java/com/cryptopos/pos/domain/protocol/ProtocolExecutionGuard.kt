package com.cryptopos.pos.domain.protocol

/**
 * Blocks live authorization unless a protocol is documented by a licensed PSP.
 * Sandbox may still use MockPaymentProcessor.
 */
object ProtocolExecutionGuard {
    const val PROVIDER_CONFIGURATION_REQUIRED =
        "PROVIDER_CONFIGURATION_REQUIRED — protocol is a catalog label only until official processor documentation is wired. Dummy APPROVED is not allowed in production."

    fun allowSandboxSimulation(environment: String, documented: Boolean): Boolean {
        val env = environment.uppercase()
        if (env == "PRODUCTION") return documented
        return true
    }
}
