package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.receipt.ReceiptContext
import com.cryptopos.pos.domain.receipt.ReceiptCopy
import com.cryptopos.pos.domain.receipt.TerminalReceipt
import com.cryptopos.pos.domain.receipt.TerminalReceiptFactory
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.repository.HistoryRepository
import com.cryptopos.pos.domain.repository.MerchantRepository
import com.cryptopos.pos.domain.terminal.TerminalSession
import javax.inject.Inject

class BuildTerminalReceiptUseCase @Inject constructor(
    private val factory: TerminalReceiptFactory,
    private val merchantRepository: MerchantRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
) {
    suspend fun fromSession(
        session: TerminalSession,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = factory.fromSession(session, receiptContext(), copy)

    suspend fun fromHistoryItem(
        item: HistoryTransaction,
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt = factory.fromHistory(item, receiptContext(), copy)

    suspend fun fromHistoryId(source: HistorySource, id: String): TerminalReceipt {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        return fromHistoryItem(item)
    }

    private suspend fun receiptContext(): ReceiptContext {
        val merchant = runCatching { merchantRepository.getMerchant() }.getOrNull()
        val wallet = runCatching {
            merchantRepository.getWallets().firstOrNull { it.isPrimary }
                ?: merchantRepository.getWallets().firstOrNull()
        }.getOrNull()
        val email = merchant?.email?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUserEmail()
        return ReceiptContext(
            merchantName = merchant?.companyName?.takeIf { it.isNotBlank() }
                ?: TerminalReceiptFactory.DEFAULT_MERCHANT,
            merchantEmail = email,
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
    suspend fun printSession(session: TerminalSession): Result<TerminalReceipt> = runCatching {
        printer.connect().getOrThrow()
        val customer = buildReceipt.fromSession(session, ReceiptCopy.CUSTOMER)
        printer.printReceipt(customer.toPrintLines()).getOrThrow()
        printer.feed()
        val merchant = buildReceipt.fromSession(session, ReceiptCopy.MERCHANT)
        printer.printReceipt(merchant.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        merchant.copy(printerName = printer.printerName())
    }

    suspend fun printHistory(source: HistorySource, id: String): Result<TerminalReceipt> = runCatching {
        val item = historyRepository.get(source, id) ?: error("Transaction not found")
        printer.connect().getOrThrow()
        val customer = buildReceipt.fromHistoryItem(item, ReceiptCopy.CUSTOMER)
        printer.printReceipt(customer.toPrintLines()).getOrThrow()
        printer.feed()
        val merchant = buildReceipt.fromHistoryItem(item, ReceiptCopy.MERCHANT)
        printer.printReceipt(merchant.toPrintLines()).getOrThrow()
        printer.feed()
        runCatching { printer.cut() }
        merchant.copy(printerName = printer.printerName())
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
