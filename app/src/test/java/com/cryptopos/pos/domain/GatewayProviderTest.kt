package com.cryptopos.pos.domain

import com.cryptopos.pos.domain.model.GatewayProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class GatewayProviderTest {
    @Test
    fun `maps api values`() {
        assertEquals(GatewayProvider.STRIPE, GatewayProvider.fromApi("stripe"))
        assertEquals(GatewayProvider.HYPERPAY, GatewayProvider.fromApi("hyperpay"))
        assertEquals(GatewayProvider.CHECKOUT, GatewayProvider.fromApi("checkout"))
        assertEquals(GatewayProvider.NOWPAYMENTS, GatewayProvider.fromApi("nowpayments"))
        assertEquals(GatewayProvider.MOYASAR, GatewayProvider.fromApi("moyasar"))
        assertEquals(GatewayProvider.SANDBOX, GatewayProvider.fromApi("unknown"))
    }
}
