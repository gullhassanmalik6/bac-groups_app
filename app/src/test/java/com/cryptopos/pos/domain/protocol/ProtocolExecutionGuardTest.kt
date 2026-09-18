package com.cryptopos.pos.domain.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolExecutionGuardTest {
    @Test
    fun sandbox_allows_simulation_without_docs() {
        assertTrue(ProtocolExecutionGuard.allowSandboxSimulation("SANDBOX", documented = false))
    }

    @Test
    fun production_blocks_undocumented_profiles() {
        assertFalse(ProtocolExecutionGuard.allowSandboxSimulation("PRODUCTION", documented = false))
    }

    @Test
    fun production_allows_documented_profiles() {
        assertTrue(ProtocolExecutionGuard.allowSandboxSimulation("PRODUCTION", documented = true))
    }
}
