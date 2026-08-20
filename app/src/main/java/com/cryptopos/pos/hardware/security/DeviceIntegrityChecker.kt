package com.cryptopos.pos.hardware.security

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceIntegrityChecker {
    fun isCompromised(): Boolean
    fun findings(): List<String>
}

/**
 * Soft root / tamper signals. Enforcement is controlled by BuildConfig.ENFORCE_DEVICE_INTEGRITY.
 */
@Singleton
class DefaultDeviceIntegrityChecker @Inject constructor() : DeviceIntegrityChecker {
    override fun isCompromised(): Boolean = findings().isNotEmpty()

    override fun findings(): List<String> = buildList {
        if (Build.TAGS?.contains("test-keys") == true) add("test-keys")
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
        )
        if (paths.any { File(it).exists() }) add("su-binary")
    }
}
