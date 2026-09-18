package com.cryptopos.pos.features.terminal

import androidx.lifecycle.ViewModel
import com.cryptopos.pos.domain.model.ProtocolProfile
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ProtocolSelectionUiState(
    val transactionType: TerminalTransactionType = TerminalTransactionType.SALE,
    val protocols: List<ProtocolProfile> = emptyList(),
    val selectedId: String? = null,
    val selectedProfile: ProtocolProfile? = null,
    val error: String? = null,
)

@HiltViewModel
class ProtocolSelectionViewModel @Inject constructor(
    private val protocolCatalog: ProtocolProfileCatalog,
) : ViewModel() {
    private val txnType = TerminalTransactionType.SALE

    private val _state = MutableStateFlow(
        ProtocolSelectionUiState(
            transactionType = txnType,
            protocols = protocolCatalog.forTransactionType(txnType),
        ),
    )
    val state: StateFlow<ProtocolSelectionUiState> = _state

    fun select(id: String) {
        val profile = protocolCatalog.get(id)
        _state.update {
            it.copy(selectedId = id, selectedProfile = profile, error = null)
        }
    }
}
