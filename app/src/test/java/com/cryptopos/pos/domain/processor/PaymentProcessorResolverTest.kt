package com.cryptopos.pos.domain.processor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentProcessorResolverTest {
    private val mock = MockPaymentProcessor()
    private val certified = UnconfiguredCertifiedPaymentProcessor()

    @Test
    fun defaults_and_aliases_resolve_to_mock() {
        assertSame(mock, PaymentProcessorResolver.resolve("mock_sandbox", mock, certified))
        assertSame(mock, PaymentProcessorResolver.resolve("sandbox", mock, certified))
        assertSame(mock, PaymentProcessorResolver.resolve("", mock, certified))
        assertSame(mock, PaymentProcessorResolver.resolve("unknown_vendor", mock, certified))
    }

    @Test
    fun certified_keys_resolve_to_fail_closed_stub() {
        assertSame(certified, PaymentProcessorResolver.resolve("certified_psp", mock, certified))
        assertSame(certified, PaymentProcessorResolver.resolve("acquirer", mock, certified))
    }

    @Test
    fun certified_stub_never_approves() = runBlocking {
        val result = certified.authorize(
            ProcessorAuthorizeRequest(
                sessionId = "s1",
                amountMinor = 1000,
                currency = "CAD",
                transactionType = com.cryptopos.pos.domain.model.TerminalTransactionType.SALE,
                protocolId = null,
                protocolCode = "101.2",
                sandboxOutcome = null,
            ),
        )
        assertEquals(ProcessorStatus.ERROR, result.status)
        assertTrue(result.message!!.contains("not configured", ignoreCase = true))
        assertTrue(result.sandbox)
    }
}
