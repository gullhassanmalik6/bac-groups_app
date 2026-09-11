package com.cryptopos.pos.features.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.AmountKeypad
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.domain.model.TerminalTransactionType
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TerminalAmountRoute(
    onContinue: (amount: String, currency: String, txnType: String) -> Unit,
    onBack: () -> Unit,
    viewModel: TerminalAmountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.payment_terminal_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SandboxBanner()

            state.sessionState?.let { sm ->
                Text(
                    text = stringResource(R.string.terminal_state_label, sm.name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = stringResource(R.string.amount_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = formatMoneyDisplay(state.amount, state.currency),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.semantics {
                    contentDescription = "Amount ${state.amount.ifBlank { "0" }} ${state.currency}"
                },
            )

            Text(
                text = stringResource(R.string.currency),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf("CAD", "USD", "EUR", "GBP", "AED", "SAR").forEach { code ->
                    FilterChip(
                        selected = state.currency == code,
                        onClick = { viewModel.setCurrency(code) },
                        label = { Text(code) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.transaction_type),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TerminalTransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.transactionType == type,
                        onClick = { viewModel.setTransactionType(type) },
                        label = { Text(type.name) },
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            AmountKeypad(
                onDigit = viewModel::appendDigit,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clearAmount,
            )

            Spacer(Modifier.height(4.dp))
            PosPrimaryButton(
                text = stringResource(R.string.continue_action),
                onClick = {
                    if (viewModel.commitAmount()) {
                        onContinue(
                            state.amount,
                            state.currency,
                            state.transactionType.name,
                        )
                    }
                },
            )
            PosSecondaryButton(text = stringResource(R.string.back), onClick = onBack)
        }
    }
}

@Composable
fun SandboxBanner() {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.sandbox_mode_banner),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

fun formatMoneyDisplay(amountRaw: String, currencyCode: String): String {
    val value = amountRaw.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.CANADA)
        format.currency = Currency.getInstance(currencyCode)
        format.format(value)
    } catch (_: Exception) {
        "$currencyCode ${value.setScale(2)}"
    }
}
