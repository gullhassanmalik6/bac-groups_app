package com.cryptopos.pos.hardware.device

import com.cryptopos.pos.domain.device.DeviceHealthStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDeviceAdapterTest {
    @Test
    fun mock_adapter_reports_online_inventory() = runBlocking {
        val adapter = MockDeviceAdapter(serial = "MOCK-TEST-1")
        adapter.connect()
        val snap = adapter.snapshot()
        assertEquals("MOCK-TEST-1", snap.identity.serialNumber)
        assertEquals(DeviceHealthStatus.ONLINE, snap.overallStatus)
        assertEquals("MockDeviceAdapter", snap.adapterName)
        assertTrue(adapter.syncWithBackend().isSuccess)
    }

    @Test
    fun mock_adapter_can_simulate_warning() = runBlocking {
        val adapter = MockDeviceAdapter()
        adapter.setOverall(DeviceHealthStatus.WARNING)
        assertEquals(DeviceHealthStatus.WARNING, adapter.snapshot().overallStatus)
        assertTrue(adapter.snapshot().warnings.isNotEmpty())
    }
}
