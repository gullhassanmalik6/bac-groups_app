package com.cryptopos.pos.features.terminal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopos.pos.R
import com.cryptopos.pos.core.theme.NeonGreen
import com.cryptopos.pos.core.ui.components.PosDangerOutlinedButton
import com.cryptopos.pos.core.ui.components.PosPrimaryButton
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import com.cryptopos.pos.features.payment.SecureWindowEffect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalProcessingRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onViewReceipt: (String) -> Unit,
    viewModel: TerminalProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    SecureWindowEffect()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.phase == TerminalProcessingUiState.Phase.PROCESSING) {
                            stringResource(R.string.processing_title)
                        } else {
                            stringResource(R.string.payment_method_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when (state.phase) {
            TerminalProcessingUiState.Phase.PROCESSING -> {
                ProcessingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    onCancel = {
                        viewModel.cancel()
                        onBack()
                    },
                )
            }
            TerminalProcessingUiState.Phase.RESULT -> {
                ResultContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    success = viewModel.isSuccess(session),
                    session = session,
                    error = state.error,
                    printMessage = state.printMessage,
                    onViewReceipt = {
                        viewModel.receiptSessionId()?.let(onViewReceipt)
                    },
                    onDone = {
                        viewModel.finish()
                        onDone()
                    },
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun ProcessingContent(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val stroke = 8.dp.toPx()
                drawArc(
                    color = NeonGreen.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = Size(size.width - stroke, size.height - stroke),
                    topLeft = Offset(stroke / 2, stroke / 2),
                )
                drawArc(
                    color = NeonGreen,
                    startAngle = angle,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = Size(size.width - stroke, size.height - stroke),
                    topLeft = Offset(stroke / 2, stroke / 2),
                )
            }
            Icon(
                Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ProcessStep(stringResource(R.string.step_connecting), StepStatus.DONE)
            ProcessStep(stringResource(R.string.step_reading), StepStatus.DONE)
            ProcessStep(stringResource(R.string.step_authorizing), StepStatus.DONE)
            ProcessStep(stringResource(R.string.step_processing), StepStatus.ACTIVE)
            ProcessStep(stringResource(R.string.step_finalizing), StepStatus.PENDING)
        }
        Spacer(Modifier.weight(1f))
        PosDangerOutlinedButton(text = stringResource(R.string.cancel), onClick = onCancel)
    }
}

private enum class StepStatus { DONE, ACTIVE, PENDING }

@Composable
private fun ProcessStep(label: String, status: StepStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (status) {
            StepStatus.DONE -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NeonGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
            StepStatus.ACTIVE -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonGreen),
                    )
                }
            }
            StepStatus.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = when (status) {
                StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun ResultContent(
    modifier: Modifier,
    success: Boolean,
    session: com.cryptopos.pos.domain.terminal.TerminalSession?,
    error: String?,
    printMessage: String?,
    onViewReceipt: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (success) NeonGreen else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(88.dp),
        )
        Text(
            text = if (success) {
                stringResource(R.string.payment_approved)
            } else {
                stringResource(R.string.payment_not_approved)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = if (success) NeonGreen else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
        session?.let { s ->
            Text(
                text = stringResource(R.string.amount_label) + " " +
                    formatMoneyDisplay(s.amountRaw.orEmpty(), s.currency ?: "USD"),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            ResultRow(stringResource(R.string.protocol_label), s.protocolCode ?: s.protocolDisplayName.orEmpty())
            ResultRow(
                "Card",
                if (s.cardLast4 != null) "**** **** **** ${s.cardLast4}" else (s.cardBrand ?: "CARD"),
            )
            ResultRow("TXN ID", s.remoteSessionId ?: s.id.take(10).uppercase())
            ResultRow("ARN", s.processorReference ?: "SANDBOX")
            ResultRow("Terminal", "POS")
            ResultRow("Connection", s.environment ?: "SANDBOX")
            ResultRow(
                "Date & Time",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now()),
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        printMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        Spacer(Modifier.weight(1f))
        if (success) {
            PosPrimaryButton(
                text = stringResource(R.string.print_receipt),
                onClick = onViewReceipt,
            )
        }
        TextButton(onClick = onDone) {
            Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (session?.state == TerminalTransactionState.FAILED ||
            session?.state == TerminalTransactionState.DECLINED
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
