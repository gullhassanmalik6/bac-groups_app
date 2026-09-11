package com.cryptopos.pos.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.domain.model.AppLanguage
import com.cryptopos.pos.domain.model.AppSettings
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.ThemeMode
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.repository.SettingsRepository
import com.cryptopos.pos.domain.usecase.LogoutUseCase
import com.cryptopos.pos.hardware.security.BiometricAuthenticator
import com.cryptopos.pos.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val logoutUseCase: LogoutUseCase,
    authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val displayName = flow { emit(authRepository.currentUserName()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val email = flow { emit(authRepository.currentUserEmail()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val biometricAvailable = biometricAuthenticator.canAuthenticate()

    val supportedCurrencies = listOf("CAD", "USD", "EUR", "GBP", "AED", "SAR")
    val gatewayChoices = listOf(
        GatewayProvider.NOWPAYMENTS,
        GatewayProvider.SANDBOX,
        GatewayProvider.MOYASAR,
        GatewayProvider.HYPERPAY,
        GatewayProvider.CHECKOUT,
        GatewayProvider.STRIPE,
    )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }

    fun setGateway(provider: GatewayProvider) {
        viewModelScope.launch { settingsRepository.setGatewayProvider(provider) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { settingsRepository.setCurrency(currency) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun syncNow() = syncScheduler.enqueueImmediate()

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onDone()
        }
    }
}
