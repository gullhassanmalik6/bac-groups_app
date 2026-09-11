package com.cryptopos.pos.data.repository

import com.cryptopos.pos.domain.terminal.TerminalSession

/**
 * Backend terminal session API used by [com.cryptopos.pos.domain.terminal.TerminalPaymentOrchestrator].
 */
interface TerminalRemoteGateway {
    val remoteEnabled: Boolean

    suspend fun createAndAuthorize(
        amountMinor: Long,
        currency: String,
        transactionType: String,
        protocolCode: String?,
        paymentMethodToken: String,
        scenario: String? = null,
        idempotencyKey: String = "POS-${java.util.UUID.randomUUID()}",
    ): TerminalSession
}
