package com.cryptopos.pos.di

import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.data.remote.interceptor.AuthInterceptor
import com.cryptopos.pos.data.remote.interceptor.RetryInterceptor
import com.cryptopos.pos.data.remote.interceptor.TokenAuthenticator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        val pins = BuildConfig.CERT_PINS
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (pins.isEmpty()) {
            if (!BuildConfig.DEBUG) {
                timber.log.Timber.e(
                    "CERT_PINS empty in release — TLS pinning disabled until pins are provisioned",
                )
            }
            return CertificatePinner.DEFAULT
        }
        val host = runCatching {
            java.net.URI(BuildConfig.API_BASE_URL).host
        }.getOrNull() ?: return CertificatePinner.DEFAULT
        val builder = CertificatePinner.Builder()
        pins.forEach { builder.add(host, it) }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
        retryInterceptor: RetryInterceptor,
        certificatePinner: CertificatePinner,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            timber.log.Timber.d("%s", redactLogMessage(message))
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .authenticator(authenticator)
            .addInterceptor(logging)
            .build()
    }

    private fun redactLogMessage(message: String): String {
        var out = message
        out = Regex("""(?i)(authorization\s*:\s*bearer\s+)\S+""")
            .replace(out, "$1[REDACTED]")
        out = Regex("""(?i)("password"\s*:\s*")[^"]*(")""")
            .replace(out, "$1[REDACTED]$2")
        out = Regex("""(?i)("access_token"\s*:\s*")[^"]*(")""")
            .replace(out, "$1[REDACTED]$2")
        out = Regex("""(?i)("refresh_token"\s*:\s*")[^"]*(")""")
            .replace(out, "$1[REDACTED]$2")
        out = Regex("""\b(?:\d[ -]*?){13,19}\b""").replace(out, "[REDACTED_PAN]")
        out = Regex("""(?i)\b(?:cvv|cvc)\s*[:=]?\s*\d{3,4}\b""").replace(out, "[REDACTED_CVV]")
        return out
    }

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient, json: Json): CryptoPosApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(CryptoPosApi::class.java)
    }
}
