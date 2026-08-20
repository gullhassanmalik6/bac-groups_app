package com.cryptopos.pos.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retries idempotent GET requests on transient network failures.
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastError: IOException? = null
        val attempts = if (request.method.equals("GET", ignoreCase = true)) 3 else 1
        repeat(attempts) { index ->
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code in 400..499 || index == attempts - 1) {
                    return response
                }
                response.close()
            } catch (error: IOException) {
                lastError = error
                if (index == attempts - 1) throw error
            }
        }
        throw lastError ?: IOException("Retry exhausted")
    }
}
