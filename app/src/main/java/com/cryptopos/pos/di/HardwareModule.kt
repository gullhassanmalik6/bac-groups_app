package com.cryptopos.pos.di

import com.cryptopos.pos.core.analytics.AnalyticsTracker
import com.cryptopos.pos.core.analytics.TimberAnalyticsTracker
import com.cryptopos.pos.domain.payment.CardReader
import com.cryptopos.pos.domain.payment.PaymentGatewayResolver
import com.cryptopos.pos.domain.printer.PosPrinter
import com.cryptopos.pos.hardware.nfc.NfcCardReader
import com.cryptopos.pos.hardware.payment.DefaultPaymentGatewayResolver
import com.cryptopos.pos.hardware.printer.SunmiPosPrinter
import com.cryptopos.pos.hardware.security.BiometricAuthenticator
import com.cryptopos.pos.hardware.security.DefaultBiometricAuthenticator
import com.cryptopos.pos.hardware.security.DefaultDeviceIntegrityChecker
import com.cryptopos.pos.hardware.security.DeviceIntegrityChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HardwareModule {
    @Binds
    @Singleton
    abstract fun bindPrinter(impl: SunmiPosPrinter): PosPrinter

    @Binds
    @Singleton
    abstract fun bindCardReader(impl: NfcCardReader): CardReader

    @Binds
    @Singleton
    abstract fun bindGatewayResolver(impl: DefaultPaymentGatewayResolver): PaymentGatewayResolver

    @Binds
    @Singleton
    abstract fun bindBiometric(impl: DefaultBiometricAuthenticator): BiometricAuthenticator

    @Binds
    @Singleton
    abstract fun bindIntegrity(impl: DefaultDeviceIntegrityChecker): DeviceIntegrityChecker

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: TimberAnalyticsTracker): AnalyticsTracker
}
