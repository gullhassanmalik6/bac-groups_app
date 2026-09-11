package com.cryptopos.pos.data

import com.cryptopos.pos.data.mapper.toDomain
import com.cryptopos.pos.data.remote.dto.TerminalSessionDto
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSessionMapperTest {
    @Test
    fun maps_backend_session_to_domain() {
        val dto = TerminalSessionDto(
            id = "abc-123",
            state = "COMPLETED",
            amountMinor = 4850,
            currency = "CAD",
            transactionType = "SALE",
            protocolId = "101.2",
            protocolCode = "101.2",
            protocolLabel = "101.2 - Cloud Sale 6 DG",
            sandboxOutcome = "capture",
            authorizationCode = "TEST-AABBCC",
            processorReference = "sbx_deadbeef",
            cardLast4 = "1111",
            cardBrand = "VISA",
        )
        val session = dto.toDomain()
        assertEquals(TerminalTransactionState.COMPLETED, session.state)
        assertEquals(4850L, session.amountMinor)
        assertEquals("48.50", session.amountRaw)
        assertEquals("TEST-AABBCC", session.authorizationCode)
        assertTrue(session.usedRemoteProcessor)
        assertEquals("CAPTURE", session.protocolSandboxOutcome)
    }
}
