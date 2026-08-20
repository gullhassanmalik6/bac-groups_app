package com.cryptopos.pos.features.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.Receipt
import com.cryptopos.pos.domain.usecase.GetReceiptUseCase
import com.cryptopos.pos.domain.usecase.PrintReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptUiState(
    val loading: Boolean = true,
    val receipt: Receipt? = null,
    val message: String? = null,
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val getReceipt: GetReceiptUseCase,
    private val printReceipt: PrintReceiptUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state

    fun load(transactionId: String) {
        viewModelScope.launch {
            _state.value = ReceiptUiState(loading = true)
            runCatching { getReceipt(transactionId) }
                .onSuccess { _state.value = ReceiptUiState(loading = false, receipt = it) }
                .onFailure {
                    _state.value = ReceiptUiState(loading = false, message = it.toUserMessage())
                }
        }
    }

    fun print(transactionId: String) {
        viewModelScope.launch {
            val result = printReceipt(transactionId)
            _state.update {
                it.copy(
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Receipt printed",
                )
            }
        }
    }
}
