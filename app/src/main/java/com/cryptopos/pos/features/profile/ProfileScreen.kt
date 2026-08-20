package com.cryptopos.pos.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.common.UiState
import com.cryptopos.pos.core.ui.components.ErrorBlock
import com.cryptopos.pos.core.ui.components.KeyValueRow
import com.cryptopos.pos.core.ui.components.LoadingBlock
import com.cryptopos.pos.core.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.merchant_profile)) }) }) { padding ->
        when (val ui = state) {
            UiState.Loading -> LoadingBlock(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorBlock(ui.message, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                val merchant = ui.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(merchant.companyName, style = MaterialTheme.typography.headlineMedium)
                    StatusChip(merchant.status)
                    KeyValueRow(stringResource(R.string.email), merchant.email)
                    KeyValueRow(stringResource(R.string.phone), merchant.phone)
                    KeyValueRow(stringResource(R.string.city), merchant.city)
                    KeyValueRow(stringResource(R.string.country), merchant.country)
                    KeyValueRow(stringResource(R.string.industry), merchant.industry ?: "-")
                    KeyValueRow(
                        stringResource(R.string.vat_number),
                        merchant.vatNumber?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_provided),
                    )
                    KeyValueRow(stringResource(R.string.merchant_id), merchant.id)
                }
            }
        }
    }
}
