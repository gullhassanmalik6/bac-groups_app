package com.cryptopos.pos.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.MerchantProfile
import com.cryptopos.pos.domain.usecase.GetMerchantProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getMerchantProfile: GetMerchantProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<MerchantProfile>>(UiState.Loading)
    val state: StateFlow<UiState<MerchantProfile>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { getMerchantProfile() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }
}
