package com.cryptopos.pos.domain.processor

import com.cryptopos.pos.domain.model.SandboxOutcome
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * Deterministic sandbox processor — never moves real money, never accepts raw PAN/CVV.
 *
 * Auth codes and references are always TEST-/sbx_ prefixed.
 */
@Singleton
class MockPaymentProcessor @Inject constructor() : PaymentProcessor {

    override val name: String = "mock_sandbox"
    override val isSandbox: Boolean = true

    private val store = LinkedHashMap<String, ProcessorResult>()

    override suspend fun authorize(request: ProcessorAuthorizeRequest): ProcessorResult {
        delay(PROCESS_DELAY_MS)
        rejectRawCardFields(request.paymentMethodToken)

        val scenario = resolveScenario(request)
        val reference = "sbx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txnId = request.sessionId

        val result = when (scenario) {
            MockScenario.DECLINED -> base(
                txnId, request, reference, ProcessorStatus.DECLINED,
                auth = null,
                message = "Sandbox declined (test scenario)",
            )
            MockScenario.TIMEOUT -> base(
                txnId, request, reference, ProcessorStatus.TIMEOUT,
                auth = null,
                message = "Sandbox processor timeout",
            )
            MockScenario.CANCELLED -> base(
                txnId, request, reference, ProcessorStatus.CANCELLED,
                auth = null,
                message = "Sandbox cancelled",
            )
            MockScenario.ERROR -> base(
                txnId, request, reference, ProcessorStatus.ERROR,
                auth = null,
                message = "Sandbox processor error",
            )
            MockScenario.SIGNATURE -> base(
                txnId, request, reference, ProcessorStatus.APPROVED,
                auth = testAuth(),
                signatureRequired = true,
                message = "Approved — signature required (SANDBOX)",
            )
            MockScenario.SUCCESS -> {
                val outcome = request.sandboxOutcome ?: SandboxOutcome.CAPTURE
                when (outcome) {
                    SandboxOutcome.PRE_AUTH, SandboxOutcome.OFFLINE_AUTH -> base(
                        txnId, request, reference, ProcessorStatus.APPROVED,
                        auth = testAuth(),
                        message = "Pre-authorized / offline auth (SANDBOX) — not captured",
                    )
                    SandboxOutcome.SIGNATURE -> base(
                        txnId, request, reference, ProcessorStatus.APPROVED,
                        auth = testAuth(),
                        signatureRequired = true,
                        message = "Approved — signature required (SANDBOX)",
                    )
                    SandboxOutcome.COMPLETION -> base(
                        txnId, request, reference, ProcessorStatus.APPROVED,
                        auth = testAuth(),
                        message = "Completion auth approved (SANDBOX)",
                    )
                    SandboxOutcome.FORCE_POST, SandboxOutcome.CAPTURE -> base(
                        txnId, request, reference, ProcessorStatus.APPROVED,
                        auth = testAuth(),
                        message = "Approved (SANDBOX)",
                    )
                }
            }
        }
        store[reference] = result
        return result
    }

    override suspend fun capture(request: ProcessorCaptureRequest): ProcessorResult {
        delay(PROCESS_DELAY_MS / 2)
        val prior = store[request.processorReference]
            ?: return errorResult(request.sessionId, request.amountMinor, request.currency, "Unknown reference")
        if (prior.status != ProcessorStatus.APPROVED) {
            return prior.copy(
                status = ProcessorStatus.ERROR,
                message = "Capture not allowed from ${prior.status}",
                timestampEpochMs = System.currentTimeMillis(),
            )
        }
        val captured = prior.copy(
            status = ProcessorStatus.APPROVED,
            message = "Captured (SANDBOX)",
            authorizationCode = prior.authorizationCode ?: testAuth(),
            timestampEpochMs = System.currentTimeMillis(),
            processorReference = request.processorReference,
        )
        // Represent capture as approved+message; session manager maps to CAPTURED state.
        store[request.processorReference] = captured.copy(message = "CAPTURED_SANDBOX")
        return store[request.processorReference]!!
    }

