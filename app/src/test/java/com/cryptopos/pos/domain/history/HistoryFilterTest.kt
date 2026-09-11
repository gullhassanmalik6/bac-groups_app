package com.cryptopos.pos.domain.history

import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {
    private val sample = HistoryTransaction(
        id = "abc-123",
        source = HistorySource.TERMINAL,
        date = "2026-09-11T10:00:00Z",
        amount = "48.50",
        currency = "CAD",
        protocol = "101.2 - Cloud Sale",
        paymentMethod = "VISA ****1111",
        status = "COMPLETED",
        processor = "Backend mock",
        environment = "SANDBOX",
        referenceId = "sbx_deadbeef",
        authorizationCode = "TEST-AABBCC",
        transactionType = "SALE",
        cardBrand = "VISA",
        cardLast4 = "1111",
        processorMessage = null,
        amountMinor = 4850,
    )

    @Test
    fun matches_reference_search() {
        assertTrue(sample.matches(HistoryFilter(query = "sbx_dead")))
        assertTrue(sample.matches(HistoryFilter(query = "TEST-AA")))
        assertFalse(sample.matches(HistoryFilter(query = "missing")))
    }

    @Test
    fun matches_status_protocol_currency_amount() {
        assertTrue(sample.matches(HistoryFilter(status = "COMPLETED")))
        assertTrue(sample.matches(HistoryFilter(protocol = "101.2")))
        assertTrue(sample.matches(HistoryFilter(currency = "CAD")))
        assertTrue(sample.matches(HistoryFilter(amountQuery = "48.50")))
        assertFalse(sample.matches(HistoryFilter(currency = "USD")))
        assertFalse(sample.matches(HistoryFilter(status = "DECLINED")))
    }

    @Test
    fun matches_date_range() {
        assertTrue(sample.matches(HistoryFilter(fromDate = "2026-09-11")))
        assertFalse(sample.matches(HistoryFilter(fromDate = "2026-09-12")))
        assertTrue(sample.matches(HistoryFilter(toDate = "2026-09-11")))
    }
}
