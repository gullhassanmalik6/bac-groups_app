package com.cryptopos.pos.domain.model

/**
 * Unified history row for terminal sessions and legacy gateway payments.
 * Never includes PAN/CVV.
 */
enum class HistorySource {
    TERMINAL,
    LEGACY,
}

data class HistoryTransaction(
    val id: String,
    val source: HistorySource,
    val date: String?,
    val amount: String,
    val currency: String,
    val protocol: String?,
    val paymentMethod: String?,
    val status: String,
    val processor: String?,
    val environment: String?,
    val referenceId: String?,
    val authorizationCode: String?,
    val transactionType: String?,
    val cardBrand: String?,
    val cardLast4: String?,
    val processorMessage: String?,
    val amountMinor: Long? = null,
) {
    val listKey: String get() = "${source.name}:$id"
}
