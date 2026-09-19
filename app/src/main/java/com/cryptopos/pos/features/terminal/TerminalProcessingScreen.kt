package com.cryptopos.pos.features.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import com.cryptopos.pos.features.payment.SecureWindowEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalProcessingRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onViewReceipt: (String) -> Unit,
    viewModel: TerminalProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    SecureWindowEffect()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.terminal_processing_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SandboxBanner()

            when (state.phase) {
                TerminalProcessingUiState.Phase.PROCESSING -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.processing),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    session?.let {
                        Text(
                            formatMoneyDisplay(it.amountRaw.orEmpty(), it.currency ?: "CAD"),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            it.protocolDisplayName.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                TerminalProcessingUiState.Phase.RESULT -> {
                    val success = viewModel.isSuccess(session)
                    Text(
                        text = if (success) {
                            stringResource(R.string.payment_approved_sandbox)
                        } else {
                            stringResource(R.string.payment_not_approved)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (success) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = stringResource(R.string.test_network_placeholder),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    session?.let { s ->
                        Text(
                            formatMoneyDisplay(s.amountRaw.orEmpty(), s.currency ?: "CAD"),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            stringResource(R.string.terminal_state_label, s.state.name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (s.usedRemoteProcessor) {
                                stringResource(R.string.processor_backend)
                            } else {
                                stringResource(R.string.processor_local_mock)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        s.authorizationCode?.let {
                            Text(
                                stringResource(R.string.authorization_label, it),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        s.processorReference?.let {
                            Text(
                                stringResource(R.string.processor_ref_label, it),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (s.cardLast4 != null) {
                            Text(
                                "${s.cardBrand ?: "CARD"} ****${s.cardLast4}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (s.signatureRequired) {
                            Text(
                                stringResource(R.string.signature_required_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        s.processorMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    state.printMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stringResource(R.string.sandbox_no_real_money),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (success) {
                        PosPrimaryButton(
                            text = stringResource(R.string.view_receipt),
                            onClick = {
                                viewModel.receiptSessionId()?.let(onViewReceipt)
                            },
                        )
                        PosSecondaryButton(
                            text = stringResource(R.string.print_receipt),
                            onClick = viewModel::printReceipt,
                        )
                    }
                    PosPrimaryButton(
                        text = stringResource(R.string.done),
                        onClick = {
                            viewModel.finish()
                            onDone()
                        },
                    )
                    if (session?.state == TerminalTransactionState.FAILED ||
                        session?.state == TerminalTransactionState.DECLINED
                    ) {
                        PosSecondaryButton(text = stringResource(R.string.back), onClick = onBack)
                    }
                }
            }
        }
    }
}
