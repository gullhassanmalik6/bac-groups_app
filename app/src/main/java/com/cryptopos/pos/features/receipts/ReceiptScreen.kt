package com.cryptopos.pos.features.receipts

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.KeyValueRow
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.features.payment.SecureWindowEffect
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptRoute(
    transactionId: String,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(transactionId) { viewModel.load(transactionId) }
    SecureWindowEffect()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.receipt)) }) }) { padding ->
        when {
            state.loading -> LoadingBlock(modifier = Modifier.padding(padding))
            state.receipt == null -> ErrorBlock(
                message = state.message ?: stringResource(R.string.not_found),
                onRetry = { viewModel.load(transactionId) },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val receipt = state.receipt!!
                val qr = remember(receipt.qrPayload) { generateQrBitmap(receipt.qrPayload) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(receipt.merchantName, style = MaterialTheme.typography.headlineMedium)
                    Text("${receipt.amount} ${receipt.currency}", style = MaterialTheme.typography.displayLarge)
                    KeyValueRow(stringResource(R.string.status), receipt.status)
                    KeyValueRow(stringResource(R.string.receipt_number), receipt.receiptNumber)
                    KeyValueRow(
                        stringResource(R.string.vat_number),
                        receipt.lines["vat_number"]?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.not_provided),
                    )
                    KeyValueRow(stringResource(R.string.transaction_id), receipt.transactionId ?: transactionId)
                    KeyValueRow(stringResource(R.string.date), receipt.createdAt ?: "-")
                    KeyValueRow(stringResource(R.string.gateway), receipt.gateway ?: "-")
                    qr?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.receipt_qr),
                            modifier = Modifier.size(180.dp),
                        )
                    }
                    state.message?.let { Text(it) }
                    PosPrimaryButton(
                        text = stringResource(R.string.print_receipt),
                        onClick = { viewModel.print(transactionId) },
                    )
                }
            }
        }
    }
}

private fun generateQrBitmap(payload: String, size: Int = 512): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    bitmap
}.getOrNull()
