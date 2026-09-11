package com.cryptopos.pos.data.mapper

import com.cryptopos.pos.data.remote.dto.TerminalSessionDto
import com.cryptopos.pos.data.remote.dto.TerminalSessionEventDto
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalSessionEvent
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Currency
import java.util.UUID

fun TerminalSessionDto.toDomain(): TerminalSession {
    val state = runCatching { TerminalTransactionState.valueOf(this.state) }
        .getOrDefault(TerminalTransactionState.FAILED)
    val txnType = runCatching { TerminalTransactionType.valueOf(transactionType) }
        .getOrNull()
    val fraction = runCatching {
        Currency.getInstance(currency.uppercase()).defaultFractionDigits.coerceAtLeast(0)
    }.getOrDefault(2)
    val amountRaw = BigDecimal(amountMinor)
        .movePointLeft(fraction)
        .setScale(fraction, RoundingMode.HALF_UP)
        .toPlainString()

    return TerminalSession(
        id = id.ifBlank { UUID.randomUUID().toString() },
        state = state,
        amountRaw = amountRaw,
        amountMinor = amountMinor,
        currency = currency.uppercase(),
        transactionType = txnType,
        protocolId = protocolId,
        protocolCode = protocolCode,
        protocolDisplayName = protocolLabel,
        protocolSandboxOutcome = sandboxOutcome?.uppercase(),
        environment = environment,
        authorizationCode = authorizationCode,
        processorReference = processorReference,
        processorStatus = processorStatus,
        processorMessage = processorMessage,
        signatureRequired = signatureRequired,
        cardBrand = cardBrand,
        cardLast4 = cardLast4,
        events = events.map { it.toDomain() },
        createdAt = createdAt.parseInstantOrNow(),
        updatedAt = updatedAt.parseInstantOrNow(),
        lastError = if (state == TerminalTransactionState.FAILED || state == TerminalTransactionState.DECLINED) {
            processorMessage
        } else {
            null
        },
        remoteSessionId = id,
        usedRemoteProcessor = true,
    )
}

fun TerminalSessionEventDto.toDomain(): TerminalSessionEvent {
    val toState = runCatching { TerminalTransactionState.valueOf(to.orEmpty()) }
        .getOrDefault(TerminalTransactionState.CREATED)
    val fromState = from?.let { runCatching { TerminalTransactionState.valueOf(it) }.getOrNull() }
    return TerminalSessionEvent(
        from = fromState,
        to = toState,
        note = note.orEmpty(),
    )
}

private fun String?.parseInstantOrNow(): Instant =
    runCatching { Instant.parse(this) }.getOrElse { Instant.now() }
