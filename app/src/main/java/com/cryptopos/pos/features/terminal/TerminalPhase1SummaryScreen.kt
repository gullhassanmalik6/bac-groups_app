package com.cryptopos.pos.features.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.data.mock.MockProtocols

/**
 * Phase 2 summary — shows session after AMOUNT_ENTERED → PROTOCOL_SELECTED.
 * No card capture or live authorization yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalPhase1SummaryRoute(
    amount: String,
    currency: String,
    txnType: String,
    protocolId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: TerminalSessionSummaryViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionManager.session.collectAsStateWithLifecycle()
    val protocol = MockProtocols.byId(protocolId)
    val active = session

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.phase2_summary_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SandboxBanner()
            Text(
                text = stringResource(R.string.phase2_state_reached),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = active?.state?.name ?: "PROTOCOL_SELECTED",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = formatMoneyDisplay(
                    active?.amountRaw ?: amount,
                    active?.currency ?: currency,
                ),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(active?.transactionType?.name ?: txnType, style = MaterialTheme.typography.titleLarge)
            Text(
                text = active?.protocolDisplayName
                    ?: protocol?.displayName
                    ?: (active?.protocolId ?: protocolId),
                style = MaterialTheme.typography.titleMedium,
            )
            active?.protocolSandboxOutcome?.let {
                Text(
                    text = "Sandbox outcome: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            active?.id?.let {
                Text(
                    text = stringResource(R.string.session_id_label, it.take(8).uppercase()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.event_log_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            (active?.events ?: emptyList()).forEach { event ->
                Text(
                    text = "${event.from?.name ?: "∅"} → ${event.to.name}: ${event.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            Text(
                text = stringResource(R.string.phase2_next_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            PosPrimaryButton(
                text = stringResource(R.string.done),
                onClick = {
                    viewModel.sessionManager.clear()
                    onDone()
                },
            )
            PosSecondaryButton(text = stringResource(R.string.back), onClick = onBack)
        }
    }
}
