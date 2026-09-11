package com.cryptopos.pos.domain.terminal

import com.cryptopos.pos.domain.model.TerminalTransactionType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Non-sensitive terminal session. Never holds PAN, CVV, track, or PIN.
 */
data class TerminalSessionEvent(
    val from: TerminalTransactionState?,
    val to: TerminalTransactionState,
    val note: String,
    val atEpochMs: Long = System.currentTimeMillis(),
)

data class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val state: TerminalTransactionState = TerminalTransactionState.CREATED,
    val amountRaw: String? = null,
    val amountMinor: Long? = null,
    val currency: String? = null,
    val transactionType: TerminalTransactionType? = null,
    val protocolId: String? = null,
    val protocolCode: String? = null,
    val protocolDisplayName: String? = null,
    val protocolSandboxOutcome: String? = null,
    val environment: String = "SANDBOX",
    val authorizationCode: String? = null,
    val processorReference: String? = null,
    val processorStatus: String? = null,
    val processorMessage: String? = null,
    val signatureRequired: Boolean = false,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    /** Backend terminal session id when Phase 6 remote API is used. */
    val remoteSessionId: String? = null,
    val usedRemoteProcessor: Boolean = false,
    val events: List<TerminalSessionEvent> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastError: String? = null,
)

sealed class TerminalSessionResult {
    data class Ok(val session: TerminalSession) : TerminalSessionResult()
    data class Err(val message: String, val session: TerminalSession?) : TerminalSessionResult()
}
