package com.cryptopos.pos.domain.printer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MockPrinterAdapterTest {
    @Test
    fun rejects_full_pan() = runBlocking {
        val printer = MockPrinterAdapter()
        try {
            printer.printReceipt(listOf("Card 4111111111111111")).getOrThrow()
            fail("Expected PAN rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun rejects_cvv() = runBlocking {
        val printer = MockPrinterAdapter()
        try {
            printer.printText("CVV: 123").getOrThrow()
            fail("Expected CVV rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun allows_masked_pan_and_tracks_jobs() = runBlocking {
        val printer = MockPrinterAdapter()
        printer.connect()
        val result = printer.printReceipt(listOf("VISA ****1111", "APPROVED - SANDBOX"))
        assertTrue(result.isSuccess)
        assertEquals(1, printer.printedJobs().size)
        assertTrue(printer.lastPrintedLines.any { it.contains("****1111") })
        assertEquals(
            com.cryptopos.pos.domain.printer.PrinterStatus.ONLINE,
            printer.getStatus(),
        )
    }
}
