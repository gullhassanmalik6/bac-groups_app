package com.cryptopos.pos.data

import com.cryptopos.pos.data.mock.MockProtocols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockProtocolsTest {
    @Test
    fun catalog_includes_required_reference_labels() {
        val codes = MockProtocols.all.map { it.displayName }
        assertTrue(codes.any { it.startsWith("101.1") })
        assertTrue(codes.any { it.startsWith("101.7") })
        assertTrue(codes.any { it.startsWith("201 -") || it.startsWith("201 ") })
        assertTrue(codes.any { it.startsWith("201.3") })
        assertTrue(codes.any { it.startsWith("202.9") })
        assertTrue(codes.any { it.startsWith("MEIL ORD") })
        assertTrue(codes.any { it.startsWith("PLATFORMA INVOIS") })
        assertTrue(codes.any { it.startsWith("W+ 2010.1") })
    }

    @Test
    fun all_phase1_profiles_are_sandbox_only() {
        assertTrue(MockProtocols.all.isNotEmpty())
        assertTrue(MockProtocols.all.all { it.sandboxOnly })
    }

    @Test
    fun byId_resolves_online_4dg() {
        val profile = MockProtocols.byId("101.1-4dg")
        assertNotNull(profile)
        assertEquals("101.1 - Online 4 DG", profile!!.displayName)
    }
}
