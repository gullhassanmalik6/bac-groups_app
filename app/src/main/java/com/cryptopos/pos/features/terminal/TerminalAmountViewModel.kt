package com.cryptopos.pos.features.terminal

import androidx.lifecycle.ViewModel
import com.cryptopos.pos.domain.model.TerminalTransactionType
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
    val currency: String = "CAD",
    val transactionType: TerminalTransactionType = TerminalTransactionType.SALE,
    val sessionState: TerminalTransactionState? = null,
    val sessionId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TerminalAmountViewModel @Inject constructor(
    private val sessionManager: TerminalSessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(TerminalAmountUiState())
    val state: StateFlow<TerminalAmountUiState> = _state

    init {
        beginSession()
    }

    fun beginSession() {
        val session = sessionManager.startNewSession()
        _state.update {
            it.copy(
                sessionState = session.state,
                sessionId = session.id,
                error = null,
            )
        }
    }

    fun setCurrency(code: String) {
        _state.update { it.copy(currency = code, error = null) }
    }

    fun setTransactionType(type: TerminalTransactionType) {
        _state.update { it.copy(transactionType = type, error = null) }
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
     * Validates UI amount and advances state machine: CREATED → AMOUNT_ENTERED.
     */
    fun commitAmount(): Boolean {
        val ui = _state.value
        val value = ui.amount.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO) {
            _state.update { it.copy(error = "Enter an amount greater than zero") }
            return false
        }
        // Fresh CREATED session so re-entry after Back always works.
        sessionManager.startNewSession()
        return when (
            val result = sessionManager.enterAmount(ui.amount, ui.currency, ui.transactionType)
        ) {
            is TerminalSessionResult.Ok -> {
                _state.update {
                    it.copy(
                        sessionState = result.session.state,
                        sessionId = result.session.id,
                        error = null,
                    )
                }
                true
            }
            is TerminalSessionResult.Err -> {
                _state.update { it.copy(error = result.message) }
                false
            }
        }
    }
}
