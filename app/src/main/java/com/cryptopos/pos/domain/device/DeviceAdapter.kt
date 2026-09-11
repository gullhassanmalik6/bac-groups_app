package com.cryptopos.pos.domain.device

import java.time.Instant

/**
 * Vendor-neutral device health. Verifone / PAX / Ingenico / Sunmi plug in via [DeviceAdapter].
 */
enum class DeviceHealthStatus {
    ONLINE,
    OFFLINE,
    WARNING,
}

enum class PeripheralStatus {
    ONLINE,
    OFFLINE,
    WARNING,
    UNAVAILABLE,
}

data class DeviceIdentity(
    val serialNumber: String,
    val manufacturer: String,
    val model: String,
    val firmware: String? = null,
    val appVersion: String,
    val androidVersion: String,
)

/**
 * Snapshot matching the Phase 9 Device inventory fields.
 */
data class DeviceSnapshot(
    val identity: DeviceIdentity,
    val overallStatus: DeviceHealthStatus,
    val connectivityStatus: DeviceHealthStatus,
    val printerStatus: PeripheralStatus,
    val cardReaderStatus: PeripheralStatus,
    val lastHeartbeat: Instant? = null,
    val remoteDeviceId: String? = null,
    val adapterName: String,
    val warnings: List<String> = emptyList(),
)

/**
 * Hardware facade so certified SDKs (Sunmi, Verifone, PAX, Ingenico, …) can be swapped later.
 * Does not process cards or store PAN/CVV.
 */
interface DeviceAdapter {
    fun adapterName(): String

    /** Bind / initialize peripherals (printer, reader). Safe to call repeatedly. */
    suspend fun connect(): Result<Unit>

    /** Local inventory + peripheral health. */
    suspend fun snapshot(): DeviceSnapshot

    /**
     * Register or heartbeat with the backend (best-effort).
     * Updates [DeviceSnapshot.remoteDeviceId] / [DeviceSnapshot.lastHeartbeat] when successful.
     */
    suspend fun syncWithBackend(): Result<DeviceSnapshot>
}
