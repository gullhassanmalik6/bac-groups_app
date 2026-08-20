package com.cryptopos.pos.domain.error

/**
 * Typed domain errors for POS flows. Presentation maps these to user-facing copy.
 */
sealed class PosError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    class Validation(message: String) : PosError(message)

    class SessionExpired(message: String = "Your session expired. Please sign in again.") : PosError(message)

    class NetworkUnavailable(message: String = "No network connection.") : PosError(message)

    class OfflineQueued(message: String = "Payment saved offline and will sync when online.") : PosError(message)

    class Server(message: String, cause: Throwable? = null) : PosError(message, cause)

    class Gateway(message: String, cause: Throwable? = null) : PosError(message, cause)

    class HardwareUnavailable(message: String, cause: Throwable? = null) : PosError(message, cause)

    class Cancelled(message: String = "Operation cancelled.") : PosError(message)

    class Unknown(message: String = "Something went wrong. Please try again.", cause: Throwable? = null) :
        PosError(message, cause)
}

fun Throwable.toPosError(): PosError = when (this) {
    is PosError -> this
    is java.net.UnknownHostException,
    is java.net.ConnectException,
    is java.net.SocketTimeoutException,
    -> PosError.NetworkUnavailable(message ?: "No network connection.")
    else -> PosError.Unknown(message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.", this)
}
