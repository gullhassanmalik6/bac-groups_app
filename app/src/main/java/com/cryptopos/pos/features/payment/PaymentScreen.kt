package com.cryptopos.pos.features.payment

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRoute(
    onSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecureWindowEffect()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.payment)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.phase) {
                PaymentPhase.INPUT -> {
                    Text(
                        text = if (state.amount.isBlank()) "0.00" else state.amount,
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.semantics {
                            contentDescription = "Amount ${state.amount.ifBlank { "0" }} ${state.currency}"
                        },
                    )
                    Text(state.currency, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SAR", "USD", "EUR").forEach { code ->
                            FilterChip(
                                selected = state.currency == code,
                                onClick = { viewModel.setCurrency(code) },
                                label = { Text(code) },
                            )
                        }
                    }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    AmountKeypad(
                        onDigit = viewModel::appendDigit,
                        onBackspace = viewModel::backspace,
                        onClear = viewModel::clearAmount,
                    )
                    PosPrimaryButton(text = stringResource(R.string.charge_card), onClick = viewModel::startPayment)
                    PosSecondaryButton(text = stringResource(R.string.cancel), onClick = onBack)
                }

                PaymentPhase.WAITING_CARD, PaymentPhase.PROCESSING -> {
                    CircularProgressIndicator()
                    Text(
                        if (state.phase == PaymentPhase.WAITING_CARD) {
                            stringResource(R.string.tap_card)
                        } else {
                            stringResource(R.string.processing)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(state.message.orEmpty())
                    PosSecondaryButton(text = stringResource(R.string.cancel), onClick = viewModel::cancel)
                }

                PaymentPhase.SUCCESS -> {
                    Text(
                        stringResource(R.string.success),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(state.message.orEmpty())
                    state.transaction?.let {
                        Text("${it.amount} ${it.currency}", style = MaterialTheme.typography.titleLarge)
                        Text(it.status)
                    }
                    PosPrimaryButton(
                        text = stringResource(R.string.done),
                        onClick = { state.transaction?.id?.let(onSuccess) },
                    )
                }

                PaymentPhase.FAILURE -> {
                    Text(
                        stringResource(R.string.payment_failed),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(state.message.orEmpty())
                    PosPrimaryButton(text = stringResource(R.string.try_again), onClick = viewModel::cancel)
                    PosSecondaryButton(text = stringResource(R.string.back), onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun AmountKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            text = key,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }
        PosSecondaryButton(text = stringResource(R.string.clear), onClick = onClear)
    }
}

@Composable
fun SecureWindowEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
