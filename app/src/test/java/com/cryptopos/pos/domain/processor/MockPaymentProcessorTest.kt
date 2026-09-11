package com.cryptopos.pos.domain.processor

import com.cryptopos.pos.domain.model.SandboxOutcome
import com.cryptopos.pos.domain.model.TerminalTransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPaymentProcessorTest {
    private val processor = MockPaymentProcessor()

    private fun authRequest(
        amountMinor: Long = 1000,
        outcome: SandboxOutcome = SandboxOutcome.CAPTURE,
        token: String = "pm_test_visa_success",
        scenario: MockScenario? = null,
    ) = ProcessorAuthorizeRequest(
        sessionId = "sess-1",
        amountMinor = amountMinor,
        currency = "CAD",
        transactionType = TerminalTransactionType.SALE,
        protocolId = "101.1-4dg",
        protocolCode = "101.1",
        sandboxOutcome = outcome,
        paymentMethodToken = token,
        scenarioOverride = scenario,
    )

    @Test
    fun authorize_success_uses_test_auth_code() = runBlocking {
        val result = processor.authorize(authRequest())
        assertEquals(ProcessorStatus.APPROVED, result.status)
        assertNotNull(result.authorizationCode)
        assertTrue(result.authorizationCode!!.startsWith("TEST-"))
        assertTrue(result.processorReference.startsWith("sbx_"))
        assertTrue(result.sandbox)
        assertEquals("SANDBOX", result.environment)
    }

    @Test
    fun authorize_declined_scenario() = runBlocking {
        val result = processor.authorize(authRequest(scenario = MockScenario.DECLINED))
        assertEquals(ProcessorStatus.DECLINED, result.status)
        assertEquals(null, result.authorizationCode)
    }

    @Test
    fun authorize_amount_ending_13_declines() = runBlocking {
        val result = processor.authorize(authRequest(amountMinor = 1013))
        assertEquals(ProcessorStatus.DECLINED, result.status)
    }

    @Test
    fun authorize_timeout_amount_99() = runBlocking {
        val result = processor.authorize(authRequest(amountMinor = 199))
        assertEquals(ProcessorStatus.TIMEOUT, result.status)
    }

    @Test
    fun authorize_signature_outcome() = runBlocking {
        val result = processor.authorize(authRequest(outcome = SandboxOutcome.SIGNATURE))
        assertEquals(ProcessorStatus.APPROVED, result.status)
        assertTrue(result.signatureRequired)
    }

    @Test
    fun capture_after_approve() = runBlocking {
        val auth = processor.authorize(authRequest())
        val cap = processor.capture(
            ProcessorCaptureRequest("sess-1", auth.processorReference, 1000, "CAD"),
        )
        assertEquals(ProcessorStatus.APPROVED, cap.status)
        assertTrue(cap.message!!.contains("SANDBOX") || cap.message!!.contains("CAPTURED"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_non_token(): Unit = runBlocking {
        processor.authorize(authRequest(token = "4111111111111111"))
        Unit
    }
}
