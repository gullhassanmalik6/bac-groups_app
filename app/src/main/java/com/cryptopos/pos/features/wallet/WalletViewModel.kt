package com.cryptopos.pos.features.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.Wallet
import com.cryptopos.pos.domain.usecase.GetWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWallets: GetWalletsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Wallet>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Wallet>>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { getWallets() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }
}
