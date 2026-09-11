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
import javax.inject.Inject

data class ProtocolSelectionUiState(
    val amount: String = "",
    val currency: String = "CAD",
    val transactionType: TerminalTransactionType = TerminalTransactionType.SALE,
    val protocols: List<ProtocolProfile> = emptyList(),
    val selectedId: String? = null,
    val selectedProfile: ProtocolProfile? = null,
    val sessionState: TerminalTransactionState? = null,
    val sessionId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ProtocolSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: TerminalSessionManager,
    private val protocolCatalog: ProtocolProfileCatalog,
) : ViewModel() {
    private val amount: String = savedStateHandle.get<String>("amount").orEmpty()
    private val currency: String = savedStateHandle.get<String>("currency") ?: "CAD"
    private val txnTypeRaw: String = savedStateHandle.get<String>("txnType") ?: "SALE"
    private val txnType = runCatching { TerminalTransactionType.valueOf(txnTypeRaw) }
        .getOrDefault(TerminalTransactionType.SALE)

    private val _state = MutableStateFlow(
        ProtocolSelectionUiState(
            amount = amount,
            currency = currency,
            transactionType = txnType,
            protocols = protocolCatalog.forTransactionType(txnType),
            sessionState = sessionManager.current()?.state,
            sessionId = sessionManager.current()?.id,
        ),
    )
    val state: StateFlow<ProtocolSelectionUiState> = _state

    fun select(id: String) {
        val profile = protocolCatalog.get(id)
        _state.update {
            it.copy(selectedId = id, selectedProfile = profile, error = null)
        }
    }

    fun selectedProfile(): ProtocolProfile? =
        _state.value.selectedProfile ?: _state.value.selectedId?.let { protocolCatalog.get(it) }

    /**
     * Validates profile config then advances: AMOUNT_ENTERED → PROTOCOL_SELECTED.
     */
    fun commitProtocol(): Boolean {
        val id = _state.value.selectedId
        if (id == null) {
            _state.update { it.copy(error = "Select a transaction protocol") }
            return false
        }
        return when (val result = sessionManager.selectProtocol(id)) {
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

    fun cancelSession(): Boolean {
        return when (val result = sessionManager.cancel()) {
            is TerminalSessionResult.Ok -> {
                _state.update { it.copy(sessionState = result.session.state) }
                true
            }
            is TerminalSessionResult.Err -> {
                _state.update { it.copy(error = result.message) }
                false
            }
        }
    }
}
