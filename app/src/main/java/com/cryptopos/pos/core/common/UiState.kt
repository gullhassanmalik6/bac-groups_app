package com.cryptopos.pos.core.common

import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.error.toPosError

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun Throwable.toUserMessage(): String = toPosError().message

fun PosError.toUserMessage(): String = message
