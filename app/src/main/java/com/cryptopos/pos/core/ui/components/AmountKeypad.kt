package com.cryptopos.pos.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cryptopos.pos.R

/**
 * Shared POS numeric keypad — large touch targets for smart POS devices.
 */
@Composable
fun AmountKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    showDecimal: Boolean = true,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showDecimal) "." else " ", "0", "⌫"),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    val enabled = key.isNotBlank()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .semantics { contentDescription = "Keypad $key" }
                            .then(
                                if (enabled) {
                                    Modifier.clickable {
                                        when (key) {
                                            "⌫" -> onBackspace()
                                            else -> onDigit(key)
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = if (enabled) 1.dp else 0.dp,
                        shadowElevation = if (enabled) 1.dp else 0.dp,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                    ) {
                        if (enabled) {
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
        }
        PosSecondaryButton(text = stringResource(R.string.clear), onClick = onClear)
    }
}
