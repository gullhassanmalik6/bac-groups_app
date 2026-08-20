package com.cryptopos.pos.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.core.network.ConnectivityObserver
import com.cryptopos.pos.domain.model.DashboardSnapshot
import com.cryptopos.pos.domain.usecase.LoadDashboardUseCase
import com.cryptopos.pos.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val loadDashboard: LoadDashboardUseCase,
    connectivityObserver: ConnectivityObserver,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<DashboardSnapshot>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardSnapshot>> = _state

    val isOnline = connectivityObserver.isOnline.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true,
    )

    init {
        load()
        syncScheduler.enqueueImmediate()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { loadDashboard() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }

    fun syncNow() {
        syncScheduler.enqueueImmediate()
        load()
    }
}
