package com.cryptopos.pos.di

import android.content.Context
import androidx.room.Room
import com.cryptopos.pos.data.local.db.CryptoPosDatabase
import com.cryptopos.pos.data.local.db.dao.PendingPaymentDao
import com.cryptopos.pos.data.local.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CryptoPosDatabase =
        Room.databaseBuilder(context, CryptoPosDatabase::class.java, "cryptopos.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: CryptoPosDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun providePendingPaymentDao(db: CryptoPosDatabase): PendingPaymentDao = db.pendingPaymentDao()
}
