package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.core.network.ConnectivityObserver
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.model.Receipt
import com.cryptopos.pos.domain.payment.CardReader
import com.cryptopos.pos.domain.payment.ChargeRequest
import com.cryptopos.pos.domain.payment.PaymentGatewayResolver
import com.cryptopos.pos.domain.printer.PosPrinter
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.repository.SettingsRepository
import java.math.BigDecimal
import javax.inject.Inject

class ValidatePaymentAmountUseCase @Inject constructor() {
    operator fun invoke(raw: String): BigDecimal {
        val amount = raw.toBigDecimalOrNull()
            ?: throw PosError.Validation("Enter a valid amount.")
        if (amount <= BigDecimal.ZERO) {
            throw PosError.Validation("Amount must be greater than zero.")
        }
        if (amount.scale() > 2) {
            throw PosError.Validation("Amount supports up to 2 decimal places.")
        }
        return amount.setScale(2)
    }
}

class ProcessPaymentUseCase @Inject constructor(
    private val validateAmount: ValidatePaymentAmountUseCase,
    private val cardReader: CardReader,
    private val gatewayResolver: PaymentGatewayResolver,
    private val paymentRepository: PaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val printer: PosPrinter,
) {
    data class Result(
        val transaction: PaymentTransaction,
        val receipt: Receipt?,
        val printed: Boolean,
        val queuedOffline: Boolean,
    )

    suspend operator fun invoke(
        amountRaw: String,
        currency: String,
        description: String? = "POS sale",
        waitForCard: Boolean = true,
    ): Result {
        val amount = validateAmount(amountRaw)
        val settings = settingsRepository.current()
        val provider = settings.gatewayProvider

        val tap = if (waitForCard) {
            cardReader.waitForCardTap().getOrElse { throw it }
        } else {
            null
        }

        if (!connectivityObserver.currentlyOnline()) {
            val queued = paymentRepository.enqueueOfflinePayment(
                amount = amount,
                currency = currency,
                description = description,
                gatewayProvider = provider,
            )
            return Result(transaction = queued, receipt = null, printed = false, queuedOffline = true)
        }

        val gateway = gatewayResolver.resolve(provider)
        val transaction = gateway.charge(
            ChargeRequest(
                amount = amount,
                currency = currency,
                description = description,
                cardTap = tap,
            ),
        )
        val receipt = runCatching { paymentRepository.getReceipt(transaction.id) }.getOrNull()
        val printed = if (receipt != null) {
            printer.printReceipt(receipt.toPrintLines()).isSuccess
        } else {
            false
        }
        return Result(transaction = transaction, receipt = receipt, printed = printed, queuedOffline = false)
    }
}

class GetReceiptUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(transactionId: String): Receipt =
        paymentRepository.getReceipt(transactionId)
}

class PrintReceiptUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val printer: PosPrinter,
) {
    suspend operator fun invoke(transactionId: String): Result<Unit> {
        val receipt = paymentRepository.getReceipt(transactionId)
        return printer.printReceipt(receipt.toPrintLines())
    }
}

fun Receipt.toPrintLines(): List<String> = buildList {
    add(merchantName)
    add("Receipt $receiptNumber")
    add("$amount $currency")
    add("Status $status")
    add("Gateway ${gateway ?: "-"}")
    transactionId?.let { add("Txn $it") }
    createdAt?.let { add(it) }
    lines.forEach { (key, value) ->
        if (!value.isNullOrBlank()) add("$key: $value")
    }
}
