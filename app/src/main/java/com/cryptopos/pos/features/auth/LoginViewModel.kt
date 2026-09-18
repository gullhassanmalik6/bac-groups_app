package com.cryptopos.pos.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.data.local.datastore.RememberedLoginStore
import com.cryptopos.pos.domain.usecase.LoginUseCase
import com.cryptopos.pos.domain.usecase.SyncDeviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberDevice: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val ready: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val syncDevice: SyncDeviceUseCase,
    private val rememberedLoginStore: RememberedLoginStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events

    init {
        viewModelScope.launch {
            val remembered = rememberedLoginStore.getRememberedLogin()
            _state.update {
                if (remembered == null) {
                    it.copy(ready = true)
                } else {
                    it.copy(
                        email = remembered.email,
                        password = remembered.password,
                        rememberDevice = true,
                        ready = true,
                    )
                }
            }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onRememberDeviceChange(value: Boolean) =
        _state.update { it.copy(rememberDevice = value, error = null) }

    fun login() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { loginUseCase(current.email, current.password) }
                .onSuccess {
                    if (current.rememberDevice) {
                        rememberedLoginStore.saveRememberedLogin(current.email.trim(), current.password)
                    } else {
                        rememberedLoginStore.clearRememberedLogin()
                    }
                    runCatching { syncDevice() }
                    _state.update { it.copy(loading = false) }
                    _events.emit(Unit)
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.toUserMessage()) }
                }
        }
    }
}
