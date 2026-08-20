package com.cryptopos.pos.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val amount: String,
    val currency: String,
    val status: String,
    val merchantReference: String,
    val gatewayReference: String?,
    val paymentMethod: String?,
    val receiptNumber: String?,
    val failureReason: String?,
    val paymentDate: String?,
    val netAmount: String,
    val fees: String,
    val createdAt: String?,
    val usdtAmount: String?,
    val settlementStatus: String?,
    val synced: Boolean = true,
    val pendingSync: Boolean = false,
)