    override suspend fun voidTransaction(request: ProcessorVoidRequest): ProcessorResult {
        delay(PROCESS_DELAY_MS / 2)
        val prior = store[request.processorReference]
            ?: return errorResult(request.sessionId, 0, "XXX", "Unknown reference")
        val voided = prior.copy(
            status = ProcessorStatus.CANCELLED,
            message = "Voided (SANDBOX)",
            timestampEpochMs = System.currentTimeMillis(),
        )
        store[request.processorReference] = voided
        return voided
    }

    override suspend fun refund(request: ProcessorRefundRequest): ProcessorResult {
        delay(PROCESS_DELAY_MS / 2)
        val prior = store[request.processorReference]
            ?: return errorResult(request.sessionId, request.amountMinor, request.currency, "Unknown reference")
        val refunded = prior.copy(
            status = ProcessorStatus.APPROVED,
            amountMinor = request.amountMinor,
            message = "Refunded (SANDBOX)",
            authorizationCode = testAuth(),
            timestampEpochMs = System.currentTimeMillis(),
        )
        store["rf_${request.processorReference}"] = refunded
        return refunded
    }

    override suspend fun completeAuthorization(request: ProcessorCompleteRequest): ProcessorResult {
        delay(PROCESS_DELAY_MS / 2)
        val prior = store[request.processorReference]
            ?: return errorResult(request.sessionId, request.amountMinor, request.currency, "Unknown reference")
        val completed = prior.copy(
            status = ProcessorStatus.APPROVED,
            message = "Authorization completed (SANDBOX)",
            timestampEpochMs = System.currentTimeMillis(),
        )
        store[request.processorReference] = completed
        return completed
    }

    override suspend fun getTransactionStatus(processorReference: String): ProcessorResult {
        return store[processorReference]
            ?: errorResult("unknown", 0, "XXX", "Not found")
    }

    private fun resolveScenario(request: ProcessorAuthorizeRequest): MockScenario {
        request.scenarioOverride?.let { return it }
        val token = request.paymentMethodToken.lowercase()
        when {
            token.contains("declined") -> return MockScenario.DECLINED
            token.contains("timeout") -> return MockScenario.TIMEOUT
            token.contains("cancel") -> return MockScenario.CANCELLED
            token.contains("error") -> return MockScenario.ERROR
            token.contains("sig") || token.contains("signature") -> return MockScenario.SIGNATURE
        }
        // Amount minor ending patterns for easy keypad testing:
        // …13 → decline, …99 → timeout, …77 → error
        when (request.amountMinor % 100) {
            13L -> return MockScenario.DECLINED
            99L -> return MockScenario.TIMEOUT
            77L -> return MockScenario.ERROR
        }
        if (request.sandboxOutcome == SandboxOutcome.SIGNATURE) {
            return MockScenario.SIGNATURE
        }
        return MockScenario.SUCCESS
    }

    private fun rejectRawCardFields(token: String) {
        require(token.startsWith("pm_test_") || token.startsWith("pm_")) {
            "MockPaymentProcessor requires a payment method token — never send raw PAN/CVV"
        }
    }

    private fun base(
        txnId: String,
        request: ProcessorAuthorizeRequest,
        reference: String,
        status: ProcessorStatus,
        auth: String?,
        signatureRequired: Boolean = false,
        message: String?,
    ) = ProcessorResult(
        transactionId = txnId,
        amountMinor = request.amountMinor,
        currency = request.currency,
        status = status,
        authorizationCode = auth,
        processorReference = reference,
        timestampEpochMs = System.currentTimeMillis(),
        cardBrand = "VISA",
        cardLast4 = "1111",
        signatureRequired = signatureRequired,
        environment = "SANDBOX",
        message = message,
        sandbox = true,
    )

    private fun errorResult(txnId: String, amountMinor: Long, currency: String, message: String) =
        ProcessorResult(
            transactionId = txnId,
            amountMinor = amountMinor,
            currency = currency,
            status = ProcessorStatus.ERROR,
            authorizationCode = null,
            processorReference = "sbx_err_${UUID.randomUUID().toString().take(8)}",
            timestampEpochMs = System.currentTimeMillis(),
            message = message,
            sandbox = true,
        )

    private fun testAuth(): String =
        "TEST-${UUID.randomUUID().toString().replace("-", "").take(6).uppercase()}"

    companion object {
        private const val PROCESS_DELAY_MS = 600L
    }
}
