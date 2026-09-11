package com.cryptopos.pos.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.MetricCard
import com.cryptopos.pos.core.ui.components.OfflineBanner
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.core.ui.components.StatusChip
import com.cryptopos.pos.domain.model.DashboardSnapshot
import com.cryptopos.pos.domain.model.PaymentTransaction
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard)) }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OfflineBanner(visible = !online)
            when (val ui = state) {
                UiState.Loading -> LoadingBlock(modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorBlock(message = ui.message, onRetry = viewModel::load)
                is UiState.Success -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = viewModel::load,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DashboardContent(
                        data = ui.data,
                        device = device,
                        onPay = onPay,
                        onLegacyPay = onLegacyPay,
                        onHistory = onHistory,
                        onWallet = onWallet,
                        onSettings = onSettings,
                        onProfile = onProfile,
                        onSupport = onSupport,
                        onTransaction = onTransaction,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardSnapshot,
    device: com.cryptopos.pos.domain.device.DeviceSnapshot?,
    onPay: () -> Unit,
    onLegacyPay: () -> Unit,
    onHistory: () -> Unit,
    onWallet: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onSupport: () -> Unit,
    onTransaction: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(data.merchantName, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            StatusChip(status = data.merchantStatus)
        }
        device?.let { snap ->
            item { DeviceHealthCard(snap) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    label = stringResource(R.string.today_sales),
                    value = formatMoney(data.sales.todayAmount, data.sales.todayCount),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.week_sales),
                    value = formatMoney(data.sales.weekAmount, data.sales.weekCount),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    label = stringResource(R.string.month_sales),
                    value = formatMoney(data.sales.monthAmount, data.sales.monthCount),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.pending_payments),
                    value = data.sales.pendingCount.toString(),
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        item {
            PosPrimaryButton(text = stringResource(R.string.new_payment), onClick = onPay)
            Spacer(modifier = Modifier.height(8.dp))
            PosSecondaryButton(text = stringResource(R.string.legacy_card_payment), onClick = onLegacyPay)
        }
        item {
            Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Outlined.Payments, stringResource(R.string.pay), onPay, Modifier.weight(1f))
                QuickAction(Icons.Outlined.History, stringResource(R.string.history), onHistory, Modifier.weight(1f))
                QuickAction(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.wallet), onWallet, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Outlined.Person, stringResource(R.string.profile), onProfile, Modifier.weight(1f))
                QuickAction(Icons.Outlined.Settings, stringResource(R.string.settings), onSettings, Modifier.weight(1f))
                QuickAction(Icons.Outlined.SupportAgent, stringResource(R.string.support), onSupport, Modifier.weight(1f))
            }
        }
        item {
            val wallet = data.wallet
            Text(stringResource(R.string.wallet_status), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (wallet == null) {
                Text(stringResource(R.string.wallet_unavailable), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${wallet.network} · ${wallet.status}", style = MaterialTheme.typography.bodyLarge)
                Text(wallet.address, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text(stringResource(R.string.latest_transactions), style = MaterialTheme.typography.titleMedium)
        }
        items(data.recent, key = { it.id }) { tx ->
            TransactionRow(tx, onClick = { onTransaction(tx.id) })
        }
    }
}

@Composable
private fun DeviceHealthCard(snap: com.cryptopos.pos.domain.device.DeviceSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.device_status), style = MaterialTheme.typography.titleMedium)
                StatusChip(snap.overallStatus.name)
            }
            Text(
                "${snap.identity.manufacturer} · ${snap.identity.model}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.device_peripherals,
                    snap.connectivityStatus.name,
                    snap.printerStatus.name,
                    snap.cardReaderStatus.name,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (snap.warnings.isNotEmpty()) {
                Text(
                    snap.warnings.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(icon, contentDescription = label)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TransactionRow(tx: PaymentTransaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("${tx.amount} ${tx.currency}", style = MaterialTheme.typography.titleMedium)
                Text(tx.createdAt ?: tx.paymentDate ?: "-", style = MaterialTheme.typography.bodyMedium)
            }
            StatusChip(tx.status)
        }
    }
}

private fun formatMoney(amount: BigDecimal, count: Int): String =
    "${amount.setScale(2)} ($count)"
