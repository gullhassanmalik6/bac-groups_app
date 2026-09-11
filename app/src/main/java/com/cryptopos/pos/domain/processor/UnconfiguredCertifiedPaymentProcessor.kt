package com.cryptopos.pos.domain.processor

import javax.inject.Inject

/**
 * Fail-closed stub for a future certified SoftPOS / acquirer SDK.
 *
 * Does **not** invent Visa, Mastercard, or Verifone host protocols.
 * Selected only when [BuildConfig.PAYMENT_PROCESSOR] is a certified_* key —
 * until a real adapter + credentials exist, every call returns [ProcessorStatus.ERROR].
 */
class UnconfiguredCertifiedPaymentProcessor @Inject constructor() : PaymentProcessor {
    override val name: String = "certified_unconfigured"
    override val isSandbox: Boolean = true

    private fun blocked(sessionId: String, amountMinor: Long = 0, currency: String = "USD"): ProcessorResult =
        ProcessorResult(
            transactionId = sessionId,
            amountMinor = amountMinor,
            currency = currency,
            status = ProcessorStatus.ERROR,
            authorizationCode = null,
            processorReference = "uncfg_${System.currentTimeMillis()}",
            timestampEpochMs = System.currentTimeMillis(),
            environment = "SANDBOX",
            message = BLOCKED_MESSAGE,
            sandbox = true,
        )

    override suspend fun authorize(request: ProcessorAuthorizeRequest): ProcessorResult =
        blocked(request.sessionId, request.amountMinor, request.currency)

    override suspend fun capture(request: ProcessorCaptureRequest): ProcessorResult =
        blocked(request.sessionId, request.amountMinor, request.currency)

    override suspend fun voidTransaction(request: ProcessorVoidRequest): ProcessorResult =
        blocked(request.sessionId)

    override suspend fun refund(request: ProcessorRefundRequest): ProcessorResult =
        blocked(request.sessionId, request.amountMinor, request.currency)

    override suspend fun completeAuthorization(request: ProcessorCompleteRequest): ProcessorResult =
        blocked(request.sessionId, request.amountMinor, request.currency)

    override suspend fun getTransactionStatus(processorReference: String): ProcessorResult =
        blocked(sessionId = "status", amountMinor = 0, currency = "USD").copy(
            processorReference = processorReference,
        )

    companion object {
        const val BLOCKED_MESSAGE =
            "Certified PSP/acquirer is not configured. Use mock_sandbox until a licensed adapter is provisioned."
    }
}
