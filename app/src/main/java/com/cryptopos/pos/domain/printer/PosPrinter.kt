package com.cryptopos.pos.domain.printer

/**
 * Printer abstraction. UI and domain never reference Sunmi/Epson/Star SDKs directly.
 */
interface PosPrinter {
    suspend fun printReceipt(lines: List<String>): Result<Unit>
    fun printerName(): String
    suspend fun isAvailable(): Boolean
}
