package com.cryptopos.pos.features.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.domain.model.AppLanguage
import com.cryptopos.pos.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onLoggedOut: () -> Unit,
    onSupport: () -> Unit,
    onAbout: () -> Unit,
    onProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val name by viewModel.displayName.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()

    LaunchedEffect(settings.language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(settings.language.tag))
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(name ?: stringResource(R.string.merchant), style = MaterialTheme.typography.headlineMedium)
            Text(email ?: "-")
            Text("API ${BuildConfig.API_BASE_URL}", style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) },
                )
            }

            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = settings.language == language,
                    onClick = { viewModel.setLanguage(language) },
                    label = { Text(language.displayName) },
                )
            }

            Text(stringResource(R.string.currency), style = MaterialTheme.typography.titleMedium)
            viewModel.supportedCurrencies.forEach { currency ->
                FilterChip(
                    selected = settings.currency == currency,
                    onClick = { viewModel.setCurrency(currency) },
                    label = { Text(currency) },
                )
            }

            Text(stringResource(R.string.gateway), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.gateway_hint), style = MaterialTheme.typography.bodyMedium)
            viewModel.gatewayChoices.forEach { provider ->
                FilterChip(
                    selected = settings.gatewayProvider == provider,
                    onClick = { viewModel.setGateway(provider) },
                    label = { Text(provider.displayName) },
                )
            }

            Text(stringResource(R.string.biometric_unlock), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = settings.biometricEnabled,
                onCheckedChange = viewModel::setBiometric,
                enabled = viewModel.biometricAvailable,
            )
            if (!viewModel.biometricAvailable) {
                Text(
                    stringResource(R.string.biometric_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PosSecondaryButton(text = stringResource(R.string.merchant_profile), onClick = onProfile)
            PosSecondaryButton(text = stringResource(R.string.sync_now), onClick = viewModel::syncNow)
            PosSecondaryButton(text = stringResource(R.string.support), onClick = onSupport)
            PosSecondaryButton(text = stringResource(R.string.about), onClick = onAbout)
            PosPrimaryButton(text = stringResource(R.string.log_out), onClick = { viewModel.logout(onLoggedOut) })
        }
    }
}
