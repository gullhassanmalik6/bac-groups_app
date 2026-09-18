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
    fun builds_screenshot_layout_without_inventing_arn() {
        val session = TerminalSession(
            id = "TEST-001743",
            state = TerminalTransactionState.COMPLETED,
            amountRaw = "1000000.00",
            amountMinor = 100000000,
            currency = "USD",
            transactionType = TerminalTransactionType.SALE,
            protocolCode = "201.3",
            protocolDisplayName = "201.3 - Offline 6 DG",
            protocolSandboxOutcome = "signature",
            authorizationCode = "TEST-800040",
            processorReference = "sbx_deadbeef",
            cardBrand = "VISA",
            cardLast4 = "7388",
            environment = "SANDBOX",
        )
        val receipt = factory.fromSession(
            session,
            ReceiptContext(
                merchantName = "DEMO MERCHANT",
                merchantEmail = "merchant@gmail.com",
                walletAddress = "TTCGyXXXXXXXXXXXXXX2ESi",
            ),
            ReceiptCopy.CUSTOMER,
        )
        val text = receipt.toPrintLines().joinToString("\n")
        assertTrue(text.contains("CUSTOMER COPY"))
        assertTrue(text.contains("SANDBOX — NOT REAL FUNDS"))
        assertTrue(text.contains("201.3"))
        assertTrue(text.contains("•••• •••• •••• 7388"))
        assertTrue(text.contains("VISA"))
        assertTrue(text.contains("1000000.00"))
        assertTrue(text.contains("SANDBOX (no ARN)"))
        assertFalse(text.contains("ARN6A778448C36B105"))
        assertTrue(text.contains("not configured"))
        assertTrue(text.contains("5000.00"))
        assertTrue(text.contains("m••••@gmail.com") || text.contains("••••@"))
        assertTrue(text.contains("TTCGy") && text.contains("2ESi"))
        assertTrue(receipt.qrPayload.startsWith("cryptopos://sandbox-receipt"))
        assertTrue(text.contains("QR REF"))
        assertTrue(text.contains(receipt.qrPayload))
        assertTrue(text.contains("PAYOUT NOT CONFIRMED"))
        assertFalse(text.contains("cvv", ignoreCase = true))
        assertFalse(Regex("""\d{13,19}""").containsMatchIn(text.replace("7388", "")))
        val merchant = receipt.forCopy(ReceiptCopy.MERCHANT).toPrintLines().joinToString("\n")
        assertTrue(merchant.contains("MERCHANT COPY"))
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
