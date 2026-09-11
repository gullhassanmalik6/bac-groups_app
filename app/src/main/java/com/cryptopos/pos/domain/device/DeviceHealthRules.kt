package com.cryptopos.pos.domain.device

import com.cryptopos.pos.domain.payment.CardReader
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.printer.PrinterStatus
import javax.inject.Inject

object DeviceHealthRules {
    fun peripheralFromPrinter(status: PrinterStatus): PeripheralStatus = when (status) {
        PrinterStatus.ONLINE -> PeripheralStatus.ONLINE
        PrinterStatus.OUT_OF_PAPER, PrinterStatus.COVER_OPEN -> PeripheralStatus.WARNING
        PrinterStatus.ERROR -> PeripheralStatus.WARNING
        PrinterStatus.OFFLINE, PrinterStatus.UNKNOWN -> PeripheralStatus.OFFLINE
    }

    fun peripheralFromNfc(available: Boolean): PeripheralStatus =
        if (available) PeripheralStatus.ONLINE else PeripheralStatus.UNAVAILABLE

    fun connectivity(online: Boolean): DeviceHealthStatus =
        if (online) DeviceHealthStatus.ONLINE else DeviceHealthStatus.OFFLINE

    fun overall(
        connectivity: DeviceHealthStatus,
        printer: PeripheralStatus,
        cardReader: PeripheralStatus,
        integrityCompromised: Boolean,
        extraWarnings: List<String> = emptyList(),
    ): Pair<DeviceHealthStatus, List<String>> {
        val warnings = extraWarnings.toMutableList()
        if (integrityCompromised) warnings += "integrity-warning"
        if (printer == PeripheralStatus.WARNING) warnings += "printer-warning"
        if (printer == PeripheralStatus.OFFLINE) warnings += "printer-offline"
        if (cardReader == PeripheralStatus.UNAVAILABLE) warnings += "card-reader-unavailable"
        if (connectivity == DeviceHealthStatus.OFFLINE) {
            return DeviceHealthStatus.OFFLINE to warnings
        }
        if (warnings.isNotEmpty() ||
            printer == PeripheralStatus.WARNING ||
            cardReader == PeripheralStatus.WARNING
        ) {
            return DeviceHealthStatus.WARNING to warnings
        }
        return DeviceHealthStatus.ONLINE to warnings
    }
}

/** Shared helpers for adapter implementations. */
class DevicePeripheralProbe @Inject constructor(
    private val printer: PrinterAdapter,
    private val cardReader: CardReader,
) {
    suspend fun printerStatus(): PeripheralStatus {
        runCatching { printer.connect() }
        return DeviceHealthRules.peripheralFromPrinter(printer.getStatus())
    }

    fun cardReaderStatus(): PeripheralStatus =
        DeviceHealthRules.peripheralFromNfc(cardReader.isNfcAvailable())
}
