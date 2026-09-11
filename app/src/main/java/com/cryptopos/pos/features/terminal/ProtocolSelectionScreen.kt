package com.cryptopos.pos.features.terminal

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.domain.model.EnvironmentBadge
import com.cryptopos.pos.domain.model.ProtocolProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSelectionRoute(
    onContinue: (protocolId: String) -> Unit,
    onHistory: () -> Unit,
    onWallet: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProtocolSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.select_transaction_protocol)) })
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProtocolGridPane(
                        state = state,
                        onSelect = viewModel::select,
                        onContinue = {
                            if (viewModel.commitProtocol()) {
                                onContinue(state.selectedId!!)
                            }
                        },
                        onHistory = onHistory,
                        onWallet = onWallet,
                        onBack = onBack,
                        modifier = Modifier
                            .weight(0.72f)
                            .fillMaxHeight(),
                    )
                    TransactionSidePanel(
                        amountLabel = formatMoneyDisplay(state.amount, state.currency),
                        txnType = state.transactionType.name,
                        sessionState = state.sessionState?.name,
                        selected = viewModel.selectedProfile(),
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ProtocolGridPane(
                        state = state,
                        onSelect = viewModel::select,
                        onContinue = {
                            if (viewModel.commitProtocol()) {
                                onContinue(state.selectedId!!)
                            }
                        },
                        onHistory = onHistory,
                        onWallet = onWallet,
                        onBack = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TransactionSidePanel(
                        amountLabel = formatMoneyDisplay(state.amount, state.currency),
                        txnType = state.transactionType.name,
                        sessionState = state.sessionState?.name,
                        selected = viewModel.selectedProfile(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtocolGridPane(
    state: ProtocolSelectionUiState,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    onHistory: () -> Unit,
    onWallet: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SandboxBanner()
        Text(
            text = stringResource(R.string.select_transaction_protocol),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.protocol_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.protocols, key = { it.id }) { profile ->
                ProtocolCard(
                    profile = profile,
                    selected = state.selectedId == profile.id,
                    onClick = { onSelect(profile.id) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PosSecondaryButton(
                text = stringResource(R.string.open_wallet),
                onClick = onWallet,
                modifier = Modifier.weight(1f),
            )
            PosSecondaryButton(
                text = stringResource(R.string.transaction_history),
                onClick = onHistory,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PosSecondaryButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                modifier = Modifier.weight(1f),
            )
            PosPrimaryButton(
                text = stringResource(R.string.continue_action),
                onClick = onContinue,
                modifier = Modifier.weight(1f),
                enabled = state.selectedId != null,
            )
        }
    }
}

@Composable
private fun ProtocolCard(
    profile: ProtocolProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(profile.uiEnvironmentLabel)
                    append(" · ")
                    append(
                        when (profile.environmentBadge) {
                            EnvironmentBadge.SANDBOX -> stringResource(R.string.badge_sandbox)
                            EnvironmentBadge.PROCESSOR_READY -> stringResource(R.string.badge_processor_ready)
                        },
                    )
                    if (profile.digitGroup != null) append(" · ${profile.digitGroup}DG")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransactionSidePanel(
    amountLabel: String,
    txnType: String,
    sessionState: String?,
    selected: ProtocolProfile?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.transaction_panel), style = MaterialTheme.typography.titleMedium)
            Text(amountLabel, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = sessionState?.let { stringResource(R.string.terminal_state_label, it) }
                    ?: "$txnType · Phase 2",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = {
                    when (sessionState) {
                        "PROTOCOL_SELECTED" -> 0.35f
                        "AMOUNT_ENTERED" -> 0.2f
                        null -> if (selected == null) 0f else 0.35f
                        else -> 0.15f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = selected?.displayName ?: stringResource(R.string.no_protocol_selected),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.last_update_none),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
    }
}
