package com.cryptopos.pos.domain.fees

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FeeCalculatorTest {
    @Test
    fun half_percent_of_one_million() {
        val result = FeeCalculator.calculate(
            grossAmount = BigDecimal("1000000.00"),
            currency = "USD",
            feePercent = BigDecimal("0.5"),
            policyVersion = "screenshot-example-v1",
        )
        assertEquals(BigDecimal("5000.00"), result.feeAmount)
        assertEquals(BigDecimal("995000.00"), result.netAmount)
        assertEquals("USD", result.currency)
    }

    @Test
    fun rounding_half_up() {
        val result = FeeCalculator.calculate(
            BigDecimal("10.01"),
            "USD",
            BigDecimal("0.5"),
        )
        assertEquals(BigDecimal("0.05"), result.feeAmount)
        assertEquals(BigDecimal("9.96"), result.netAmount)
    }
}
