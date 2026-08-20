package com.cryptopos.pos.hardware.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.printer.PosPrinter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import woyou.aidlservice.jiuiv5.ICallback
import woyou.aidlservice.jiuiv5.IWoyouService
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production Sunmi printer adapter via system AIDL service.
 * Fails closed with [PosError.HardwareUnavailable] when the service is absent.
 */
@Singleton
class SunmiPosPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
) : PosPrinter {
    private val mutex = Mutex()
    @Volatile private var service: IWoyouService? = null

    override fun printerName(): String = "SunmiPrinter"

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.Main) {
        runCatching { ensureBound() }.isSuccess
    }

    override suspend fun printReceipt(lines: List<String>): Result<Unit> = mutex.withLock {
        try {
            val printer = ensureBound()
            awaitCallback { printer.printerInit(it) }
            awaitCallback { printer.printText(lines.joinToString("\n") + "\n\n", it) }
            awaitCallback { printer.lineWrap(3, it) }
            runCatching { awaitCallback { printer.cutPaper(it) } }
            Result.success(Unit)
        } catch (error: Exception) {
            Timber.e(error, "Sunmi print failed")
            Result.failure(
                error as? PosError
                    ?: PosError.HardwareUnavailable("Sunmi printer unavailable", error),
            )
        }
    }

    private suspend fun awaitCallback(block: (ICallback) -> Unit) {
        suspendCancellableCoroutine { cont ->
            val completed = AtomicBoolean(false)
            val callback = object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean) {
                    if (!completed.compareAndSet(false, true) || !cont.isActive) return
                    if (isSuccess) cont.resume(Unit)
                    else cont.resumeWithException(PosError.HardwareUnavailable("Printer reported failure"))
                }

                override fun onReturnString(result: String?) = Unit

                override fun onRaiseException(code: Int, msg: String?) {
                    if (!completed.compareAndSet(false, true) || !cont.isActive) return
                    cont.resumeWithException(
                        PosError.HardwareUnavailable(msg ?: "Printer exception $code"),
                    )
                }
            }
            block(callback)
        }
    }

    private suspend fun ensureBound(): IWoyouService {
        service?.let { return it }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val svc = IWoyouService.Stub.asInterface(binder)
                        service = svc
                        if (cont.isActive) cont.resume(svc)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        service = null
                    }
                }
                val intent = Intent().apply {
                    setPackage(SUNMI_PACKAGE)
                    action = SUNMI_ACTION
                }
                val bound = context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
                if (!bound && cont.isActive) {
                    cont.resumeWithException(
                        PosError.HardwareUnavailable(
                            "Sunmi printer service not found on this device",
                        ),
                    )
                }
                cont.invokeOnCancellation {
                    runCatching { context.unbindService(conn) }
                    service = null
                }
            }
        }
    }

    private companion object {
        const val SUNMI_PACKAGE = "woyou.aidlservice.jiuiv5"
        const val SUNMI_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
    }
}

class EpsonPosPrinter : PosPrinter {
    override suspend fun printReceipt(lines: List<String>): Result<Unit> =
        Result.failure(PosError.HardwareUnavailable("Epson printer adapter is not configured"))

    override fun printerName(): String = "EpsonPrinter"

    override suspend fun isAvailable(): Boolean = false
}

class StarPosPrinter : PosPrinter {
    override suspend fun printReceipt(lines: List<String>): Result<Unit> =
        Result.failure(PosError.HardwareUnavailable("Star printer adapter is not configured"))

    override fun printerName(): String = "StarPrinter"

    override suspend fun isAvailable(): Boolean = false
}
