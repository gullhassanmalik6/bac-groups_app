package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.receipt.TerminalReceipt
import com.cryptopos.pos.domain.receipt.TerminalReceiptFactory
import com.cryptopos.pos.domain.repository.HistoryRepository
import com.cryptopos.pos.domain.repository.MerchantRepository
import com.cryptopos.pos.domain.terminal.TerminalSession
import javax.inject.Inject

class BuildTerminalReceiptUseCase @Inject constructor(
    private val factory: TerminalReceiptFactory,
    private val merchantRepository: MerchantRepository,
    private val historyRepository: HistoryRepository,
) {
    suspend fun fromSession(session: TerminalSession): TerminalReceipt =
        factory.fromSession(session, merchantName())

    suspend fun fromHistoryItem(item: HistoryTransaction): TerminalReceipt =
        factory.fromHistory(item, merchantName())

    suspend fun fromHistoryId(source: HistorySource, id: String): TerminalReceipt {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        return fromHistoryItem(item)
    }

    private suspend fun merchantName(): String =
        runCatching { merchantRepository.getMerchant().companyName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: TerminalReceiptFactory.DEFAULT_MERCHANT
}

class PrintTerminalReceiptUseCase @Inject constructor(
    private val buildReceipt: BuildTerminalReceiptUseCase,
    private val printer: PrinterAdapter,
    private val historyRepository: HistoryRepository,
) {
    suspend fun printSession(session: TerminalSession): Result<TerminalReceipt> = runCatching {
        val receipt = buildReceipt.fromSession(session)
        printer.connect().getOrThrow()
        printer.printReceipt(receipt.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        receipt.copy(printerName = printer.printerName())
    }

    suspend fun printHistory(source: HistorySource, id: String): Result<TerminalReceipt> = runCatching {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        val receipt = buildReceipt.fromHistoryItem(item)
        printer.connect().getOrThrow()
        printer.printReceipt(receipt.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        receipt.copy(printerName = printer.printerName())
    }
}

class GetTerminalReceiptUseCase @Inject constructor(
    private val buildReceipt: BuildTerminalReceiptUseCase,
) {
    suspend fun fromSession(session: TerminalSession): TerminalReceipt =
        buildReceipt.fromSession(session)

    suspend fun fromHistory(source: HistorySource, id: String): TerminalReceipt =
        buildReceipt.fromHistoryId(source, id)
}
