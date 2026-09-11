package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.device.DeviceAdapter
import com.cryptopos.pos.domain.device.DeviceSnapshot
import javax.inject.Inject

class SyncDeviceUseCase @Inject constructor(
    private val deviceAdapter: DeviceAdapter,
) {
    suspend operator fun invoke(): DeviceSnapshot {
        deviceAdapter.connect()
        return deviceAdapter.syncWithBackend().getOrElse { deviceAdapter.snapshot() }
    }
}

class ObserveDeviceSnapshotUseCase @Inject constructor(
    private val deviceAdapter: DeviceAdapter,
) {
    suspend operator fun invoke(): DeviceSnapshot = deviceAdapter.snapshot()
}
