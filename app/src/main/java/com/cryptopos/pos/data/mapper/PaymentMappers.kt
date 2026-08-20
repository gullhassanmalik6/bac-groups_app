package com.cryptopos.pos.data.mapper

import com.cryptopos.pos.data.local.db.entity.PendingPaymentEntity
import com.cryptopos.pos.data.local.db.entity.TransactionEntity
import com.cryptopos.pos.data.remote.dto.PaymentDto
import com.cryptopos.pos.data.remote.dto.ReceiptDto
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.model.PendingPayment
import com.cryptopos.pos.domain.model.Receipt

fun PaymentDto.toEntity(synced: Boolean = true, pendingSync: Boolean = false) = TransactionEntity(
    id = id,
    merchantId = merchantId,
    amount = amount,
    currency = currency,
    status = status,
    merchantReference = merchantReference,
    gatewayReference = gatewayReference,
    paymentMethod = paymentMethod,
    receiptNumber = receiptNumber,
    failureReason = failureReason,
    paymentDate = paymentDate,
    netAmount = netAmount,
    fees = fees,
    createdAt = createdAt,
    usdtAmount = settlement?.usdtAmount,
    settlementStatus = settlement?.status,
    synced = synced,
    pendingSync = pendingSync,
)

fun TransactionEntity.toDomain() = PaymentTransaction(
    id = id,
    amount = amount,
    currency = currency,
    status = status,
    merchantReference = merchantReference,
    gatewayReference = gatewayReference,
    paymentMethod = paymentMethod,
    receiptNumber = receiptNumber,
    failureReason = failureReason,
    paymentDate = paymentDate,
    netAmount = netAmount,
    fees = fees,
    createdAt = createdAt,
    usdtAmount = usdtAmount,
    settlementStatus = settlementStatus,
    isPendingSync = pendingSync,
)

fun PaymentDto.toDomain() = toEntity().toDomain()

fun ReceiptDto.toDomain(transactionId: String? = null) = Receipt(
    id = id,
    receiptNumber = receiptNumber,
    merchantName = merchantName,
    amount = amount,
    currency = currency,
    status = status,
    gateway = gateway,
    createdAt = createdAt,
    transactionId = transactionId,
    lines = printablePayload,
    qrPayload = "cryptopos://receipt/$receiptNumber",
)

fun PendingPaymentEntity.toDomain() = PendingPayment(
    localId = localId,
    amount = amount,
    currency = currency,
    description = description,
    merchantReference = merchantReference,
    gatewayProvider = gatewayProvider,
    createdAtEpochMs = createdAtEpochMs,
)

fun PendingPaymentEntity.toLocalTransaction() = TransactionEntity(
    id = localId,
    merchantId = "local",
    amount = amount,
    currency = currency,
    status = "pending_sync",
    merchantReference = merchantReference,
    gatewayReference = null,
    paymentMethod = "card",
    receiptNumber = null,
    failureReason = null,
    paymentDate = null,
    netAmount = amount,
    fees = "0.00",
    createdAt = createdAtIso,
    usdtAmount = null,
    settlementStatus = null,
    synced = false,
    pendingSync = true,
)
