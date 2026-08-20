package com.cryptopos.pos.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptopos.pos.data.local.db.dao.PendingPaymentDao
import com.cryptopos.pos.data.local.db.dao.TransactionDao
import com.cryptopos.pos.data.local.db.entity.PendingPaymentEntity
import com.cryptopos.pos.data.local.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, PendingPaymentEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CryptoPosDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingPaymentDao(): PendingPaymentDao
}
