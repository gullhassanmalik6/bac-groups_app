package com.cryptopos.pos.features.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.R
import com.cryptopos.pos.core.ui.components.KeyValueRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportRoute() {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.support)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.support_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyLarge)
            KeyValueRow(stringResource(R.string.email), BuildConfig.SUPPORT_EMAIL)
            Text(stringResource(R.string.faq), style = MaterialTheme.typography.titleMedium)
            FaqItem(
                question = stringResource(R.string.faq_offline_q),
                answer = stringResource(R.string.faq_offline_a),
            )
            FaqItem(
                question = stringResource(R.string.faq_print_q),
                answer = stringResource(R.string.faq_print_a),
            )
            FaqItem(
                question = stringResource(R.string.faq_card_q),
                answer = stringResource(R.string.faq_card_a),
            )
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(question, style = MaterialTheme.typography.titleMedium)
        Text(answer, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutRoute() {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.about)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.terms), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.terms_body), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
