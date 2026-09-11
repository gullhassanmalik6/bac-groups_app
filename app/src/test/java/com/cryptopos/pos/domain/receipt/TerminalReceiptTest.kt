package com.cryptopos.pos.domain.receipt

import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.printer.MockPrinterAdapter
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TerminalReceiptTest {
    private val factory = TerminalReceiptFactory()

    @Test
    fun builds_thermal_layout_without_pan() {
        val session = TerminalSession(
            id = "TEST-001743",
            state = TerminalTransactionState.COMPLETED,
            amountRaw = "4850.00",
            amountMinor = 485000,
            currency = "CAD",
            transactionType = TerminalTransactionType.SALE,
            protocolCode = "101.2",
            protocolDisplayName = "101.2 - Cloud Sale",
            authorizationCode = "TEST-800040",
            processorReference = "TEST-XXXXXXXX",
            cardBrand = "MASTERCARD",
            cardLast4 = "1111",
            environment = "SANDBOX",
        )
        val receipt = factory.fromSession(session, "DEMO MERCHANT")
        val text = receipt.toPrintLines().joinToString("\n")
        assertTrue(text.contains("DEMO MERCHANT"))
        assertTrue(text.contains("CA$4850.00") || text.contains("CA$4,850.00") || text.contains("4850.00"))
        assertTrue(text.contains("TEST MASTERCARD"))
        assertTrue(text.contains("****1111"))
        assertTrue(text.contains("APPROVED - SANDBOX"))
        assertTrue(text.contains("TEST-800040"))
        assertTrue(text.contains("SANDBOX TRANSACTION"))
        assertFalse(text.contains("cvv", ignoreCase = true))
        assertFalse(Regex("""\d{13,19}""").containsMatchIn(text.replace("****1111", "")))
    }

    @Test
    fun mock_printer_rejects_pan_and_cvv() = runBlocking {
        val printer = MockPrinterAdapter()
        printer.connect()
        try {
            printer.printReceipt(listOf("PAN 4111111111111111")).getOrThrow()
            fail("Expected PAN rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        try {
            printer.printReceipt(listOf("CVV: 123")).getOrThrow()
            fail("Expected CVV rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        val ok = printer.printReceipt(listOf("VISA ****1111", "APPROVED - SANDBOX"))
        assertTrue(ok.isSuccess)
        assertTrue(printer.lastPrintedLines.any { it.contains("****1111") })
    }
}
