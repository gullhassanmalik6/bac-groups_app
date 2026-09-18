package com.cryptopos.pos.di

import com.cryptopos.pos.data.local.datastore.RememberedLoginStore
import com.cryptopos.pos.data.local.datastore.SecureSessionStore
import com.cryptopos.pos.data.repository.AuthRepositoryImpl
import com.cryptopos.pos.data.repository.HistoryRepositoryImpl
import com.cryptopos.pos.data.repository.MerchantRepositoryImpl
import com.cryptopos.pos.data.repository.PaymentRepositoryImpl
import com.cryptopos.pos.data.repository.SettingsRepositoryImpl
import com.cryptopos.pos.data.repository.TerminalRemoteGateway
import com.cryptopos.pos.data.repository.TerminalRemoteRepository
import com.cryptopos.pos.data.mock.DefaultProtocolProfileCatalog
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.repository.HistoryRepository
import com.cryptopos.pos.domain.repository.MerchantRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRememberedLoginStore(impl: SecureSessionStore): RememberedLoginStore

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRepository(impl: MerchantRepositoryImpl): MerchantRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProtocolProfileCatalog(impl: DefaultProtocolProfileCatalog): ProtocolProfileCatalog

    @Binds
    @Singleton
    abstract fun bindTerminalRemoteGateway(impl: TerminalRemoteRepository): TerminalRemoteGateway

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
