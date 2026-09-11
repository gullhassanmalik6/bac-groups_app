package com.cryptopos.pos.domain.terminal

import com.cryptopos.pos.data.mock.DefaultProtocolProfileCatalog
import com.cryptopos.pos.domain.model.TerminalTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TerminalStateMachineTest {

    @Test
    fun happyPath_sale_to_completed() {
        var state = TerminalTransactionState.CREATED
        state = TerminalStateMachine.transition(state, TerminalTransactionState.AMOUNT_ENTERED)
        state = TerminalStateMachine.transition(state, TerminalTransactionState.PROTOCOL_SELECTED)
        state = TerminalStateMachine.transition(state, TerminalTransactionState.CARD_PRESENTED)
        state = TerminalStateMachine.transition(state, TerminalTransactionState.AUTHORIZING)
        state = TerminalStateMachine.transition(state, TerminalTransactionState.APPROVED)
        state = TerminalStateMachine.transition(state, TerminalTransactionState.COMPLETED)
        assertEquals(TerminalTransactionState.COMPLETED, state)
        assertFalse(TerminalStateMachine.isTerminal(state)) // can still refund
    }

    @Test
    fun happyPath_with_card_data_read_and_capture() {
        var state = TerminalTransactionState.CREATED
        listOf(
            TerminalTransactionState.AMOUNT_ENTERED,
            TerminalTransactionState.PROTOCOL_SELECTED,
            TerminalTransactionState.CARD_PRESENTED,
            TerminalTransactionState.CARD_DATA_READ,
            TerminalTransactionState.AUTHORIZING,
            TerminalTransactionState.APPROVED,
            TerminalTransactionState.CAPTURED,
            TerminalTransactionState.COMPLETED,
        ).forEach { next ->
            state = TerminalStateMachine.transition(state, next)
        }
        assertEquals(TerminalTransactionState.COMPLETED, state)
    }

    @Test
    fun decline_path() {
        var state = TerminalTransactionState.CREATED
        listOf(
            TerminalTransactionState.AMOUNT_ENTERED,
            TerminalTransactionState.PROTOCOL_SELECTED,
            TerminalTransactionState.CARD_PRESENTED,
            TerminalTransactionState.AUTHORIZING,
            TerminalTransactionState.DECLINED,
        ).forEach { next ->
            state = TerminalStateMachine.transition(state, next)
        }
        assertTrue(TerminalStateMachine.isTerminal(state))
    }

    @Test
    fun cancel_from_each_cancellable_state() {
        val cancellable = listOf(
            TerminalTransactionState.CREATED,
            TerminalTransactionState.AMOUNT_ENTERED,
            TerminalTransactionState.PROTOCOL_SELECTED,
            TerminalTransactionState.CARD_PRESENTED,
            TerminalTransactionState.CARD_DATA_READ,
            TerminalTransactionState.AUTHORIZING,
            TerminalTransactionState.APPROVED,
        )
        cancellable.forEach { from ->
            assertTrue(
                "Expected cancel from $from",
                TerminalStateMachine.canTransition(from, TerminalTransactionState.CANCELLED),
            )
        }
    }

    @Test
    fun invalid_skip_amount() {
        assertFalse(
            TerminalStateMachine.canTransition(
                TerminalTransactionState.CREATED,
                TerminalTransactionState.PROTOCOL_SELECTED,
            ),
        )
        try {
            TerminalStateMachine.assertTransition(
                TerminalTransactionState.CREATED,
                TerminalTransactionState.APPROVED,
            )
            fail("Expected InvalidTerminalTransitionException")
        } catch (_: InvalidTerminalTransitionException) {
            // expected
        }
    }

    @Test
    fun terminal_states_have_no_outbound() {
        listOf(
            TerminalTransactionState.DECLINED,
            TerminalTransactionState.FAILED,
            TerminalTransactionState.CANCELLED,
            TerminalTransactionState.VOIDED,
            TerminalTransactionState.REFUNDED,
        ).forEach { state ->
            assertTrue(state.name, TerminalStateMachine.isTerminal(state))
            assertTrue(TerminalStateMachine.allowedTargets(state).isEmpty())
        }
    }

    @Test
    fun refund_from_completed() {
        assertTrue(
            TerminalStateMachine.canTransition(
                TerminalTransactionState.COMPLETED,
                TerminalTransactionState.REFUNDED,
            ),
        )
    }

    @Test
    fun void_from_approved_and_captured() {
        assertTrue(
            TerminalStateMachine.canTransition(
                TerminalTransactionState.APPROVED,
                TerminalTransactionState.VOIDED,
            ),
        )
        assertTrue(
            TerminalStateMachine.canTransition(
                TerminalTransactionState.CAPTURED,
                TerminalTransactionState.VOIDED,
            ),
        )
    }

    @Test
    fun every_defined_edge_is_accepted() {
        TerminalTransactionState.entries.forEach { from ->
            TerminalStateMachine.allowedTargets(from).forEach { to ->
                assertEquals(to, TerminalStateMachine.transition(from, to))
            }
        }
    }
}

class TerminalSessionManagerTest {

    private fun manager() = TerminalSessionManager(DefaultProtocolProfileCatalog())

    @Test
    fun amount_then_protocol_advances_states() {
        val manager = manager()
        manager.startNewSession()
        val amount = manager.enterAmount("48.50", "CAD", TerminalTransactionType.SALE)
        assertTrue(amount is TerminalSessionResult.Ok)
        assertEquals(
            TerminalTransactionState.AMOUNT_ENTERED,
            (amount as TerminalSessionResult.Ok).session.state,
        )
        assertEquals(4850L, amount.session.amountMinor)

        val protocol = manager.selectProtocol("101.1-4dg")
        assertTrue(protocol is TerminalSessionResult.Ok)
        assertEquals(
            TerminalTransactionState.PROTOCOL_SELECTED,
            (protocol as TerminalSessionResult.Ok).session.state,
        )
        assertEquals("101.1", protocol.session.protocolCode)
        assertEquals("CAPTURE", protocol.session.protocolSandboxOutcome)
        assertEquals(3, protocol.session.events.size)
    }

    @Test
    fun rejects_zero_amount() {
        val manager = manager()
        manager.startNewSession()
        val result = manager.enterAmount("0", "USD", TerminalTransactionType.SALE)
        assertTrue(result is TerminalSessionResult.Err)
    }

    @Test
    fun rejects_invalid_protocol_transition_without_amount() {
        val manager = manager()
        manager.startNewSession()
        val result = manager.selectProtocol("101.1-4dg")
        assertTrue(result is TerminalSessionResult.Err)
    }

    @Test
    fun rejects_protocol_unsupported_for_txn_type() {
        val manager = manager()
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        // 201.1 only supports COMPLETION / VOID
        val result = manager.selectProtocol("201.1")
        assertTrue(result is TerminalSessionResult.Err)
    }

    @Test
    fun stub_auth_path_to_completed() {
        val manager = manager()
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        manager.selectProtocol("101.2")
        assertTrue(manager.presentCard() is TerminalSessionResult.Ok)
        assertTrue(manager.startAuthorizing() is TerminalSessionResult.Ok)
        assertTrue(manager.approve() is TerminalSessionResult.Ok)
        assertTrue(manager.complete() is TerminalSessionResult.Ok)
        assertEquals(TerminalTransactionState.COMPLETED, manager.current()?.state)
    }

    @Test
    fun toMinorUnits_cad() {
        assertEquals(
            485000L,
            TerminalSessionManager.toMinorUnits(java.math.BigDecimal("4850.00"), "CAD"),
        )
    }
}
