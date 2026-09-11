package com.cryptopos.pos.hardware.device

import android.os.Build
import android.provider.Settings
import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.core.network.ConnectivityObserver
import com.cryptopos.pos.data.local.datastore.SecureSessionStore
import com.cryptopos.pos.data.repository.TerminalRemoteRepository
import com.cryptopos.pos.domain.device.DeviceAdapter
import com.cryptopos.pos.domain.device.DeviceHealthRules
import com.cryptopos.pos.domain.device.DeviceHealthStatus
import com.cryptopos.pos.domain.device.DeviceIdentity
import com.cryptopos.pos.domain.device.DevicePeripheralProbe
import com.cryptopos.pos.domain.device.DeviceSnapshot
import com.cryptopos.pos.domain.device.PeripheralStatus
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.hardware.security.DeviceIntegrityChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [DeviceAdapter]: aggregates printer, NFC, connectivity, integrity.
 * Vendor SDKs stay behind existing adapters — this does not bind to a single OEM.
 */
@Singleton
class DefaultDeviceAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printer: PrinterAdapter,
    private val probe: DevicePeripheralProbe,
    private val connectivity: ConnectivityObserver,
    private val integrity: DeviceIntegrityChecker,
    private val remote: TerminalRemoteRepository,
    private val session: SecureSessionStore,
) : DeviceAdapter {

    @Volatile
    private var lastHeartbeat: Instant? = null

    override fun adapterName(): String = "DefaultDeviceAdapter(${printer.printerName()})"

    override suspend fun connect(): Result<Unit> = runCatching {
        printer.connect().getOrThrow()
        Unit
    }

    override suspend fun snapshot(): DeviceSnapshot {
        val identity = identity()
        val connectivityStatus = DeviceHealthRules.connectivity(connectivity.currentlyOnline())
        val printerStatus = probe.printerStatus()
        val cardStatus = probe.cardReaderStatus()
        val (overall, warnings) = DeviceHealthRules.overall(
            connectivity = connectivityStatus,
            printer = printerStatus,
            cardReader = cardStatus,
            integrityCompromised = integrity.isCompromised(),
            extraWarnings = integrity.findings().map { "integrity:$it" },
        )
        return DeviceSnapshot(
            identity = identity,
            overallStatus = overall,
            connectivityStatus = connectivityStatus,
            printerStatus = printerStatus,
            cardReaderStatus = cardStatus,
            lastHeartbeat = lastHeartbeat,
            remoteDeviceId = session.getDeviceId(),
            adapterName = adapterName(),
            warnings = warnings,
        )
    }

    override suspend fun syncWithBackend(): Result<DeviceSnapshot> = runCatching {
        val local = snapshot()
        val id = local.identity
        remote.registerDeviceBestEffort(
            serial = id.serialNumber,
            model = id.model,
            androidVersion = id.androidVersion,
            appVersion = id.appVersion,
            manufacturer = id.manufacturer,
            connectivityStatus = local.connectivityStatus.name,
            printerStatus = local.printerStatus.name,
            cardReaderStatus = local.cardReaderStatus.name,
            overallStatus = local.overallStatus.name,
        )?.let { remoteId ->
            session.saveDeviceId(remoteId)
        }
        lastHeartbeat = Instant.now()
        remote.heartbeatBestEffort(
            deviceId = session.getDeviceId(),
            serial = id.serialNumber,
            connectivityStatus = local.connectivityStatus.name,
            printerStatus = local.printerStatus.name,
            cardReaderStatus = local.cardReaderStatus.name,
            overallStatus = local.overallStatus.name,
        )
        snapshot().copy(lastHeartbeat = lastHeartbeat)
    }

    private fun identity(): DeviceIdentity {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        val serial = androidId?.takeIf { it.isNotBlank() }
            ?: "POS-${(Build.FINGERPRINT + Build.MODEL).hashCode().toUInt().toString(16)}"
        return DeviceIdentity(
            serialNumber = serial.take(64),
            manufacturer = Build.MANUFACTURER ?: "Generic",
            model = Build.MODEL ?: "Android POS",
            firmware = Build.DISPLAY,
            appVersion = BuildConfig.VERSION_NAME,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
        )
    }
}

/**
 * Emulator / unit-test adapter: always healthy mock peripherals.
 * Used when explicitly bound in tests; production uses [DefaultDeviceAdapter].
 */
class MockDeviceAdapter(
    private val serial: String = "MOCK-${UUID.randomUUID().toString().take(8)}",
    private var overall: DeviceHealthStatus = DeviceHealthStatus.ONLINE,
) : DeviceAdapter {
    override fun adapterName(): String = "MockDeviceAdapter"

    override suspend fun connect(): Result<Unit> = Result.success(Unit)

    override suspend fun snapshot(): DeviceSnapshot = DeviceSnapshot(
        identity = DeviceIdentity(
            serialNumber = serial,
            manufacturer = "MockOEM",
            model = "Mock Terminal",
            firmware = "mock-1.0",
            appVersion = "1.0.0-test",
            androidVersion = "14",
        ),
        overallStatus = overall,
        connectivityStatus = DeviceHealthStatus.ONLINE,
        printerStatus = PeripheralStatus.ONLINE,
        cardReaderStatus = PeripheralStatus.ONLINE,
        lastHeartbeat = Instant.now(),
        remoteDeviceId = null,
        adapterName = adapterName(),
        warnings = if (overall == DeviceHealthStatus.WARNING) listOf("mock-warning") else emptyList(),
    )

    override suspend fun syncWithBackend(): Result<DeviceSnapshot> =
        Result.success(snapshot())

    fun setOverall(status: DeviceHealthStatus) {
        overall = status
    }
}
