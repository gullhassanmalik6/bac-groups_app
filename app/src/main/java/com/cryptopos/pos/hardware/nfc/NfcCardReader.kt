package com.cryptopos.pos.hardware.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.payment.CardReader
import com.cryptopos.pos.domain.payment.CardTapEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Holds the foreground Activity so NFC reader mode can be enabled during payment.
 */
@Singleton
class NfcActivityHolder @Inject constructor() {
    private val ref = AtomicReference<WeakReference<Activity>?>(null)

    fun attach(activity: Activity) {
        ref.set(WeakReference(activity))
    }

    fun detach(activity: Activity) {
        val current = ref.get()?.get()
        if (current === activity) ref.set(null)
    }

    fun current(): Activity? = ref.get()?.get()
}

@Singleton
class NfcCardReader @Inject constructor(
    private val activityHolder: NfcActivityHolder,
) : CardReader {

    override fun isNfcAvailable(): Boolean {
        val activity = activityHolder.current() ?: return false
        return NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true
    }

    override suspend fun waitForCardTap(timeoutMs: Long): Result<CardTapEvent> =
        withContext(Dispatchers.Main) {
            val activity = activityHolder.current()
                ?: return@withContext Result.failure(
                    PosError.HardwareUnavailable("NFC host activity is not available"),
                )
            val adapter = NfcAdapter.getDefaultAdapter(activity)
                ?: return@withContext Result.failure(
                    PosError.HardwareUnavailable("NFC is not available on this device"),
                )
            if (!adapter.isEnabled) {
                return@withContext Result.failure(
                    PosError.HardwareUnavailable("NFC is disabled. Enable NFC to accept cards."),
                )
            }

            val result = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val callback = NfcAdapter.ReaderCallback { tag ->
                        if (!cont.isActive) return@ReaderCallback
                        val event = tag.toCardTapEvent()
                        cont.resume(Result.success(event))
                    }
                    adapter.enableReaderMode(
                        activity,
                        callback,
                        NfcAdapter.FLAG_READER_NFC_A or
                            NfcAdapter.FLAG_READER_NFC_B or
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                        null,
                    )
                    cont.invokeOnCancellation {
                        runCatching { adapter.disableReaderMode(activity) }
                    }
                }
            }

            runCatching { adapter.disableReaderMode(activity) }
            result ?: Result.failure(PosError.HardwareUnavailable("Card tap timed out"))
        }
}

private fun Tag.toCardTapEvent(): CardTapEvent {
    val idHex = id.joinToString("") { "%02X".format(it) }
    val masked = if (idHex.length >= 4) "**** **** **** ${idHex.takeLast(4)}" else "****"
    val brand = when {
        techList.any { it.contains("IsoDep", ignoreCase = true) } -> "contactless"
        else -> "nfc"
    }
    runCatching {
        IsoDep.get(this)?.use { iso ->
            if (!iso.isConnected) iso.connect()
            delayBlocking(50)
        }
    }
    return CardTapEvent(brandHint = brand, maskedPan = masked)
}

private fun delayBlocking(ms: Long) {
    try {
        Thread.sleep(ms)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

/**
 * Emulator / CI fallback that waits briefly then fails closed — never reports fake cards.
 * Bound only when NFC hardware is absent at DI time is avoided; NfcCardReader itself fails closed.
 * This class exists for instrumented tests via Hilt test modules if needed.
 */
class TimeoutCardReader : CardReader {
    override fun isNfcAvailable(): Boolean = false

    override suspend fun waitForCardTap(timeoutMs: Long): Result<CardTapEvent> {
        delay(timeoutMs.coerceAtMost(1_000))
        return Result.failure(PosError.HardwareUnavailable("No card reader available"))
    }
}
