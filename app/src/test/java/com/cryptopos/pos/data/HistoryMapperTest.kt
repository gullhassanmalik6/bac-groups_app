package com.cryptopos.pos.data

import com.cryptopos.pos.data.mapper.toHistoryEntity
import com.cryptopos.pos.data.mapper.toHistoryTransaction
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMapperTest {
    @Test
    fun maps_completed_session_to_history_entity() {
        val session = TerminalSession(
            id = "local-1",
            state = TerminalTransactionState.COMPLETED,
            amountRaw = "10.00",
            amountMinor = 1000,
            currency = "USD",
            transactionType = TerminalTransactionType.SALE,
            protocolCode = "101.2",
            protocolDisplayName = "101.2 - Cloud Sale",
            authorizationCode = "TEST-XYZ",
            processorReference = "sbx_1",
            usedRemoteProcessor = false,
            cardBrand = "VISA",
            cardLast4 = "1111",
        )
        val entity = session.toHistoryEntity()!!
        val history = entity.toHistoryTransaction()
        assertEquals(HistorySource.TERMINAL, history.source)
        assertEquals("COMPLETED", history.status)
        assertEquals("101.2 - Cloud Sale", history.protocol)
        assertEquals("Local mock", history.processor)
        assertTrue(history.paymentMethod!!.contains("1111"))
    }

    @Test
    fun skips_in_progress_sessions() {
        val session = TerminalSession(
            state = TerminalTransactionState.PROTOCOL_SELECTED,
            amountRaw = "10.00",
            currency = "USD",
        )
        assertNull(session.toHistoryEntity())
    }
}
