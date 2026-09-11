package com.cryptopos.pos.features.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.receipt.TerminalReceipt
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.usecase.GetTerminalReceiptUseCase
import com.cryptopos.pos.domain.usecase.PrintTerminalReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalReceiptUiState(
    val loading: Boolean = true,
    val receipt: TerminalReceipt? = null,
    val printerName: String? = null,
    val message: String? = null,
)

@HiltViewModel
class TerminalReceiptViewModel @Inject constructor(
    private val getReceipt: GetTerminalReceiptUseCase,
    private val printReceipt: PrintTerminalReceiptUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TerminalReceiptUiState())
    val state: StateFlow<TerminalReceiptUiState> = _state

    fun load(source: HistorySource, id: String) {
        viewModelScope.launch {
            _state.value = TerminalReceiptUiState(loading = true)
            runCatching { getReceipt.fromHistory(source, id) }
                .onSuccess { _state.value = TerminalReceiptUiState(loading = false, receipt = it) }
                .onFailure {
                    _state.value = TerminalReceiptUiState(loading = false, message = it.toUserMessage())
                }
        }
    }

    fun loadSession(session: TerminalSession) {
        viewModelScope.launch {
            _state.value = TerminalReceiptUiState(loading = true)
            runCatching { getReceipt.fromSession(session) }
                .onSuccess { _state.value = TerminalReceiptUiState(loading = false, receipt = it) }
                .onFailure {
                    _state.value = TerminalReceiptUiState(loading = false, message = it.toUserMessage())
                }
        }
    }

    fun print(source: HistorySource, id: String) {
        viewModelScope.launch {
            val result = printReceipt.printHistory(source, id)
            _state.update {
                it.copy(
                    receipt = result.getOrNull() ?: it.receipt,
                    printerName = result.getOrNull()?.printerName,
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Receipt printed (${result.getOrNull()?.printerName ?: "printer"})",
                )
            }
        }
    }

    fun printSession(session: TerminalSession) {
        viewModelScope.launch {
            val result = printReceipt.printSession(session)
            _state.update {
                it.copy(
                    receipt = result.getOrNull() ?: it.receipt,
                    printerName = result.getOrNull()?.printerName,
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Receipt printed (${result.getOrNull()?.printerName ?: "printer"})",
                )
            }
        }
    }
}
