package com.cryptopos.pos.features.transactions

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsRoute(
    onOpen: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search)) },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "success", "pending", "failed").forEach { status ->
                    FilterChip(
                        selected = state.filter.status == status,
                        onClick = { viewModel.onStatusChange(status) },
                        label = { Text(status ?: stringResource(R.string.all)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    items(state.items, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(tx.id) },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("${tx.amount} ${tx.currency}", style = MaterialTheme.typography.titleMedium)
                                    Text(tx.createdAt ?: "-", style = MaterialTheme.typography.bodyMedium)
                                }
                                StatusChip(tx.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailRoute(
    id: String,
    onReceipt: (String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { viewModel.load(id) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.transaction_details)) }) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> LoadingBlock()
                state.transaction == null -> ErrorBlock(
                    message = state.message ?: stringResource(R.string.not_found),
                    onRetry = { viewModel.load(id) },
                )
                else -> {
                    val tx = state.transaction!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("${tx.amount} ${tx.currency}", style = MaterialTheme.typography.headlineMedium)
                        StatusChip(tx.status)
                        KeyValueRow(stringResource(R.string.transaction_id), tx.id)
                        KeyValueRow(stringResource(R.string.reference), tx.merchantReference)
                        KeyValueRow(stringResource(R.string.gateway_ref), tx.gatewayReference ?: "-")
                        KeyValueRow(stringResource(R.string.fees), tx.fees)
                        KeyValueRow(stringResource(R.string.net_amount), tx.netAmount)
                        KeyValueRow(stringResource(R.string.date), tx.createdAt ?: tx.paymentDate ?: "-")
                        tx.failureReason?.let { KeyValueRow(stringResource(R.string.failure_reason), it) }
                        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        PosPrimaryButton(text = stringResource(R.string.view_receipt), onClick = { onReceipt(tx.id) })
                        PosSecondaryButton(text = stringResource(R.string.reprint), onClick = { viewModel.reprint(tx.id) })
                        PosSecondaryButton(text = stringResource(R.string.refund), onClick = { viewModel.refund(tx.id) })
                    }
                }
            }
        }
    }
}
