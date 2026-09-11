package com.cryptopos.pos.features.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.EmptyState
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.KeyValueRow
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.core.ui.components.StatusChip
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsRoute(
    onOpen: (HistorySource, String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_history)) },
                actions = {
                    TextButton(onClick = viewModel::refresh) {
                        Text(stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_txn_or_ref)) },
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val statuses = listOf(
                    null to stringResource(R.string.all),
                    "COMPLETED" to "COMPLETED",
                    "APPROVED" to "APPROVED",
                    "DECLINED" to "DECLINED",
                    "FAILED" to "FAILED",
                    "success" to "success",
                    "pending" to "pending",
                )
                statuses.forEach { (value, label) ->
                    FilterChip(
                        selected = state.filter.status == value,
                        onClick = { viewModel.onStatusChange(value) },
                        label = { Text(label) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.protocol == null,
                    onClick = { viewModel.onProtocolChange(null) },
                    label = { Text(stringResource(R.string.all_protocols)) },
                )
                state.protocols.take(12).forEach { protocol ->
                    FilterChip(
                        selected = state.filter.protocol == protocol,
                        onClick = { viewModel.onProtocolChange(protocol) },
                        label = { Text(protocol, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.currency == null,
                    onClick = { viewModel.onCurrencyChange(null) },
                    label = { Text(stringResource(R.string.all_currencies)) },
                )
                state.currencies.forEach { currency ->
                    FilterChip(
                        selected = state.filter.currency == currency,
                        onClick = { viewModel.onCurrencyChange(currency) },
                        label = { Text(currency) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.filter.amountQuery.orEmpty(),
                    onValueChange = { viewModel.onAmountChange(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.amount_filter)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.filter.fromDate.orEmpty(),
                    onValueChange = { viewModel.onFromDateChange(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.from_date)) },
                    singleLine = true,
                    placeholder = { Text("YYYY-MM-DD") },
                )
                OutlinedTextField(
                    value = state.filter.toDate.orEmpty(),
                    onValueChange = { viewModel.onToDateChange(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.to_date)) },
                    singleLine = true,
                    placeholder = { Text("YYYY-MM-DD") },
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.items.isEmpty()) {
                EmptyState(stringResource(R.string.no_transactions))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.listKey }) { tx ->
                        HistoryRow(tx = tx, onClick = { onOpen(tx.source, tx.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    tx: HistoryTransaction,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${tx.amount} ${tx.currency}",
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusChip(tx.status)
            }
            Text(
                tx.date ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.txn_id_short, tx.id.take(13)),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    tx.protocol ?: stringResource(R.string.legacy_payment),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    tx.processor ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                listOfNotNull(
                    tx.paymentMethod,
                    tx.environment,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailRoute(
    source: HistorySource,
    id: String,
    onReceipt: (String) -> Unit,
    onTerminalReceipt: (String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(source, id) { viewModel.load(source, id) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.transaction_details)) }) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> LoadingBlock()
                state.item == null -> ErrorBlock(
                    message = state.message ?: stringResource(R.string.not_found),
                    onRetry = { viewModel.load(source, id) },
                )
                else -> {
                    val tx = state.item!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("${tx.amount} ${tx.currency}", style = MaterialTheme.typography.headlineMedium)
                        StatusChip(tx.status)
                        KeyValueRow(stringResource(R.string.transaction_id), tx.id)
                        KeyValueRow(stringResource(R.string.date), tx.date ?: "-")
                        KeyValueRow(stringResource(R.string.protocol_label), tx.protocol ?: "-")
                        KeyValueRow(stringResource(R.string.payment_method_label), tx.paymentMethod ?: "-")
                        KeyValueRow(stringResource(R.string.processor_label), tx.processor ?: "-")
                        KeyValueRow(stringResource(R.string.environment_label), tx.environment ?: "-")
                        KeyValueRow(stringResource(R.string.reference), tx.referenceId ?: "-")
                        tx.authorizationCode?.let {
                            KeyValueRow(stringResource(R.string.auth_code_label), it)
                        }
                        tx.transactionType?.let {
                            KeyValueRow(stringResource(R.string.transaction_type), it)
                        }
                        tx.processorMessage?.let {
                            KeyValueRow(stringResource(R.string.failure_reason), it)
                        }
                        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (tx.source == HistorySource.LEGACY) {
                            PosPrimaryButton(
                                text = stringResource(R.string.view_receipt),
                                onClick = { onReceipt(tx.id) },
                            )
                            PosSecondaryButton(
                                text = stringResource(R.string.reprint),
                                onClick = { viewModel.reprint(tx.id) },
                            )
                            PosSecondaryButton(
                                text = stringResource(R.string.refund),
                                onClick = { viewModel.refund(tx.id) },
                            )
                        } else {
                            PosPrimaryButton(
                                text = stringResource(R.string.view_receipt),
                                onClick = { onTerminalReceipt(tx.id) },
                            )
                            PosSecondaryButton(
                                text = stringResource(R.string.print_receipt),
                                onClick = { viewModel.printTerminal(tx.source, tx.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
