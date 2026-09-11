package com.cryptopos.pos.domain.terminal

/**
 * Strict POS terminal transaction states (Phase 2).
 *
 * No card PAN/CVV is associated with these states — only non-sensitive session metadata.
 */
enum class TerminalTransactionState {
    CREATED,
    AMOUNT_ENTERED,
    PROTOCOL_SELECTED,
    CARD_PRESENTED,
    CARD_DATA_READ,
    AUTHORIZING,
    APPROVED,
    DECLINED,
    CAPTURED,
    COMPLETED,
    VOIDED,
    REFUNDED,
    CANCELLED,
    FAILED,
}

class InvalidTerminalTransitionException(
    val from: TerminalTransactionState?,
    val to: TerminalTransactionState,
) : IllegalStateException("Invalid terminal transition: ${from ?: "∅"} → $to")

/**
 * Pure state machine — no I/O, no payment processor calls.
 */
object TerminalStateMachine {

    private val ALLOWED: Map<TerminalTransactionState, Set<TerminalTransactionState>> = mapOf(
        TerminalTransactionState.CREATED to setOf(
            TerminalTransactionState.AMOUNT_ENTERED,
            TerminalTransactionState.CANCELLED,
        ),
        TerminalTransactionState.AMOUNT_ENTERED to setOf(
            TerminalTransactionState.PROTOCOL_SELECTED,
            TerminalTransactionState.CANCELLED,
        ),
        TerminalTransactionState.PROTOCOL_SELECTED to setOf(
            TerminalTransactionState.CARD_PRESENTED,
            TerminalTransactionState.CANCELLED,
        ),
        TerminalTransactionState.CARD_PRESENTED to setOf(
            TerminalTransactionState.CARD_DATA_READ,
            TerminalTransactionState.AUTHORIZING,
            TerminalTransactionState.CANCELLED,
            TerminalTransactionState.FAILED,
        ),
        TerminalTransactionState.CARD_DATA_READ to setOf(
            TerminalTransactionState.AUTHORIZING,
            TerminalTransactionState.CANCELLED,
            TerminalTransactionState.FAILED,
        ),
        TerminalTransactionState.AUTHORIZING to setOf(
            TerminalTransactionState.APPROVED,
            TerminalTransactionState.DECLINED,
            TerminalTransactionState.FAILED,
            TerminalTransactionState.CANCELLED,
        ),
        TerminalTransactionState.APPROVED to setOf(
            TerminalTransactionState.CAPTURED,
            TerminalTransactionState.COMPLETED,
            TerminalTransactionState.VOIDED,
            TerminalTransactionState.CANCELLED,
        ),
        TerminalTransactionState.CAPTURED to setOf(
            TerminalTransactionState.COMPLETED,
            TerminalTransactionState.REFUNDED,
            TerminalTransactionState.VOIDED,
        ),
        TerminalTransactionState.COMPLETED to setOf(
            TerminalTransactionState.REFUNDED,
        ),
        TerminalTransactionState.DECLINED to emptySet(),
        TerminalTransactionState.FAILED to emptySet(),
        TerminalTransactionState.CANCELLED to emptySet(),
        TerminalTransactionState.VOIDED to emptySet(),
        TerminalTransactionState.REFUNDED to emptySet(),
    )

    fun allowedTargets(from: TerminalTransactionState): Set<TerminalTransactionState> =
        ALLOWED[from].orEmpty()

    fun canTransition(from: TerminalTransactionState, to: TerminalTransactionState): Boolean =
        to in allowedTargets(from)

    fun assertTransition(from: TerminalTransactionState, to: TerminalTransactionState) {
        if (!canTransition(from, to)) {
            throw InvalidTerminalTransitionException(from, to)
        }
    }

    /** Returns [to] after validating; throws on illegal transition. */
    fun transition(from: TerminalTransactionState, to: TerminalTransactionState): TerminalTransactionState {
        assertTransition(from, to)
        return to
    }

    fun isTerminal(state: TerminalTransactionState): Boolean =
        allowedTargets(state).isEmpty()
}
