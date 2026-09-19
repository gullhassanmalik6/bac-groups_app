package com.cryptopos.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cryptopos.pos.core.navigation.CryptoPosNavHost
import com.cryptopos.pos.core.theme.CryptoPosTheme
import com.cryptopos.pos.domain.model.ThemeMode
import com.cryptopos.pos.domain.repository.SettingsRepository
import com.cryptopos.pos.hardware.nfc.NfcActivityHolder
import com.cryptopos.pos.hardware.security.DeviceIntegrityChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var nfcActivityHolder: NfcActivityHolder
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var deviceIntegrityChecker: DeviceIntegrityChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        nfcActivityHolder.attach(this)

        if (deviceIntegrityChecker.isCompromised()) {
            Timber.w("Device integrity findings: %s", deviceIntegrityChecker.findings())
            if (BuildConfig.ENFORCE_DEVICE_INTEGRITY) {
                finish()
                return
            }
        }

        val themeMode = settingsRepository.settings
            .map { it.themeMode }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

        lifecycleScope.launch {
            settingsRepository.settings.collect { settings ->
                val tag = settings.language.tag
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
            }
        }

        setContent {
            val mode by themeMode.collectAsStateWithLifecycle()
            CryptoPosTheme(themeMode = mode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CryptoPosNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        nfcActivityHolder.detach(this)
        super.onDestroy()
    }
}
