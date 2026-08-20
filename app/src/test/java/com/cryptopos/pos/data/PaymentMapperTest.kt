package com.cryptopos.pos.data

import com.cryptopos.pos.data.local.db.entity.PendingPaymentEntity
import com.cryptopos.pos.data.mapper.toDomain
import com.cryptopos.pos.data.mapper.toLocalTransaction
import com.cryptopos.pos.data.remote.dto.PaymentDto
import com.cryptopos.pos.data.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMapperTest {
    @Test
    fun `maps payment dto to domain`() {
        val dto = PaymentDto(
            id = "pay_1",
            merchantId = "m_1",
            amount = "10.00",
            currency = "SAR",
            merchantReference = "POS-1",
            status = "success",
            netAmount = "9.70",
            fees = "0.30",
        )
        val domain = dto.toEntity().toDomain()
        assertEquals("pay_1", domain.id)
        assertEquals("10.00", domain.amount)
        assertEquals("success", domain.status)
    }

    @Test
    fun `pending payment becomes local transaction`() {
        val pending = PendingPaymentEntity(
            localId = "local-1",
            amount = "5.00",
            currency = "SAR",
            description = "sale",
            merchantReference = "POS-OFF-1",
            gatewayProvider = "sandbox",
            createdAtEpochMs = 1L,
            createdAtIso = "2026-01-01T00:00:00Z",
        )
        val tx = pending.toLocalTransaction()
        assertTrue(tx.pendingSync)
        assertEquals("pending_sync", tx.status)
        assertEquals("local-1", tx.toDomain().id)
    }
}
