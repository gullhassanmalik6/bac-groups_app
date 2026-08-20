package com.cryptopos.pos.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.domain.repository.SettingsRepository
import com.cryptopos.pos.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SplashDestination(
    val loggedIn: Boolean,
    val biometricRequired: Boolean,
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val destination = combine(
        observeAuthState(),
        settingsRepository.settings,
    ) { loggedIn, settings ->
        SplashDestination(
            loggedIn = loggedIn,
            biometricRequired = loggedIn && settings.biometricEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
