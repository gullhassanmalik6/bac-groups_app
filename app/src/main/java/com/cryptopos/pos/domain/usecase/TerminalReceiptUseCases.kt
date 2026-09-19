package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.receipt.ReceiptContext
import com.cryptopos.pos.domain.receipt.ReceiptCopy
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
    suspend fun fromSession(
        session: TerminalSession,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = factory.fromSession(session, receiptContext(), copy)

    suspend fun fromHistoryItem(
        item: HistoryTransaction,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = factory.fromHistory(item, receiptContext(), copy)

    suspend fun fromHistoryId(
        source: HistorySource,
        id: String,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        return fromHistoryItem(item, copy)
    }

    private suspend fun receiptContext(): ReceiptContext {
        val merchant = runCatching { merchantRepository.getMerchant() }.getOrNull()
        val wallet = runCatching {
            merchantRepository.getWallets().firstOrNull { it.isPrimary }
                ?: merchantRepository.getWallets().firstOrNull()
        }.getOrNull()
        return ReceiptContext(
            merchantName = merchant?.companyName?.takeIf { it.isNotBlank() }
                ?: TerminalReceiptFactory.DEFAULT_MERCHANT,
            merchantEmail = merchant?.email,
            walletAddress = wallet?.address,
            payoutNetwork = wallet?.network?.ifBlank { "TRC20" } ?: "TRC20",
        )
    }
}

class PrintTerminalReceiptUseCase @Inject constructor(
    private val buildReceipt: BuildTerminalReceiptUseCase,
    private val printer: PrinterAdapter,
    private val historyRepository: HistoryRepository,
) {
    suspend fun printSession(
        session: TerminalSession,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): Result<TerminalReceipt> = runCatching {
        val receipt = buildReceipt.fromSession(session, copy)
        printer.connect().getOrThrow()
        printer.printReceipt(receipt.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        receipt.copy(printerName = printer.printerName())
    }

    suspend fun printHistory(
        source: HistorySource,
        id: String,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): Result<TerminalReceipt> = runCatching {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        val receipt = buildReceipt.fromHistoryItem(item, copy)
        printer.connect().getOrThrow()
        printer.printReceipt(receipt.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        receipt.copy(printerName = printer.printerName())
    }

    /** Prints both customer and merchant copies (sandbox honesty). */
    suspend fun printBothCopies(source: HistorySource, id: String): Result<TerminalReceipt> =
        runCatching {
            printHistory(source, id, ReceiptCopy.CUSTOMER).getOrThrow()
            printHistory(source, id, ReceiptCopy.MERCHANT).getOrThrow()
        }
}

class GetTerminalReceiptUseCase @Inject constructor(
    private val buildReceipt: BuildTerminalReceiptUseCase,
) {
    suspend fun fromSession(
        session: TerminalSession,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = buildReceipt.fromSession(session, copy)

    suspend fun fromHistory(
        source: HistorySource,
        id: String,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = buildReceipt.fromHistoryId(source, id, copy)
}
