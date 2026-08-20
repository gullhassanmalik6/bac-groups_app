package com.cryptopos.pos.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.usecase.GetTransactionUseCase
import com.cryptopos.pos.domain.usecase.ObserveTransactionsUseCase
import com.cryptopos.pos.domain.usecase.PrintReceiptUseCase
import com.cryptopos.pos.domain.usecase.RefreshTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsFilter(
    val query: String = "",
    val status: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
)

data class TransactionsUiState(
    val filter: TransactionsFilter = TransactionsFilter(),
    val items: List<PaymentTransaction> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    observeTransactions: ObserveTransactionsUseCase,
    private val refreshTransactions: RefreshTransactionsUseCase,
) : ViewModel() {
    private val filter = MutableStateFlow(TransactionsFilter())
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsUiState> = combine(
        observeTransactions(),
        filter,
        refreshing,
        error,
    ) { items, currentFilter, isRefreshing, err ->
        TransactionsUiState(
            filter = currentFilter,
            items = items.filter { matches(it, currentFilter) },
            refreshing = isRefreshing,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    init {
        refresh()
    }

    fun onQueryChange(value: String) = filter.update { it.copy(query = value) }
    fun onStatusChange(value: String?) = filter.update { it.copy(status = value) }
    fun onFromDateChange(value: String?) = filter.update { it.copy(fromDate = value) }
    fun onToDateChange(value: String?) = filter.update { it.copy(toDate = value) }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            runCatching { refreshTransactions() }
                .onFailure { error.value = it.toUserMessage() }
            refreshing.value = false
        }
    }

    private fun matches(tx: PaymentTransaction, filter: TransactionsFilter): Boolean {
        val q = filter.query.trim()
        if (q.isNotEmpty()) {
            val haystack = listOfNotNull(
                tx.id, tx.amount, tx.status, tx.merchantReference, tx.receiptNumber, tx.gatewayReference,
            ).joinToString(" ").lowercase()
            if (!haystack.contains(q.lowercase())) return false
        }
        filter.status?.takeIf { it.isNotBlank() }?.let { status ->
            if (!tx.status.equals(status, ignoreCase = true)) return false
        }
        val created = tx.createdAt ?: tx.paymentDate
        filter.fromDate?.takeIf { it.isNotBlank() }?.let { from ->
            if (created == null || created < from) return false
        }
        filter.toDate?.takeIf { it.isNotBlank() }?.let { to ->
            if (created == null || created > to) return false
        }
        return true
    }
}

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val transaction: PaymentTransaction? = null,
    val message: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val getTransaction: GetTransactionUseCase,
    private val printReceipt: PrintReceiptUseCase,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state: StateFlow<TransactionDetailUiState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = TransactionDetailUiState(loading = true)
            runCatching { getTransaction(id) }
                .onSuccess { _state.value = TransactionDetailUiState(loading = false, transaction = it) }
                .onFailure {
                    _state.value = TransactionDetailUiState(loading = false, message = it.toUserMessage())
                }
        }
    }

    fun reprint(id: String) {
        viewModelScope.launch {
            val result = printReceipt(id)
            _state.update {
                it.copy(
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Receipt sent to printer",
                )
            }
        }
    }

    fun refund(id: String) {
        viewModelScope.launch {
            runCatching { paymentRepository.refundPayment(id) }
                .onSuccess { tx ->
                    _state.update { it.copy(transaction = tx, message = "Refund ${tx.status}") }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.toUserMessage()) }
                }
        }
    }
}
