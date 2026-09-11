package com.cryptopos.pos.domain.processor

/**
 * Resolves the on-device [PaymentProcessor] from a config key.
 * Default is always mock sandbox — never silently selects a "live" inventing adapter.
 */
object PaymentProcessorResolver {
    fun resolve(
        key: String,
        mock: PaymentProcessor,
        certifiedUnconfigured: PaymentProcessor,
    ): PaymentProcessor {
        return when (key.trim().lowercase()) {
            "", "mock", "mock_sandbox", "sandbox" -> mock
            "certified", "certified_psp", "certified_unconfigured", "acquirer" -> certifiedUnconfigured
            else -> mock
        }
    }
}
