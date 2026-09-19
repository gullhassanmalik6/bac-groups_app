package com.cryptopos.pos.features.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.receipt.ReceiptCopy
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
    val copy: ReceiptCopy = ReceiptCopy.CUSTOMER,
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

    private var lastSource: HistorySource? = null
    private var lastId: String? = null
    private var lastSession: TerminalSession? = null

    fun load(source: HistorySource, id: String) {
        lastSource = source
        lastId = id
        lastSession = null
        viewModelScope.launch {
            val copy = _state.value.copy
            _state.value = TerminalReceiptUiState(loading = true, copy = copy)
            runCatching { getReceipt.fromHistory(source, id, copy) }
                .onSuccess {
                    _state.value = TerminalReceiptUiState(loading = false, receipt = it, copy = copy)
                }
                .onFailure {
                    _state.value = TerminalReceiptUiState(
                        loading = false,
                        copy = copy,
                        message = it.toUserMessage(),
                    )
                }
        }
    }

    fun loadSession(session: TerminalSession) {
        lastSession = session
        lastSource = null
        lastId = null
        viewModelScope.launch {
            val copy = _state.value.copy
            _state.value = TerminalReceiptUiState(loading = true, copy = copy)
            runCatching { getReceipt.fromSession(session, copy) }
                .onSuccess {
                    _state.value = TerminalReceiptUiState(loading = false, receipt = it, copy = copy)
                }
                .onFailure {
                    _state.value = TerminalReceiptUiState(
                        loading = false,
                        copy = copy,
                        message = it.toUserMessage(),
                    )
                }
        }
    }

    fun setCopy(copy: ReceiptCopy) {
        if (_state.value.copy == copy) return
        _state.update { it.copy(copy = copy) }
        lastSession?.let { loadSession(it); return }
        val source = lastSource
        val id = lastId
        if (source != null && id != null) load(source, id)
    }

    fun print(source: HistorySource, id: String) {
        viewModelScope.launch {
            val copy = _state.value.copy
            val result = printReceipt.printHistory(source, id, copy)
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

    fun printBoth(source: HistorySource, id: String) {
        viewModelScope.launch {
            val result = printReceipt.printBothCopies(source, id)
            _state.update {
                it.copy(
                    receipt = result.getOrNull() ?: it.receipt,
                    printerName = result.getOrNull()?.printerName,
                    message = result.exceptionOrNull()?.toUserMessage()
                        ?: "Customer + merchant copies printed",
                )
            }
        }
    }

    fun printSession(session: TerminalSession) {
        viewModelScope.launch {
            val copy = _state.value.copy
            val result = printReceipt.printSession(session, copy)
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
