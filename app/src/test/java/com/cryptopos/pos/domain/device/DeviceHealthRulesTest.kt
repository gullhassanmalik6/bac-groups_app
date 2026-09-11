package com.cryptopos.pos.domain.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceHealthRulesTest {
    @Test
    fun offline_network_makes_overall_offline() {
        val (status, warnings) = DeviceHealthRules.overall(
            connectivity = DeviceHealthStatus.OFFLINE,
            printer = PeripheralStatus.ONLINE,
            cardReader = PeripheralStatus.ONLINE,
            integrityCompromised = false,
        )
        assertEquals(DeviceHealthStatus.OFFLINE, status)
    }

    @Test
    fun printer_warning_or_integrity_yields_warning() {
        val (status, warnings) = DeviceHealthRules.overall(
            connectivity = DeviceHealthStatus.ONLINE,
            printer = PeripheralStatus.WARNING,
            cardReader = PeripheralStatus.ONLINE,
            integrityCompromised = false,
        )
        assertEquals(DeviceHealthStatus.WARNING, status)
        assertTrue(warnings.any { it.contains("printer") })

        val (status2, warnings2) = DeviceHealthRules.overall(
            connectivity = DeviceHealthStatus.ONLINE,
            printer = PeripheralStatus.ONLINE,
            cardReader = PeripheralStatus.ONLINE,
            integrityCompromised = true,
        )
        assertEquals(DeviceHealthStatus.WARNING, status2)
        assertTrue(warnings2.any { it.contains("integrity") })
    }

    @Test
    fun healthy_when_all_online() {
        val (status, warnings) = DeviceHealthRules.overall(
            connectivity = DeviceHealthStatus.ONLINE,
            printer = PeripheralStatus.ONLINE,
            cardReader = PeripheralStatus.ONLINE,
            integrityCompromised = false,
        )
        assertEquals(DeviceHealthStatus.ONLINE, status)
        assertTrue(warnings.isEmpty())
    }
}
