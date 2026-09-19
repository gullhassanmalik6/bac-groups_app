package com.cryptopos.pos.domain.receipt

/**
 * Thermal / on-screen receipt. Never includes PAN, CVV, PIN, track, or private keys.
 * ARN / ISO network fields stay "not configured" until a licensed provider supplies them.
 */
enum class ReceiptCopy {
    CUSTOMER,
    MERCHANT,
}

data class TerminalReceipt(
    val merchantName: String,
    val copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
    val transactionId: String,
    val amountDisplay: String,
    val currency: String,
    val paymentMethod: String,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val statusLine: String,
    val authorizationCode: String?,
    val reference: String?,
    val protocol: String?,
    val environment: String,
    val dateDisplay: String,
    val connectionMode: String = "SANDBOX",
    val maskedEmail: String? = null,
    val payoutNetwork: String? = null,
    val maskedWallet: String? = null,
    /** Never invent DE18/DE25 — provider schema only. */
    val isoFields: String = "not configured",
    val feeLine: String? = null,
    val qrPayload: String? = null,
    val printerName: String? = null,
    val terminalLabel: String = "POS",
) {
    fun toPrintLines(): List<String> = buildList {
        add(SEPARATOR)
        add(center(copyLabel()))
        add(center(merchantName.uppercase()))
        add(SEPARATOR)
        add(kv("DATE", dateDisplay))
        add(kv("TXN ID", transactionId))
        add(kv("ARN", "not configured"))
        add(kv("TERMINAL", terminalLabel))
        add(kv("CONNECTION", connectionMode))
        if (!maskedEmail.isNullOrBlank()) add(kv("EMAIL", maskedEmail))
        if (!protocol.isNullOrBlank()) add(kv("PROTOCOL", protocol))
        add(kv("CARD", maskedCard()))
        if (!cardBrand.isNullOrBlank()) add(kv("CARD TYPE", cardBrand.uppercase()))
        add("")
        add(kv("AMOUNT", "${currency.uppercase()} $amountDisplay"))
        if (!payoutNetwork.isNullOrBlank()) add(kv("PAYOUT", payoutNetwork))
        if (!maskedWallet.isNullOrBlank()) add(kv("WALLET", maskedWallet))
        add(kv("AUTH CODE", authorizationCode?.takeIf { it.isNotBlank() } ?: "not configured"))
        if (!reference.isNullOrBlank()) add(kv("REFERENCE", reference))
        add(kv("ISO 18 / 25", isoFields))
        add(kv("STATUS", statusLine))
        add("")
        add(center("Signature"))
        add("")
        feeLine?.takeIf { it.isNotBlank() }?.let {
            add(it)
            add("")
        }
        if (!qrPayload.isNullOrBlank()) {
            add(center("QR (sandbox receipt ref)"))
            add(center(qrPayload.take(32)))
            add("")
        }
        add(SEPARATOR)
        add(center("${environment.uppercase()} — NO REAL FUNDS"))
        add(SEPARATOR)
    }

    private fun copyLabel(): String = when (copy) {
        ReceiptCopy.CUSTOMER -> "CUSTOMER COPY"
        ReceiptCopy.MERCHANT -> "MERCHANT COPY"
    }

    private fun maskedCard(): String =
        cardLast4?.let { "**** **** **** $it" }
            ?: paymentMethod.ifBlank { "TOKENIZED" }

    companion object {
        const val SEPARATOR = "--------------------------------"
        private const val WIDTH = 32

        fun center(text: String): String {
            if (text.length >= WIDTH) return text.take(WIDTH)
            val pad = (WIDTH - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun kv(label: String, value: String): String {
            val left = "$label:"
            val space = (WIDTH - left.length - value.length).coerceAtLeast(1)
            val line = left + " ".repeat(space) + value
            return if (line.length <= WIDTH) line else "$left\n$value"
        }

        fun maskEmail(email: String?): String? {
            if (email.isNullOrBlank()) return null
            val at = email.indexOf('@')
            if (at <= 0) return "••••@••••"
            val user = email.substring(0, at)
            val domain = email.substring(at)
            val maskedUser = when {
                user.length == 1 -> "${user[0]}••••"
                else -> "${user[0]}••••"
            }
            return maskedUser + domain
        }

        fun maskWallet(address: String?): String? {
            if (address.isNullOrBlank()) return null
            if (address.length < 10) return "••••"
            return address.take(5) + "••••" + address.takeLast(4)
        }

        fun sandboxQr(transactionId: String): String =
            "sbx-receipt:$transactionId"
    }
}
