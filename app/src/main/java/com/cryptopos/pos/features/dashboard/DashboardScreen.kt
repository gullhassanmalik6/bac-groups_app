package com.cryptopos.pos.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.R
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.theme.NeonGreen
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.OfflineBanner
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.domain.device.DeviceHealthStatus

@Composable
fun DashboardRoute(
    onPay: () -> Unit,
    onLegacyPay: () -> Unit = {},
    onHistory: () -> Unit,
    onWallet: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onSupport: () -> Unit,
    onTransaction: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OfflineBanner(visible = !online)
        when (val ui = state) {
            UiState.Loading -> LoadingBlock(modifier = Modifier.fillMaxSize())
            is UiState.Error -> ErrorBlock(message = ui.message, onRetry = viewModel::load)
            is UiState.Success -> HomeContent(
                merchantName = ui.data.merchantName,
                deviceOnline = online &&
                    (device?.overallStatus == DeviceHealthStatus.ONLINE || device == null),
                deviceLabel = device?.let {
                    "Terminal ${it.identity.model}"
                } ?: "Terminal SUNMI V3",
                onPay = onPay,
                onSettings = onSettings,
                onMenu = onProfile,
                onHistory = onHistory,
                onWallet = onWallet,
            )
        }
    }
}

@Composable
private fun HomeContent(
    merchantName: String,
    deviceOnline: Boolean,
    deviceLabel: String,
    onPay: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    onHistory: () -> Unit,
    onWallet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.profile))
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CryptoPos",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (merchantName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = merchantName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(160.dp, 200.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = NeonGreen,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHistory),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(deviceLabel, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (deviceOnline) NeonGreen
                                else MaterialTheme.colorScheme.error,
                            ),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (deviceOnline) {
                            stringResource(R.string.online_status)
                        } else {
                            stringResource(R.string.offline_status)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (deviceOnline) NeonGreen else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PosPrimaryButton(
            text = stringResource(R.string.start_payment),
            onClick = onPay,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.powered_by_cryptopos),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onWallet),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
