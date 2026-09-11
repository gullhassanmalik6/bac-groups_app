package com.cryptopos.pos.domain.receipt

import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.printer.MockPrinterAdapter
import com.cryptopos.pos.domain.terminal.TerminalSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thermal-style receipt document for terminal (and history) sessions.
 * Never includes PAN, CVV, track, or PIN.
 */
data class TerminalReceipt(
    val merchantName: String,
    val transactionId: String,
    val amountDisplay: String,
    val currency: String,
    val paymentMethod: String,
    val statusLine: String,
    val authorizationCode: String?,
    val reference: String?,
    val protocol: String?,
    val environment: String,
    val dateDisplay: String,
    val printerName: String? = null,
) {
    fun toPrintLines(): List<String> = buildList {
        add(SEPARATOR)
        add(center(merchantName.uppercase()))
        add(SEPARATOR)
        add("")
        add("Transaction:")
        add(transactionId)
        add("")
        add("Total:")
        add(amountDisplay)
        add("")
        add("Payment:")
        add(paymentMethod)
        add("")
        add("Status:")
        add(statusLine)
        if (!authorizationCode.isNullOrBlank()) {
            add("")
            add("Authorization:")
            add(authorizationCode)
        }
        if (!reference.isNullOrBlank()) {
            add("")
            add("Reference:")
            add(reference)
        }
        if (!protocol.isNullOrBlank()) {
            add("")
            add("Protocol:")
            add(protocol)
        }
        add("")
        add("Date:")
        add(dateDisplay)
        add("")
        add(SEPARATOR)
        add(center("${environment.uppercase()} TRANSACTION"))
        add(SEPARATOR)
    }

    companion object {
        const val SEPARATOR = "--------------------------------"
        private const val WIDTH = 32

        fun center(text: String): String {
            if (text.length >= WIDTH) return text.take(WIDTH)
            val pad = (WIDTH - text.length) / 2
            return " ".repeat(pad) + text
        }
    }
}

@Singleton
class TerminalReceiptFactory @Inject constructor() {
    private val dateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    fun fromSession(
        session: TerminalSession,
        merchantName: String = DEFAULT_MERCHANT,
    ): TerminalReceipt {
        val currency = session.currency ?: "CAD"
        val amount = session.amountRaw ?: "0.00"
        val method = listOfNotNull(
            session.cardBrand?.uppercase()?.let { "TEST $it" },
            session.cardLast4?.let { "****$it" },
        ).joinToString(" ").ifBlank { "TEST TOKENIZED CARD" }

        val status = when {
            session.state.name in SUCCESS_STATES -> "APPROVED - SANDBOX"
            else -> "${session.state.name} - SANDBOX"
        }

        return TerminalReceipt(
            merchantName = merchantName,
            transactionId = session.remoteSessionId ?: session.id,
            amountDisplay = formatMoney(amount, currency),
            currency = currency,
            paymentMethod = method,
            statusLine = status,
            authorizationCode = session.authorizationCode,
            reference = session.processorReference,
            protocol = session.protocolDisplayName ?: session.protocolCode,
            environment = session.environment.ifBlank { "SANDBOX" },
            dateDisplay = dateFmt.format(session.updatedAt),
        ).also { MockPrinterAdapter.requireNoSensitiveCardData(it.toPrintLines()) }
    }

    fun fromHistory(
        item: HistoryTransaction,
        merchantName: String = DEFAULT_MERCHANT,
    ): TerminalReceipt {
        val method = item.paymentMethod?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                item.cardBrand?.uppercase()?.let { "TEST $it" },
                item.cardLast4?.let { "****$it" },
            ).joinToString(" ").ifBlank { "TEST TOKENIZED CARD" }

        val status = when {
            item.status.equals("COMPLETED", true) ||
                item.status.equals("APPROVED", true) ||
                item.status.equals("CAPTURED", true) ||
                item.status.equals("success", true) -> "APPROVED - SANDBOX"
            else -> "${item.status.uppercase()} - SANDBOX"
        }

        val date = item.date?.let { raw ->
            runCatching { dateFmt.format(Instant.parse(raw)) }.getOrElse { raw.take(16).replace('T', ' ') }
        } ?: dateFmt.format(Instant.now())

        return TerminalReceipt(
            merchantName = merchantName,
            transactionId = item.id,
            amountDisplay = formatMoney(item.amount, item.currency),
            currency = item.currency,
            paymentMethod = method,
            statusLine = status,
            authorizationCode = item.authorizationCode,
            reference = item.referenceId,
            protocol = item.protocol,
            environment = item.environment ?: "SANDBOX",
            dateDisplay = date,
        ).also { MockPrinterAdapter.requireNoSensitiveCardData(it.toPrintLines()) }
    }

    private fun formatMoney(amount: String, currency: String): String {
        val symbol = when (currency.uppercase()) {
            "CAD" -> "CA$"
            "USD" -> "US$"
            "AED" -> "AED "
            "SAR" -> "SAR "
            else -> "$currency "
        }
        return "$symbol$amount"
    }

    companion object {
        const val DEFAULT_MERCHANT = "DEMO MERCHANT"
        private val SUCCESS_STATES = setOf("APPROVED", "CAPTURED", "COMPLETED")
    }
}
