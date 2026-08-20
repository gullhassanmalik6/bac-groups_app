package com.cryptopos.pos.features.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.ui.components.EmptyState
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.PosSecondaryButton
import com.cryptopos.pos.core.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletRoute(viewModel: WalletViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.wallet)) }) }) { padding ->
        when (val ui = state) {
            UiState.Loading -> LoadingBlock(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorBlock(ui.message, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                if (ui.data.isEmpty()) {
                    EmptyState(stringResource(R.string.no_wallets), modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(ui.data, key = { it.id }) { wallet ->
                            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        if (wallet.isPrimary) {
                                            stringResource(R.string.primary_wallet)
                                        } else {
                                            stringResource(R.string.wallet)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    StatusChip(wallet.status)
                                    Text("${wallet.network} · ${wallet.provider}", style = MaterialTheme.typography.bodyMedium)
                                    Text(wallet.address, style = MaterialTheme.typography.bodyLarge)
                                    PosSecondaryButton(
                                        text = stringResource(R.string.copy_address),
                                        onClick = { copy(context, wallet.address) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copy(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("wallet", value))
}
