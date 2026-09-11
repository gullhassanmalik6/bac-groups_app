package com.cryptopos.pos.domain.protocol

import com.cryptopos.pos.data.mock.DefaultProtocolProfileCatalog
import com.cryptopos.pos.domain.model.AuthorizationMode
import com.cryptopos.pos.domain.model.SandboxOutcome
import com.cryptopos.pos.domain.model.TerminalTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolProfileCatalogTest {
    private val catalog = DefaultProtocolProfileCatalog()

    @Test
    fun profiles_have_required_config_fields() {
        val preAuth = catalog.get("101.6")!!
        assertEquals(AuthorizationMode.PRE_AUTH, preAuth.authorizationMode)
        assertEquals(SandboxOutcome.PRE_AUTH, preAuth.sandboxOutcome)
        assertTrue(preAuth.sandboxOnly)
        assertFalse(preAuth.documented)
        assertTrue(TerminalTransactionType.AUTH in preAuth.supportedTransactionTypes)
        assertFalse(TerminalTransactionType.SALE in preAuth.supportedTransactionTypes)
    }

    @Test
    fun filters_by_transaction_type() {
        val completionOnly = catalog.forTransactionType(TerminalTransactionType.COMPLETION)
        assertTrue(completionOnly.any { it.id == "201.1" })
        assertTrue(completionOnly.all { TerminalTransactionType.COMPLETION in it.supportedTransactionTypes })
    }

    @Test
    fun validate_rejects_wrong_txn_type() {
        val result = catalog.validateSelection("201.1", TerminalTransactionType.SALE)
        assertTrue(result is ProtocolSelectionValidation.Err)
    }

    @Test
    fun validate_accepts_matching_profile() {
        val result = catalog.validateSelection("101.1-4dg", TerminalTransactionType.SALE)
        assertTrue(result is ProtocolSelectionValidation.Ok)
        assertEquals("Demo", (result as ProtocolSelectionValidation.Ok).profile.uiEnvironmentLabel)
    }

    @Test
    fun offline_profiles_marked_simulation() {
        val offline = catalog.get("201")!!
        assertEquals("Simulation", offline.uiEnvironmentLabel)
        assertEquals(SandboxOutcome.OFFLINE_AUTH, offline.sandboxOutcome)
    }

    @Test
    fun signature_profile_flagged() {
        val p = catalog.get("201.3")!!
        assertTrue(p.signatureLikely)
        assertEquals(SandboxOutcome.SIGNATURE, p.sandboxOutcome)
    }
}
