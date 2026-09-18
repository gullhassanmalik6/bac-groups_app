package com.cryptopos.pos.domain.receipt

enum class ReceiptCopy {
    CUSTOMER,
    MERCHANT,
}

/**
 * Thermal receipt for sandbox terminal sessions.
 *
 * Layout follows client screenshots. Values that a licensed acquirer would
 * supply (ARN, ISO DE18/DE25, payout confirmation) are **not invented**.
 */
data class TerminalReceipt(
    val merchantName: String,
    val copy: ReceiptCopy,
    val transactionId: String,
    val amountDisplay: String,
    val currency: String,
    val paymentMethod: String,
    val cardBrand: String?,
    val cardLast4: String?,
    val statusLine: String,
    val authorizationCode: String?,
    val reference: String?,
    val protocol: String?,
    val environment: String,
    val dateDisplay: String,
    val connectionMode: String,
    val maskedEmail: String,
    val payoutNetwork: String,
    val maskedWallet: String,
    val isoFields: String,
    val feeLine: String,
    val qrPayload: String,
    val printerName: String? = null,
) {
    fun forCopy(copy: ReceiptCopy): TerminalReceipt = copy(copy = copy)

    fun toPrintLines(): List<String> = buildList {
        add(SEPARATOR)
        add(center(merchantName.uppercase()))
        add(center(copyBanner()))
        add(SEPARATOR)
        add(center("SANDBOX — NOT REAL FUNDS"))
        add("")
        add(kv("DATE", dateDisplay))
        add(kv("TXN ID", transactionId.take(18)))
        add(kv("ARN", arnDisplay()))
        add(kv("TERMINAL", "POS"))
        add(kv("CONNECTION", connectionMode))
        add(kv("EMAIL", maskedEmail))
        add(kv("PROTOCOL", protocol.orEmpty().ifBlank { "—" }))
        add(kv("CARD", maskedCard()))
        add(kv("CARD TYPE", cardBrand?.uppercase() ?: "—"))
        add(kv("AMOUNT", "$currency $amountDisplay".trim()))
        add(kv("PAYOUT", payoutNetwork))
        add(kv("WALLET", maskedWallet))
        add(kv("AUTH CODE", authDisplay()))
        add(kv("ISO 18 / 25", isoFields))
        add(kv("STATUS", statusLine))
        add("")
        add(center("Signature"))
        add("")
        add(feeLine)
        add("")
        add("QR REF")
        add(qrPayload)
        add("")
        add(SEPARATOR)
        add(center("NOT SETTLED"))
        add(center("PAYOUT NOT CONFIRMED"))
        add(center("${environment.uppercase()} TRANSACTION"))
        add(SEPARATOR)
    }

    private fun copyBanner(): String =
        if (copy == ReceiptCopy.CUSTOMER) "CUSTOMER COPY" else "MERCHANT COPY"

    private fun arnDisplay(): String {
        val ref = reference?.trim().orEmpty()
        return if (ref.startsWith("sbx_") || ref.startsWith("TEST") || ref.isBlank()) {
            "SANDBOX (no ARN)"
        } else {
            ref.take(18)
        }
    }

    private fun authDisplay(): String {
        val code = authorizationCode?.trim().orEmpty()
        return if (code.isBlank()) "SANDBOX (none)" else code.take(16)
    }

    private fun maskedCard(): String {
        val last4 = cardLast4?.filter { it.isDigit() }.orEmpty()
        return if (last4.length == 4) "•••• •••• •••• $last4" else paymentMethod
    }

    companion object {
        const val SEPARATOR = "--------------------------------"
        private const val WIDTH = 32

        fun center(text: String): String {
            if (text.length >= WIDTH) return text.take(WIDTH)
            val pad = (WIDTH - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun kv(label: String, value: String): String {
            val left = label.uppercase()
            val right = value.ifBlank { "—" }
            val spaces = (WIDTH - left.length - right.length).coerceAtLeast(1)
            val line = left + " ".repeat(spaces) + right
            return if (line.length <= WIDTH) line else (left + " " + right).take(WIDTH)
        }

        fun maskEmail(email: String?): String {
            val value = email?.trim().orEmpty()
            val at = value.indexOf('@')
            if (at <= 0 || at == value.lastIndex) return "••••@••••"
            return value.first() + "••••@" + value.substring(at + 1)
        }

        fun maskWallet(address: String?): String {
            val value = address?.trim().orEmpty()
            if (value.length < 10) return "not configured"
            return value.take(5) + "••••" + value.takeLast(4)
        }

        fun sandboxQr(transactionId: String): String =
            "cryptopos://sandbox-receipt/$transactionId"
    }
}
