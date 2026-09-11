package com.cryptopos.pos.domain.terminal

import com.cryptopos.pos.data.mock.DefaultProtocolProfileCatalog
import com.cryptopos.pos.data.repository.TerminalRemoteGateway
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.processor.MockPaymentProcessor
import com.cryptopos.pos.domain.processor.MockScenario
import com.cryptopos.pos.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPaymentOrchestratorTest {

    private class FakeRemote(
        override val remoteEnabled: Boolean = false,
        private val onAuthorize: (suspend () -> TerminalSession)? = null,
    ) : TerminalRemoteGateway {
        override suspend fun createAndAuthorize(
            amountMinor: Long,
            currency: String,
            transactionType: String,
            protocolCode: String?,
            paymentMethodToken: String,
            scenario: String?,
            idempotencyKey: String,
        ): TerminalSession {
            val handler = onAuthorize
                ?: error("Remote createAndAuthorize should not be called when disabled")
            return handler()
        }
    }

    private class FakeHistory : HistoryRepository {
        val recorded = mutableListOf<TerminalSession>()
        override fun observeHistory(): Flow<List<HistoryTransaction>> =
            MutableStateFlow(emptyList())
        override suspend fun refresh() = Unit
        override suspend fun get(source: HistorySource, id: String): HistoryTransaction? = null
        override suspend fun recordTerminalSession(session: TerminalSession) {
            recorded += session
        }
    }

    private fun build(
        remote: TerminalRemoteGateway = FakeRemote(remoteEnabled = false),
        history: FakeHistory = FakeHistory(),
    ): Triple<TerminalSessionManager, TerminalPaymentOrchestrator, FakeHistory> {
        val catalog = DefaultProtocolProfileCatalog()
        val manager = TerminalSessionManager(catalog)
        val orchestrator = TerminalPaymentOrchestrator(
            manager,
            MockPaymentProcessor(),
            catalog,
            remote,
            history,
        )
        return Triple(manager, orchestrator, history)
    }

    @Test
    fun sale_capture_reaches_completed_local() = runBlocking {
        val (manager, orchestrator, history) = build()
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        manager.selectProtocol("101.2")
        val result = orchestrator.runAuthorization()
        assertTrue(result is TerminalSessionResult.Ok)
        assertEquals(TerminalTransactionState.COMPLETED, (result as TerminalSessionResult.Ok).session.state)
        assertTrue(result.session.authorizationCode!!.startsWith("TEST-"))
        assertFalse(result.session.usedRemoteProcessor)
        assertTrue(history.recorded.isNotEmpty())
        assertEquals(TerminalTransactionState.COMPLETED, history.recorded.last().state)
    }

    @Test
    fun pre_auth_stays_approved() = runBlocking {
        val (manager, orchestrator) = build()
        manager.startNewSession()
        manager.enterAmount("25.00", "USD", TerminalTransactionType.AUTH)
        manager.selectProtocol("101.6")
        val result = orchestrator.runAuthorization()
        assertTrue(result is TerminalSessionResult.Ok)
        assertEquals(TerminalTransactionState.APPROVED, (result as TerminalSessionResult.Ok).session.state)
    }

    @Test
    fun declined_scenario() = runBlocking {
        val (manager, orchestrator, history) = build()
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        manager.selectProtocol("101.1-4dg")
        orchestrator.runAuthorization(scenarioOverride = MockScenario.DECLINED)
        assertEquals(TerminalTransactionState.DECLINED, manager.current()!!.state)
        assertTrue(history.recorded.any { it.state == TerminalTransactionState.DECLINED })
    }

    @Test
    fun remote_success_applies_snapshot() = runBlocking {
        val remote = FakeRemote(remoteEnabled = true) {
            TerminalSession(
                id = "remote-1",
                state = TerminalTransactionState.COMPLETED,
                amountRaw = "10.00",
                amountMinor = 1000,
                currency = "USD",
                transactionType = TerminalTransactionType.SALE,
                protocolId = "101.2",
                protocolCode = "101.2",
                authorizationCode = "TEST-REMOTE",
                processorReference = "sbx_remote",
                usedRemoteProcessor = true,
                remoteSessionId = "remote-1",
                processorMessage = "Approved (SANDBOX)",
            )
        }
        val (manager, orchestrator, history) = build(remote)
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        manager.selectProtocol("101.2")
        val result = orchestrator.runAuthorization()
        assertTrue(result is TerminalSessionResult.Ok)
        assertEquals(TerminalTransactionState.COMPLETED, (result as TerminalSessionResult.Ok).session.state)
        assertTrue(result.session.usedRemoteProcessor)
        assertEquals("TEST-REMOTE", result.session.authorizationCode)
        assertTrue(history.recorded.any { it.usedRemoteProcessor })
    }

    @Test
    fun remote_failure_falls_back_to_local_mock() = runBlocking {
        val remote = FakeRemote(remoteEnabled = true) {
            throw IllegalStateException("API not deployed")
        }
        val (manager, orchestrator, history) = build(remote)
        manager.startNewSession()
        manager.enterAmount("10.00", "USD", TerminalTransactionType.SALE)
        manager.selectProtocol("101.2")
        val result = orchestrator.runAuthorization()
        assertTrue(result is TerminalSessionResult.Ok)
        assertEquals(TerminalTransactionState.COMPLETED, (result as TerminalSessionResult.Ok).session.state)
        assertFalse(result.session.usedRemoteProcessor)
        assertTrue(result.session.processorMessage!!.contains("Local mock"))
        assertTrue(history.recorded.isNotEmpty())
    }
}
