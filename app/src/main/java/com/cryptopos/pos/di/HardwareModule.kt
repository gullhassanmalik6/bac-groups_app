package com.cryptopos.pos.di

import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.core.analytics.AnalyticsTracker
import com.cryptopos.pos.core.analytics.TimberAnalyticsTracker
import com.cryptopos.pos.domain.device.DeviceAdapter
import com.cryptopos.pos.domain.payment.CardReader
import com.cryptopos.pos.domain.payment.PaymentGatewayResolver
import com.cryptopos.pos.domain.printer.PosPrinter
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.processor.MockPaymentProcessor
import com.cryptopos.pos.domain.processor.PaymentProcessor
import com.cryptopos.pos.domain.processor.PaymentProcessorResolver
import com.cryptopos.pos.domain.processor.UnconfiguredCertifiedPaymentProcessor
import com.cryptopos.pos.hardware.device.DefaultDeviceAdapter
import com.cryptopos.pos.hardware.nfc.NfcCardReader
import com.cryptopos.pos.hardware.payment.DefaultPaymentGatewayResolver
import com.cryptopos.pos.hardware.printer.AdapterPosPrinter
import com.cryptopos.pos.hardware.printer.ResolvingPrinterAdapter
import com.cryptopos.pos.hardware.security.BiometricAuthenticator
import com.cryptopos.pos.hardware.security.DefaultBiometricAuthenticator
import com.cryptopos.pos.hardware.security.DefaultDeviceIntegrityChecker
import com.cryptopos.pos.hardware.security.DeviceIntegrityChecker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HardwareModule {
    @Binds
    @Singleton
    abstract fun bindPrinterAdapter(impl: ResolvingPrinterAdapter): PrinterAdapter

    @Binds
    @Singleton
    abstract fun bindPrinter(impl: AdapterPosPrinter): PosPrinter

    @Binds
    @Singleton
    abstract fun bindDeviceAdapter(impl: DefaultDeviceAdapter): DeviceAdapter

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

@Module
@InstallIn(SingletonComponent::class)
object PaymentProcessorModule {
    @Provides
    @Singleton
    fun providePaymentProcessor(
        mock: MockPaymentProcessor,
        certified: UnconfiguredCertifiedPaymentProcessor,
    ): PaymentProcessor =
        PaymentProcessorResolver.resolve(
            key = BuildConfig.PAYMENT_PROCESSOR,
            mock = mock,
            certifiedUnconfigured = certified,
        )
}
