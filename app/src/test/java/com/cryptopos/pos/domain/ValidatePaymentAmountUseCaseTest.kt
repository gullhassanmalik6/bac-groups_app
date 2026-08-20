package com.cryptopos.pos.domain

import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.usecase.ValidatePaymentAmountUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ValidatePaymentAmountUseCaseTest {
    private val useCase = ValidatePaymentAmountUseCase()

    @Test
    fun `accepts valid amount with two decimals`() {
        assertEquals(BigDecimal("12.50"), useCase("12.50"))
    }

    @Test
    fun `rejects zero`() {
        val error = runCatching { useCase("0") }.exceptionOrNull()
        assertTrue(error is PosError.Validation)
    }

    @Test
    fun `rejects blank`() {
        val error = runCatching { useCase("") }.exceptionOrNull()
        assertTrue(error is PosError.Validation)
    }
}
