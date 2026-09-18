package com.cryptopos.pos.features.receipts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.theme.NeonGreen
import com.cryptopos.pos.core.theme.ReceiptInk
import com.cryptopos.pos.core.theme.ReceiptPaper
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.receipt.ReceiptCopy
import com.cryptopos.pos.domain.receipt.TerminalReceipt
import com.cryptopos.pos.features.payment.SecureWindowEffect

@Composable
fun TerminalReceiptRoute(
    source: HistorySource,
    id: String,
    onDone: () -> Unit,
    onNewPayment: () -> Unit = onDone,
    viewModel: TerminalReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(source, id) { viewModel.load(source, id) }
    SecureWindowEffect()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        when {
            state.loading -> LoadingBlock(modifier = Modifier.padding(padding))
            state.receipt == null -> ErrorBlock(
                message = state.message ?: stringResource(R.string.not_found),
                onRetry = { viewModel.load(source, id) },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val base = state.receipt!!
                var copy by remember { mutableStateOf(ReceiptCopy.CUSTOMER) }
                val receipt = base.forCopy(copy)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PaperReceipt(receipt = receipt)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CopyChip(
                                label = stringResource(R.string.customer_copy),
                                selected = copy == ReceiptCopy.CUSTOMER,
                                accent = Color(0xFF3B82F6),
                                onClick = { copy = ReceiptCopy.CUSTOMER },
                            )
                            CopyChip(
                                label = stringResource(R.string.merchant_copy),
                                selected = copy == ReceiptCopy.MERCHANT,
                                accent = Color(0xFF64748B),
                                onClick = { copy = ReceiptCopy.MERCHANT },
                            )
                        }
                        state.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (copy == ReceiptCopy.MERCHANT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PosSecondaryButton(
                                text = stringResource(R.string.print_again),
                                onClick = { viewModel.print(source, id) },
                                modifier = Modifier.weight(1f),
                            )
                            PosPrimaryButton(
                                text = stringResource(R.string.new_payment_plus),
                                onClick = onNewPayment,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        PosPrimaryButton(
                            text = stringResource(R.string.print_receipt),
                            onClick = { viewModel.print(source, id) },
                        )
                        Spacer(Modifier.height(8.dp))
                        PosSecondaryButton(
                            text = stringResource(R.string.done),
                            onClick = onDone,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PaperReceipt(receipt: TerminalReceipt) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = ReceiptPaper,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (receipt.copy == ReceiptCopy.CUSTOMER) Color(0xFF3B82F6) else Color(0xFF64748B),
            ) {
                Text(
                    text = if (receipt.copy == ReceiptCopy.CUSTOMER) {
                        stringResource(R.string.customer_copy)
                    } else {
                        stringResource(R.string.merchant_copy)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "CryptoPos",
                color = ReceiptInk,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                receipt.merchantName,
                color = ReceiptInk.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            receipt.toPrintLines().forEach { line ->
                Text(
                    text = line,
                    modifier = Modifier.fillMaxWidth(),
                    color = ReceiptInk,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    textAlign = if (line.trimStart() == line && !line.contains("  ")) {
                        TextAlign.Center
                    } else {
                        TextAlign.Start
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            // QR placeholder block (payload shown as text in print lines)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("QR", color = ReceiptInk, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sandbox_no_real_money),
                color = NeonGreen,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
