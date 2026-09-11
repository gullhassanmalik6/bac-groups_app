package com.cryptopos.pos.data.mapper

import com.cryptopos.pos.data.local.db.entity.TerminalHistoryEntity
import com.cryptopos.pos.data.local.db.entity.TransactionEntity
import com.cryptopos.pos.data.remote.dto.TerminalSessionDto
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import java.math.BigDecimal
import java.math.RoundingMode

fun TerminalHistoryEntity.toHistoryTransaction() = HistoryTransaction(
    id = id,
    source = HistorySource.TERMINAL,
    date = updatedAt ?: createdAt,
    amount = amountRaw,
    currency = currency,
    protocol = protocolLabel ?: protocolCode ?: protocolId,
    paymentMethod = paymentMethod
        ?: listOfNotNull(cardBrand, cardLast4?.let { "****$it" }).joinToString(" ").ifBlank { null },
    status = state,
    processor = when {
        usedRemoteProcessor -> "Backend mock"
        else -> "Local mock"
    },
    environment = environment,
    referenceId = processorReference,
    authorizationCode = authorizationCode,
    transactionType = transactionType,
    cardBrand = cardBrand,
    cardLast4 = cardLast4,
    processorMessage = processorMessage,
    amountMinor = amountMinor,
)

fun TransactionEntity.toHistoryTransaction() = HistoryTransaction(
    id = id,
    source = HistorySource.LEGACY,
    date = createdAt ?: paymentDate,
    amount = amount,
    currency = currency,
    protocol = null,
    paymentMethod = paymentMethod,
    status = status,
    processor = "Legacy gateway",
    environment = "SANDBOX",
    referenceId = gatewayReference ?: merchantReference,
    authorizationCode = null,
    transactionType = "SALE",
    cardBrand = null,
    cardLast4 = null,
    processorMessage = failureReason,
    amountMinor = null,
)

fun TerminalSessionDto.toHistoryEntity(usedRemote: Boolean = true): TerminalHistoryEntity {
    val amountRaw = BigDecimal(amountMinor)
        .movePointLeft(2)
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
    val method = listOfNotNull(cardBrand, cardLast4?.let { "****$it" })
        .joinToString(" ")
        .ifBlank { "Tokenized card" }
    return TerminalHistoryEntity(
        id = id,
        state = state,
        amountRaw = amountRaw,
        amountMinor = amountMinor,
        currency = currency.uppercase(),
        transactionType = transactionType,
        protocolId = protocolId,
        protocolCode = protocolCode,
        protocolLabel = protocolLabel,
        environment = environment,
        authorizationCode = authorizationCode,
        processorReference = processorReference,
        processorStatus = processorStatus,
        processorMessage = processorMessage,
        cardBrand = cardBrand,
        cardLast4 = cardLast4,
        paymentMethod = method,
        usedRemoteProcessor = usedRemote,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun TerminalSession.toHistoryEntity(): TerminalHistoryEntity? {
    // Skip in-progress drafts that are not useful in history.
    if (state == TerminalTransactionState.CREATED ||
        state == TerminalTransactionState.AMOUNT_ENTERED ||
        state == TerminalTransactionState.PROTOCOL_SELECTED ||
        state == TerminalTransactionState.CARD_PRESENTED ||
        state == TerminalTransactionState.AUTHORIZING
    ) {
        return null
    }
    val amount = amountRaw ?: return null
    val curr = currency ?: return null
    val method = listOfNotNull(cardBrand, cardLast4?.let { "****$it" })
        .joinToString(" ")
        .ifBlank { "Tokenized card" }
    return TerminalHistoryEntity(
        id = remoteSessionId ?: id,
        state = state.name,
        amountRaw = amount,
        amountMinor = amountMinor ?: 0L,
        currency = curr,
        transactionType = transactionType?.name,
        protocolId = protocolId,
        protocolCode = protocolCode,
        protocolLabel = protocolDisplayName,
        environment = environment,
        authorizationCode = authorizationCode,
        processorReference = processorReference,
        processorStatus = processorStatus,
        processorMessage = processorMessage ?: lastError,
        cardBrand = cardBrand,
        cardLast4 = cardLast4,
        paymentMethod = method,
        usedRemoteProcessor = usedRemoteProcessor,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}

fun PaymentTransaction.toHistoryTransaction() = HistoryTransaction(
    id = id,
    source = HistorySource.LEGACY,
    date = createdAt ?: paymentDate,
    amount = amount,
    currency = currency,
    protocol = null,
    paymentMethod = paymentMethod,
    status = status,
    processor = "Legacy gateway",
    environment = "SANDBOX",
    referenceId = gatewayReference ?: merchantReference,
    authorizationCode = null,
    transactionType = "SALE",
    cardBrand = null,
    cardLast4 = null,
    processorMessage = failureReason,
    amountMinor = null,
)

fun TerminalHistoryEntity.toDetailSessionHints(): Map<String, String?> = mapOf(
    "state" to state,
    "protocol" to (protocolLabel ?: protocolCode),
    "auth_code" to authorizationCode,
    "reference" to processorReference,
    "environment" to environment,
    "updated" to (updatedAt ?: createdAt),
)
