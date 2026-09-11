package com.cryptopos.pos.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.history.HistoryFilter
import com.cryptopos.pos.domain.history.matches
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.repository.HistoryRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.usecase.PrintReceiptUseCase
import com.cryptopos.pos.domain.usecase.PrintTerminalReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val filter: HistoryFilter = HistoryFilter(),
    val items: List<HistoryTransaction> = emptyList(),
    val protocols: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter())
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsUiState> = combine(
        historyRepository.observeHistory(),
        filter,
        refreshing,
        error,
    ) { items, currentFilter, isRefreshing, err ->
        TransactionsUiState(
            filter = currentFilter,
            items = items.filter { it.matches(currentFilter) },
            protocols = items.mapNotNull { it.protocol }.distinct().sorted(),
            currencies = items.map { it.currency }.distinct().sorted(),
            refreshing = isRefreshing,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    init {
        refresh()
    }

    fun onQueryChange(value: String) = filter.update { it.copy(query = value) }
    fun onStatusChange(value: String?) = filter.update { it.copy(status = value) }
    fun onProtocolChange(value: String?) = filter.update { it.copy(protocol = value) }
    fun onCurrencyChange(value: String?) = filter.update { it.copy(currency = value) }
    fun onAmountChange(value: String?) = filter.update { it.copy(amountQuery = value) }
    fun onFromDateChange(value: String?) = filter.update { it.copy(fromDate = value) }
    fun onToDateChange(value: String?) = filter.update { it.copy(toDate = value) }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            runCatching { historyRepository.refresh() }
                .onFailure { error.value = it.toUserMessage() }
            refreshing.value = false
        }
    }
}

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val item: HistoryTransaction? = null,
    val message: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val printReceipt: PrintReceiptUseCase,
    private val printTerminalReceipt: PrintTerminalReceiptUseCase,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state: StateFlow<TransactionDetailUiState> = _state

    fun load(source: HistorySource, id: String) {
        viewModelScope.launch {
            _state.value = TransactionDetailUiState(loading = true)
            runCatching { historyRepository.get(source, id) }
                .onSuccess { item ->
                    _state.value = TransactionDetailUiState(
                        loading = false,
                        item = item,
                        message = if (item == null) "Not found" else null,
                    )
                }
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

    fun printTerminal(source: HistorySource, id: String) {
        viewModelScope.launch {
            val result = printTerminalReceipt.printHistory(source, id)
            _state.update {
                it.copy(
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Receipt printed (${result.getOrNull()?.printerName ?: "printer"})",
                )
            }
        }
    }

    fun refund(id: String) {
        viewModelScope.launch {
            runCatching { paymentRepository.refundPayment(id) }
                .onSuccess { tx ->
                    _state.update {
                        it.copy(
                            message = "Refund ${tx.status}",
                            item = it.item?.copy(status = tx.status),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.toUserMessage()) }
                }
        }
    }
}
