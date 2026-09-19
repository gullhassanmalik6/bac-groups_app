package com.cryptopos.pos.domain.receipt

import com.cryptopos.pos.domain.fees.FeeCalculator
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.printer.MockPrinterAdapter
import com.cryptopos.pos.domain.terminal.TerminalSession
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptContext(
    val merchantName: String = TerminalReceiptFactory.DEFAULT_MERCHANT,
    val merchantEmail: String? = null,
    val walletAddress: String? = null,
    val payoutNetwork: String = "TRC20",
)

@Singleton
class TerminalReceiptFactory @Inject constructor() {
    private val dateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun fromSession(
        session: TerminalSession,
        context: ReceiptContext = ReceiptContext(),
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt {
        val currency = session.currency ?: "USD"
        val amount = session.amountRaw ?: "0.00"
        val method = listOfNotNull(
            session.cardBrand?.uppercase(),
            session.cardLast4?.let { "****$it" },
        ).joinToString(" ").ifBlank { "TOKENIZED CARD" }

        val receipt = TerminalReceipt(
            merchantName = context.merchantName,
            copy = copy,
            transactionId = session.remoteSessionId ?: session.id,
            amountDisplay = amount,
            currency = currency,
            paymentMethod = method,
            cardBrand = session.cardBrand,
            cardLast4 = session.cardLast4,
            statusLine = sandboxStatus(session.state.name),
            authorizationCode = session.authorizationCode,
            reference = session.processorReference,
            protocol = session.protocolCode ?: session.protocolDisplayName,
            environment = session.environment.ifBlank { "SANDBOX" },
            dateDisplay = dateFmt.format(session.updatedAt),
            connectionMode = sandboxConnection(session.protocolSandboxOutcome),
            maskedEmail = TerminalReceipt.maskEmail(context.merchantEmail),
            payoutNetwork = "${context.payoutNetwork} (not confirmed)",
            maskedWallet = TerminalReceipt.maskWallet(context.walletAddress),
            isoFields = "not configured",
            feeLine = feeDisclaimer(amount, currency),
            qrPayload = TerminalReceipt.sandboxQr(session.remoteSessionId ?: session.id),
        )
        MockPrinterAdapter.requireNoSensitiveCardData(receipt.toPrintLines())
        return receipt
    }

    fun fromHistory(
        item: HistoryTransaction,
        context: ReceiptContext = ReceiptContext(),
        copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    ): TerminalReceipt {
        val method = item.paymentMethod?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                item.cardBrand?.uppercase(),
                item.cardLast4?.let { "****$it" },
            ).joinToString(" ").ifBlank { "TOKENIZED CARD" }

        val date = item.date?.let { raw ->
            runCatching { dateFmt.format(Instant.parse(raw)) }.getOrElse { raw.take(19).replace('T', ' ') }
        } ?: dateFmt.format(Instant.now())

        val receipt = TerminalReceipt(
            merchantName = context.merchantName,
            copy = copy,
            transactionId = item.id,
            amountDisplay = item.amount,
            currency = item.currency,
            paymentMethod = method,
            cardBrand = item.cardBrand,
            cardLast4 = item.cardLast4,
            statusLine = sandboxStatus(item.status),
            authorizationCode = item.authorizationCode,
            reference = item.referenceId,
            protocol = item.protocol,
            environment = item.environment ?: "SANDBOX",
            dateDisplay = date,
            connectionMode = "SANDBOX",
            maskedEmail = TerminalReceipt.maskEmail(context.merchantEmail),
            payoutNetwork = "${context.payoutNetwork} (not confirmed)",
            maskedWallet = TerminalReceipt.maskWallet(context.walletAddress),
            isoFields = "not configured",
            feeLine = feeDisclaimer(item.amount, item.currency),
            qrPayload = TerminalReceipt.sandboxQr(item.id),
        )
        MockPrinterAdapter.requireNoSensitiveCardData(receipt.toPrintLines())
        return receipt
    }

    private fun sandboxStatus(state: String): String {
        val ok = state.equals("COMPLETED", true) ||
            state.equals("APPROVED", true) ||
            state.equals("CAPTURED", true) ||
            state.equals("success", true)
        return if (ok) "00 - Approved (SANDBOX)" else "${state.uppercase()} (SANDBOX)"
    }

    private fun sandboxConnection(outcome: String?): String {
        val offline = outcome?.contains("offline", ignoreCase = true) == true ||
            outcome?.equals("signature", ignoreCase = true) == true
        return if (offline) "Offline (SANDBOX SIM)" else "Online (SANDBOX)"
    }

    private fun feeDisclaimer(amount: String, currency: String): String {
        val gross = amount.toBigDecimalOrNull() ?: return "Fee: n/a"
        val fee = FeeCalculator.calculate(
            gross,
            currency,
            FeeCalculator.SANDBOX_RECEIPT_PERCENT,
            policyVersion = FeeCalculator.SANDBOX_RECEIPT_POLICY,
        )
        return "Please pay ${fee.feePercent.stripTrailingZeros().toPlainString()}% merchant fee " +
            "(${fee.currency} ${fee.feeAmount.toPlainString()}) — SANDBOX DISPLAY ONLY"
    }

    companion object {
        const val DEFAULT_MERCHANT = "Bonyan Advanced Contracting"
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? =
    runCatching { BigDecimal(replace(",", "").trim()) }.getOrNull()
