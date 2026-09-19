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
    fun builds_customer_copy_without_invented_arn_or_iso() {
        val session = TerminalSession(
            id = "TEST-001743",
            state = TerminalTransactionState.COMPLETED,
            amountRaw = "4850.00",
            amountMinor = 485000,
            currency = "USD",
            transactionType = TerminalTransactionType.SALE,
            protocolCode = "201.3",
            protocolDisplayName = "201.3 - Offline 6 DG",
            authorizationCode = "TEST-800040",
            processorReference = "sbx_ref",
            cardBrand = "VISA",
            cardLast4 = "7388",
            environment = "SANDBOX",
        )
        val receipt = factory.fromSession(
            session,
            ReceiptContext(
                merchantName = "Bonyan Advanced Contracting",
                merchantEmail = "info@bacgroupsa.com",
                walletAddress = "TGQbDuBTUw75Uhh5qTuK1NFyQXyTTjDai7",
            ),
            ReceiptCopy.CUSTOMER,
        )
        val text = receipt.toPrintLines().joinToString("\n")
        assertTrue(text.contains("CUSTOMER COPY"))
        assertTrue(text.contains("Bonyan Advanced Contracting".uppercase()) || text.contains("BONYAN"))
        assertTrue(text.contains("201.3"))
        assertTrue(text.contains("**** **** **** 7388"))
        assertTrue(text.contains("VISA"))
        assertTrue(text.contains("USD") && text.contains("4850.00"))
        assertTrue(text.contains("TRC20 (not confirmed)"))
        assertTrue(text.contains("not configured")) // ARN + ISO
        assertFalse(text.contains("ARN6A7784"))
        assertFalse(text.contains("5999 / 00"))
        assertTrue(text.contains("0.5%") && text.contains("merchant fee"))
        assertTrue(text.contains("SANDBOX"))
        assertTrue(text.contains("i••••@bacgroupsa.com") || text.contains("••••"))
        assertFalse(text.contains("cvv", ignoreCase = true))
        assertFalse(Regex("""\d{13,19}""").containsMatchIn(text.replace("7388", "")))
    }

    @Test
    fun merchant_copy_header() {
        val session = TerminalSession(
            id = "m1",
            state = TerminalTransactionState.COMPLETED,
            amountRaw = "10.00",
            currency = "USD",
            cardLast4 = "1111",
            environment = "SANDBOX",
        )
        val text = factory.fromSession(session, copy = ReceiptCopy.MERCHANT)
            .toPrintLines().joinToString("\n")
        assertTrue(text.contains("MERCHANT COPY"))
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
    }
}
