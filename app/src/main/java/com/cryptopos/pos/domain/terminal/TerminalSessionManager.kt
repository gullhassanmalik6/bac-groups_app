package com.cryptopos.pos.domain.terminal

import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import com.cryptopos.pos.domain.protocol.ProtocolSelectionValidation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory active terminal session for the POS UI.
 * Drives state machine through amount + validated protocol profiles.
 */
@Singleton
class TerminalSessionManager @Inject constructor(
    private val protocolCatalog: ProtocolProfileCatalog,
) {

    private val _session = MutableStateFlow<TerminalSession?>(null)
    val session: StateFlow<TerminalSession?> = _session.asStateFlow()

    fun current(): TerminalSession? = _session.value

    fun startNewSession(): TerminalSession {
        val created = TerminalSession(
            events = listOf(
                TerminalSessionEvent(
                    from = null,
                    to = TerminalTransactionState.CREATED,
                    note = "Session created",
                ),
            ),
        )
        _session.value = created
        return created
    }

    fun clear() {
        _session.value = null
    }

    fun enterAmount(
        amountRaw: String,
        currency: String,
        transactionType: TerminalTransactionType,
    ): TerminalSessionResult {
        val active = ensureCreated()
        val amount = amountRaw.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            return TerminalSessionResult.Err("Amount must be greater than zero", active)
        }
        val minor = toMinorUnits(amount, currency)
            ?: return TerminalSessionResult.Err("Unsupported currency: $currency", active)

        return transition(active, TerminalTransactionState.AMOUNT_ENTERED, "Amount entered") { s ->
            s.copy(
                amountRaw = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                amountMinor = minor,
                currency = currency.uppercase(),
                transactionType = transactionType,
            )
        }
    }

    fun selectProtocol(protocolId: String): TerminalSessionResult {
        val active = _session.value
            ?: return TerminalSessionResult.Err("No active session", null)
        if (protocolId.isBlank()) {
            return TerminalSessionResult.Err("Protocol required", active)
        }
        val txnType = active.transactionType
            ?: return TerminalSessionResult.Err("Enter amount before selecting a protocol", active)
        when (val validation = protocolCatalog.validateSelection(protocolId, txnType)) {
            is ProtocolSelectionValidation.Err ->
                return TerminalSessionResult.Err(validation.message, active)
            is ProtocolSelectionValidation.Ok -> {
                val profile = validation.profile
                return transition(
                    active,
                    TerminalTransactionState.PROTOCOL_SELECTED,
                    "Protocol selected: ${profile.displayName}",
                ) { s ->
                    s.copy(
                        protocolId = profile.id,
                        protocolCode = profile.code,
                        protocolDisplayName = profile.displayName,
                        protocolSandboxOutcome = profile.sandboxOutcome.name,
                        environment = if (profile.sandboxOnly) "SANDBOX" else "PRODUCTION",
                    )
                }
            }
        }
    }

    fun presentCard(): TerminalSessionResult =
        advance(TerminalTransactionState.CARD_PRESENTED, "Card presented (stub)")

    fun markCardDataRead(): TerminalSessionResult =
        advance(TerminalTransactionState.CARD_DATA_READ, "Card data read (tokenized stub — no PAN stored)")

    fun startAuthorizing(): TerminalSessionResult =
        advance(TerminalTransactionState.AUTHORIZING, "Authorizing (stub)")

    fun approve(note: String = "Approved (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.APPROVED, note)

    fun decline(note: String = "Declined (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.DECLINED, note)

    fun capture(note: String = "Captured (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.CAPTURED, note)

    fun complete(note: String = "Completed (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.COMPLETED, note)

    fun voidTransaction(note: String = "Voided (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.VOIDED, note)

    fun refund(note: String = "Refunded (sandbox stub)"): TerminalSessionResult =
        advance(TerminalTransactionState.REFUNDED, note)

    fun fail(note: String = "Failed"): TerminalSessionResult =
        advance(TerminalTransactionState.FAILED, note)

    fun cancel(note: String = "Cancelled by operator"): TerminalSessionResult =
        advance(TerminalTransactionState.CANCELLED, note)

    /** Merges processor metadata onto the active session without changing state/events. */
    fun replaceMetadata(updated: TerminalSession) {
        val current = _session.value ?: return
        if (current.id != updated.id && current.remoteSessionId != updated.remoteSessionId) {
            // Allow replace when applying remote snapshot onto local session.
            if (updated.remoteSessionId == null) return
        }
        _session.value = current.copy(
            authorizationCode = updated.authorizationCode ?: current.authorizationCode,
            processorReference = updated.processorReference ?: current.processorReference,
            processorStatus = updated.processorStatus ?: current.processorStatus,
            processorMessage = updated.processorMessage ?: current.processorMessage,
            signatureRequired = updated.signatureRequired || current.signatureRequired,
            cardBrand = updated.cardBrand ?: current.cardBrand,
            cardLast4 = updated.cardLast4 ?: current.cardLast4,
            remoteSessionId = updated.remoteSessionId ?: current.remoteSessionId,
            usedRemoteProcessor = updated.usedRemoteProcessor || current.usedRemoteProcessor,
            lastError = updated.lastError ?: current.lastError,
            updatedAt = Instant.now(),
        )
    }

    /** Replace local session with backend snapshot (Phase 6). */
    fun applyRemoteSnapshot(remote: TerminalSession) {
        _session.value = remote
    }

    private fun advance(to: TerminalTransactionState, note: String): TerminalSessionResult {
        val active = _session.value
            ?: return TerminalSessionResult.Err("No active session", null)
        return transition(active, to, note) { it }
    }

    private fun ensureCreated(): TerminalSession {
        val existing = _session.value
        if (existing != null && existing.state == TerminalTransactionState.CREATED) {
            return existing
        }
        if (existing != null && !TerminalStateMachine.isTerminal(existing.state)) {
            // Restart cleanly when beginning amount entry from a prior non-terminal session mid-flow.
            if (existing.state == TerminalTransactionState.AMOUNT_ENTERED ||
                existing.state == TerminalTransactionState.PROTOCOL_SELECTED
            ) {
                // Allow re-entry from amount screen by resetting to CREATED.
                return startNewSession()
            }
        }
        return startNewSession()
    }

    private fun transition(
        current: TerminalSession,
        to: TerminalTransactionState,
        note: String,
        transform: (TerminalSession) -> TerminalSession,
    ): TerminalSessionResult {
        return try {
            TerminalStateMachine.assertTransition(current.state, to)
            val next = transform(current).copy(
                state = to,
                updatedAt = Instant.now(),
                lastError = null,
                events = current.events + TerminalSessionEvent(
                    from = current.state,
                    to = to,
                    note = note,
                ),
            )
            _session.value = next
            TerminalSessionResult.Ok(next)
        } catch (ex: InvalidTerminalTransitionException) {
            val failed = current.copy(lastError = ex.message)
            _session.update { failed }
            TerminalSessionResult.Err(ex.message ?: "Invalid transition", failed)
        }
    }

    companion object {
        fun toMinorUnits(amount: BigDecimal, currencyCode: String): Long? {
            return try {
                val fraction = Currency.getInstance(currencyCode.uppercase()).defaultFractionDigits
                    .coerceAtLeast(0)
                val scaled = amount.movePointRight(fraction).setScale(0, RoundingMode.HALF_UP)
                scaled.longValueExact()
            } catch (_: Exception) {
                null
            }
        }
    }
}
