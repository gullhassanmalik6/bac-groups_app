package com.cryptopos.pos.domain.history

import com.cryptopos.pos.domain.model.HistoryTransaction

data class HistoryFilter(
    val query: String = "",
    val status: String? = null,
    val protocol: String? = null,
    val currency: String? = null,
    val amountQuery: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
)

fun HistoryTransaction.matches(filter: HistoryFilter): Boolean {
    val q = filter.query.trim()
    if (q.isNotEmpty()) {
        val haystack = listOfNotNull(
            id,
            referenceId,
            authorizationCode,
            protocol,
            status,
            processor,
            paymentMethod,
        ).joinToString(" ").lowercase()
        if (!haystack.contains(q.lowercase())) return false
    }
    filter.status?.takeIf { it.isNotBlank() }?.let { statusFilter ->
        if (!status.equals(statusFilter, ignoreCase = true) &&
            !status.contains(statusFilter, ignoreCase = true)
        ) {
            return false
        }
    }
    filter.protocol?.takeIf { it.isNotBlank() }?.let { protocolFilter ->
        if (protocol.isNullOrBlank() ||
            !protocol.contains(protocolFilter, ignoreCase = true)
        ) {
            return false
        }
    }
    filter.currency?.takeIf { it.isNotBlank() }?.let { currencyFilter ->
        if (!currency.equals(currencyFilter, ignoreCase = true)) return false
    }
    filter.amountQuery?.takeIf { it.isNotBlank() }?.let { amountFilter ->
        if (!amount.contains(amountFilter)) return false
    }
    filter.fromDate?.takeIf { it.isNotBlank() }?.let { from ->
        if (date == null || date < from) return false
    }
    filter.toDate?.takeIf { it.isNotBlank() }?.let { to ->
        if (date == null || date > "$to\uffff") return false
    }
    return true
}
