package com.cryptopos.pos.domain.printer

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory / log printer for emulator and sandbox devices without a thermal head.
 * Never claims to be a production Sunmi/Epson printer.
 */
@Singleton
class MockPrinterAdapter @Inject constructor() : PrinterAdapter {
    @Volatile
    private var connected: Boolean = false

    @Volatile
    var lastPrintedLines: List<String> = emptyList()
        private set

    private val jobLog = mutableListOf<String>()

    fun printedJobs(): List<String> = jobLog.toList()

    fun clearHistory() {
        lastPrintedLines = emptyList()
        jobLog.clear()
    }

    override suspend fun connect(): Result<Unit> {
        connected = true
        return Result.success(Unit)
    }

    override suspend fun printReceipt(lines: List<String>): Result<Unit> {
        ensureConnected()
        requireNoSensitiveCardData(lines)
        lastPrintedLines = lines
        jobLog += lines.joinToString("\n")
        Timber.d("MockPrinterAdapter receipt lines=%d", lines.size)
        return Result.success(Unit)
    }

    override suspend fun printText(text: String): Result<Unit> {
        ensureConnected()
        requireNoSensitiveCardData(listOf(text))
        lastPrintedLines = text.lines()
        jobLog += text
        Timber.d("MockPrinterAdapter text lines=%d", text.lines().size)
        return Result.success(Unit)
    }

    override suspend fun feed(lines: Int): Result<Unit> {
        ensureConnected()
        Timber.d("MockPrinterAdapter feed(%d)", lines)
        return Result.success(Unit)
    }

    override suspend fun cut(): Result<Unit> {
        ensureConnected()
        Timber.d("MockPrinterAdapter cut()")
        return Result.success(Unit)
    }

    override suspend fun getStatus(): PrinterStatus =
        if (connected) PrinterStatus.ONLINE else PrinterStatus.OFFLINE

    override fun printerName(): String = "MockPrinter"

    private fun ensureConnected() {
        if (!connected) {
            connected = true
        }
    }

    companion object {
        private val panLike = Regex("""\b(?:\d[ -]*?){13,19}\b""")
        private val cvvLike = Regex("""(?i)\b(cvv|cvc)\s*[:=]?\s*\d{3,4}\b""")

        fun requireNoSensitiveCardData(lines: List<String>) {
            val blob = lines.joinToString("\n")
            require(!cvvLike.containsMatchIn(blob)) {
                "Receipt must never include CVV/CVC"
            }
            val withoutMasked = blob.replace(Regex("""\*+\d{2,4}"""), "")
            require(!panLike.containsMatchIn(withoutMasked)) {
                "Receipt must never include full PAN"
            }
        }
    }
}
