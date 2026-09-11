package com.cryptopos.pos.domain.processor

import com.cryptopos.pos.domain.model.SandboxOutcome
import com.cryptopos.pos.domain.model.TerminalTransactionType

/**
 * Processor response status — sandbox or future certified PSP.
 * Never implies real funds moved unless a live adapter is configured.
 */
enum class ProcessorStatus {
    APPROVED,
    DECLINED,
    CANCELLED,
    TIMEOUT,
    PROCESSING,
    ERROR,
}

data class ProcessorAuthorizeRequest(
    val sessionId: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: TerminalTransactionType,
    val protocolId: String?,
    val protocolCode: String?,
    val sandboxOutcome: SandboxOutcome?,
    /** Token only — never PAN/CVV. */
    val paymentMethodToken: String = "pm_test_visa_success",
    val scenarioOverride: MockScenario? = null,
)

data class ProcessorCaptureRequest(
    val sessionId: String,
    val processorReference: String,
    val amountMinor: Long,
    val currency: String,
)

data class ProcessorVoidRequest(
    val sessionId: String,
    val processorReference: String,
)

data class ProcessorRefundRequest(
    val sessionId: String,
    val processorReference: String,
    val amountMinor: Long,
    val currency: String,
)

data class ProcessorCompleteRequest(
    val sessionId: String,
    val processorReference: String,
    val amountMinor: Long,
    val currency: String,
)

/**
 * Safe processor result. Authorization codes are TEST-/SANDBOX-prefixed for mocks.
 */
data class ProcessorResult(
    val transactionId: String,
    val amountMinor: Long,
    val currency: String,
    val status: ProcessorStatus,
    val authorizationCode: String?,
    val processorReference: String,
    val timestampEpochMs: Long,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val signatureRequired: Boolean = false,
    val environment: String = "SANDBOX",
    val message: String? = null,
    val sandbox: Boolean = true,
)

enum class MockScenario {
    SUCCESS,
    DECLINED,
    TIMEOUT,
    CANCELLED,
    ERROR,
    SIGNATURE,
}

/**
 * Abstraction for mock and future certified payment processors.
 */
interface PaymentProcessor {
    val name: String
    val isSandbox: Boolean

    suspend fun authorize(request: ProcessorAuthorizeRequest): ProcessorResult
    suspend fun capture(request: ProcessorCaptureRequest): ProcessorResult
    suspend fun voidTransaction(request: ProcessorVoidRequest): ProcessorResult
    suspend fun refund(request: ProcessorRefundRequest): ProcessorResult
    suspend fun completeAuthorization(request: ProcessorCompleteRequest): ProcessorResult
    suspend fun getTransactionStatus(processorReference: String): ProcessorResult
}
