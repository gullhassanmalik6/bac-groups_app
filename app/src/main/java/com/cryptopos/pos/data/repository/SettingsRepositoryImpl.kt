package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.local.datastore.AppPreferencesStore
import com.cryptopos.pos.domain.model.AppLanguage
import com.cryptopos.pos.domain.model.AppSettings
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.ThemeMode
import com.cryptopos.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesStore: AppPreferencesStore,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = preferencesStore.settings

    override suspend fun setThemeMode(mode: ThemeMode) = preferencesStore.setThemeMode(mode)

    override suspend fun setBiometricEnabled(enabled: Boolean) =
        preferencesStore.setBiometricEnabled(enabled)

    override suspend fun setGatewayProvider(provider: GatewayProvider) =
        preferencesStore.setGatewayProvider(provider)

    override suspend fun setCurrency(currency: String) = preferencesStore.setCurrency(currency)

    override suspend fun setLanguage(language: AppLanguage) = preferencesStore.setLanguage(language)

    override suspend fun current(): AppSettings = preferencesStore.current()
}
