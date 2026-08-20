package com.cryptopos.pos.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cryptopos.pos.domain.model.AppLanguage
import com.cryptopos.pos.domain.model.AppSettings
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferences by preferencesDataStore(name = "cryptopos_preferences")

@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val GATEWAY = stringPreferencesKey("gateway_provider")
        val CURRENCY = stringPreferencesKey("currency")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<AppSettings> = context.appPreferences.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME] } ?: ThemeMode.SYSTEM,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            gatewayProvider = GatewayProvider.fromApi(prefs[Keys.GATEWAY]),
            currency = prefs[Keys.CURRENCY] ?: "SAR",
            language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferences.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.appPreferences.edit { it[Keys.BIOMETRIC] = enabled }
    }

    suspend fun setGatewayProvider(provider: GatewayProvider) {
        context.appPreferences.edit { it[Keys.GATEWAY] = provider.apiValue }
    }

    suspend fun setCurrency(currency: String) {
        context.appPreferences.edit { it[Keys.CURRENCY] = currency }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.appPreferences.edit { it[Keys.LANGUAGE] = language.tag }
    }
}
