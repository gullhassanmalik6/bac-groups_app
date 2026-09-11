package com.cryptopos.pos.domain.printer

/**
 * Hardware printer abstraction. UI never imports Sunmi/Epson SDKs.
 * Real thermal SDKs plug in as additional [PrinterAdapter] implementations.
 */
enum class PrinterStatus {
    ONLINE,
    OFFLINE,
    OUT_OF_PAPER,
    COVER_OPEN,
    ERROR,
    UNKNOWN,
}

interface PrinterAdapter {
    suspend fun connect(): Result<Unit>
    suspend fun printReceipt(lines: List<String>): Result<Unit>
    suspend fun printText(text: String): Result<Unit>
    suspend fun feed(lines: Int = 3): Result<Unit>
    suspend fun cut(): Result<Unit>
    suspend fun getStatus(): PrinterStatus
    fun printerName(): String
}

/**
 * Legacy thin wrapper used by existing payment use-cases.
 */
interface PosPrinter {
    suspend fun printReceipt(lines: List<String>): Result<Unit>
    fun printerName(): String
    suspend fun isAvailable(): Boolean
}
