package com.cryptopos.pos.domain

import com.cryptopos.pos.domain.model.Receipt
import com.cryptopos.pos.domain.usecase.toPrintLines
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptPrintLinesTest {
    @Test
    fun `includes core receipt fields`() {
        val lines = Receipt(
            id = "r1",
            receiptNumber = "RCP-9",
            merchantName = "Acme",
            amount = "25.00",
            currency = "SAR",
            status = "success",
            gateway = "sandbox",
            createdAt = "2026-01-01T00:00:00Z",
            transactionId = "tx1",
            lines = mapOf("VAT" to "0.00"),
            qrPayload = "cryptopos://receipt/RCP-9",
        ).toPrintLines()

        assertTrue(lines.any { it.contains("Acme") })
        assertTrue(lines.any { it.contains("25.00") })
        assertTrue(lines.any { it.contains("RCP-9") })
    }
}
