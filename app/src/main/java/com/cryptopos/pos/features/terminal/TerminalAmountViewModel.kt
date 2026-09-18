package com.cryptopos.pos.features.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cryptopos.pos.domain.model.ProtocolProfile
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import com.cryptopos.pos.domain.terminal.TerminalSessionManager
import com.cryptopos.pos.domain.terminal.TerminalSessionResult
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

data class TerminalAmountUiState(
    val amount: String = "",
    val currency: String = "USD",
    val transactionType: TerminalTransactionType = TerminalTransactionType.SALE,
    val protocolId: String? = null,
    val protocol: ProtocolProfile? = null,
    val sessionState: TerminalTransactionState? = null,
    val sessionId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TerminalAmountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: TerminalSessionManager,
    private val protocolCatalog: ProtocolProfileCatalog,
) : ViewModel() {
    private val protocolId: String = savedStateHandle.get<String>("protocolId").orEmpty()

    private val _state = MutableStateFlow(
        TerminalAmountUiState(
            protocolId = protocolId.ifBlank { null },
            protocol = protocolId.takeIf { it.isNotBlank() }?.let { protocolCatalog.get(it) },
        ),
    )
    val state: StateFlow<TerminalAmountUiState> = _state

    fun setCurrency(code: String) {
        _state.update { it.copy(currency = code, error = null) }
    }

    fun setTransactionType(type: TerminalTransactionType) {
        _state.update { it.copy(transactionType = type, error = null) }
    }

    fun setQuickAmount(value: String) {
        _state.update { it.copy(amount = value, error = null) }
    }

    fun appendDigit(digit: String) {
        val current = _state.value.amount
        if (digit == "." && current.contains('.')) return
        val next = when {
            digit == "." && current.isEmpty() -> "0."
            else -> current + digit
        }
        if (!next.matches(Regex("^\\d*\\.?\\d{0,2}$"))) return
        if (next.replace(".", "").length > 9) return
        _state.update { it.copy(amount = next, error = null) }
    }

    fun backspace() {
        _state.update { it.copy(amount = it.amount.dropLast(1), error = null) }
    }

    fun clearAmount() {
        _state.update { it.copy(amount = "", error = null) }
    }

    /**
     * CREATED → AMOUNT_ENTERED → PROTOCOL_SELECTED (mockup order is protocol→amount in UI).
     */
    fun commitAmount(): Boolean {
        val ui = _state.value
        val value = ui.amount.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO) {
            _state.update { it.copy(error = "Enter an amount greater than zero") }
            return false
        }
        val protocolKey = ui.protocolId
        if (protocolKey.isNullOrBlank()) {
            _state.update { it.copy(error = "Select a transaction protocol") }
            return false
        }
        sessionManager.startNewSession()
        when (val amountResult = sessionManager.enterAmount(ui.amount, ui.currency, ui.transactionType)) {
            is TerminalSessionResult.Err -> {
                _state.update { it.copy(error = amountResult.message) }
                return false
            }
            is TerminalSessionResult.Ok -> Unit
        }
        return when (val protocolResult = sessionManager.selectProtocol(protocolKey)) {
            is TerminalSessionResult.Ok -> {
                _state.update {
                    it.copy(
                        sessionState = protocolResult.session.state,
                        sessionId = protocolResult.session.id,
                        error = null,
                    )
                }
                true
            }
            is TerminalSessionResult.Err -> {
                _state.update { it.copy(error = protocolResult.message) }
                false
            }
        }
    }
}
