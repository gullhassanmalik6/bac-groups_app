package com.cryptopos.pos.features.receipts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.receipt.ReceiptCopy
import com.cryptopos.pos.features.payment.SecureWindowEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalReceiptRoute(
    source: HistorySource,
    id: String,
    onDone: () -> Unit,
    viewModel: TerminalReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(source, id) { viewModel.load(source, id) }
    SecureWindowEffect()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.receipt)) }) }) { padding ->
        when {
            state.loading -> LoadingBlock(modifier = Modifier.padding(padding))
            state.receipt == null -> ErrorBlock(
                message = state.message ?: stringResource(R.string.not_found),
                onRetry = { viewModel.load(source, id) },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val receipt = state.receipt!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.copy == ReceiptCopy.CUSTOMER,
                            onClick = { viewModel.setCopy(ReceiptCopy.CUSTOMER) },
                            label = { Text(stringResource(R.string.customer_copy)) },
                        )
                        FilterChip(
                            selected = state.copy == ReceiptCopy.MERCHANT,
                            onClick = { viewModel.setCopy(ReceiptCopy.MERCHANT) },
                            label = { Text(stringResource(R.string.merchant_copy)) },
                        )
                    }
                    Text(
                        text = receipt.toPrintLines().joinToString("\n"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(
                            R.string.printer_used,
                            state.printerName ?: stringResource(R.string.printer_not_yet),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.sandbox_no_real_money),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.message?.let { Text(it) }
                    PosPrimaryButton(
                        text = stringResource(R.string.print_receipt),
                        onClick = { viewModel.print(source, id) },
                    )
                    PosSecondaryButton(
                        text = stringResource(R.string.print_both_copies),
                        onClick = { viewModel.printBoth(source, id) },
                    )
                    PosSecondaryButton(
                        text = stringResource(R.string.done),
                        onClick = onDone,
                    )
                }
            }
        }
    }
}
