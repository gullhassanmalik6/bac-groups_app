package com.cryptopos.pos.hardware.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.printer.MockPrinterAdapter
import com.cryptopos.pos.domain.printer.PrinterAdapter
import com.cryptopos.pos.domain.printer.PrinterStatus
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
 * Sunmi thermal adapter via system AIDL. Fails closed when the service is absent.
 */
@Singleton
class SunmiPrinterAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrinterAdapter {
    private val mutex = Mutex()
    @Volatile private var service: IWoyouService? = null
    @Volatile private var connected: Boolean = false

    override fun printerName(): String = "SunmiPrinter"

    override suspend fun connect(): Result<Unit> = runCatching {
        ensureBound()
        connected = true
        Unit
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = {
            connected = false
            Result.failure(
                it as? PosError
                    ?: PosError.HardwareUnavailable("Sunmi printer unavailable", it),
            )
        },
    )

    override suspend fun printReceipt(lines: List<String>): Result<Unit> = mutex.withLock {
        try {
            MockPrinterAdapter.requireNoSensitiveCardData(lines)
            val printer = ensureBound()
            awaitCallback { printer.printerInit(it) }
            awaitCallback { printer.printText(lines.joinToString("\n") + "\n\n", it) }
            awaitCallback { printer.lineWrap(3, it) }
            runCatching { awaitCallback { printer.cutPaper(it) } }
            connected = true
            Result.success(Unit)
        } catch (error: Exception) {
            Timber.e(error, "Sunmi print failed")
            Result.failure(
                error as? PosError
                    ?: PosError.HardwareUnavailable("Sunmi printer unavailable", error),
            )
        }
    }

    override suspend fun printText(text: String): Result<Unit> =
        printReceipt(text.lines())

    override suspend fun feed(lines: Int): Result<Unit> = mutex.withLock {
        try {
            val printer = ensureBound()
            awaitCallback { printer.lineWrap(lines.coerceAtLeast(1), it) }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(
                error as? PosError
                    ?: PosError.HardwareUnavailable("Sunmi feed failed", error),
            )
        }
    }

    override suspend fun cut(): Result<Unit> = mutex.withLock {
        try {
            val printer = ensureBound()
            awaitCallback { printer.cutPaper(it) }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(
                error as? PosError
                    ?: PosError.HardwareUnavailable("Sunmi cut failed", error),
            )
        }
    }

    override suspend fun getStatus(): PrinterStatus =
        if (connected && service != null) PrinterStatus.ONLINE else PrinterStatus.OFFLINE

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
                        connected = false
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
                    connected = false
                }
            }
        }
    }

    private companion object {
        const val SUNMI_PACKAGE = "woyou.aidlservice.jiuiv5"
        const val SUNMI_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
    }
}

/**
 * Tries Sunmi first; falls back to [MockPrinterAdapter] on emulator / non-Sunmi devices.
 */
@Singleton
class ResolvingPrinterAdapter @Inject constructor(
    private val sunmi: SunmiPrinterAdapter,
    private val mock: MockPrinterAdapter,
) : PrinterAdapter {
    @Volatile
    private var active: PrinterAdapter = mock

    override fun printerName(): String = active.printerName()

    override suspend fun connect(): Result<Unit> {
        val sunmiResult = sunmi.connect()
        return if (sunmiResult.isSuccess) {
            active = sunmi
            sunmiResult
        } else {
            active = mock
            mock.connect()
        }
    }

    override suspend fun printReceipt(lines: List<String>): Result<Unit> {
        connect()
        val result = active.printReceipt(lines)
        if (result.isSuccess || active === mock) return result
        active = mock
        mock.connect()
        return mock.printReceipt(lines).also {
            if (it.isSuccess) {
                Timber.w("Sunmi print failed; used MockPrinterAdapter fallback")
            }
        }
    }

    override suspend fun printText(text: String): Result<Unit> {
        connect()
        val result = active.printText(text)
        if (result.isSuccess || active === mock) return result
        active = mock
        mock.connect()
        return mock.printText(text)
    }

    override suspend fun feed(lines: Int): Result<Unit> {
        connect()
        return active.feed(lines)
    }

    override suspend fun cut(): Result<Unit> {
        connect()
        return active.cut()
    }

    override suspend fun getStatus(): PrinterStatus {
        connect()
        return active.getStatus()
    }
}

/** Bridges [PrinterAdapter] to legacy [com.cryptopos.pos.domain.printer.PosPrinter]. */
@Singleton
class AdapterPosPrinter @Inject constructor(
    private val adapter: PrinterAdapter,
) : com.cryptopos.pos.domain.printer.PosPrinter {
    override suspend fun printReceipt(lines: List<String>): Result<Unit> {
        adapter.connect()
        val printed = adapter.printReceipt(lines)
        if (printed.isSuccess) {
            adapter.feed()
            runCatching { adapter.cut() }
        }
        return printed
    }

    override fun printerName(): String = adapter.printerName()

    override suspend fun isAvailable(): Boolean {
        adapter.connect()
        return adapter.getStatus() == PrinterStatus.ONLINE
    }
}

class EpsonPosPrinter : com.cryptopos.pos.domain.printer.PosPrinter {
    override suspend fun printReceipt(lines: List<String>): Result<Unit> =
        Result.failure(PosError.HardwareUnavailable("Epson printer adapter is not configured"))

    override fun printerName(): String = "EpsonPrinter"

    override suspend fun isAvailable(): Boolean = false
}

class StarPosPrinter : com.cryptopos.pos.domain.printer.PosPrinter {
    override suspend fun printReceipt(lines: List<String>): Result<Unit> =
        Result.failure(PosError.HardwareUnavailable("Star printer adapter is not configured"))

    override fun printerName(): String = "StarPrinter"

    override suspend fun isAvailable(): Boolean = false
}
