package com.cryptopos.pos.features.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.processor.MockScenario
import com.cryptopos.pos.domain.terminal.TerminalPaymentOrchestrator
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalSessionManager
import com.cryptopos.pos.domain.terminal.TerminalSessionResult
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import com.cryptopos.pos.domain.usecase.PrintTerminalReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalProcessingUiState(
    val phase: Phase = Phase.PROCESSING,
    val session: TerminalSession? = null,
    val error: String? = null,
    val printMessage: String? = null,
) {
    enum class Phase { PROCESSING, RESULT }
}

@HiltViewModel
class TerminalProcessingViewModel @Inject constructor(
    private val orchestrator: TerminalPaymentOrchestrator,
    private val sessionManager: TerminalSessionManager,
    private val printTerminalReceipt: PrintTerminalReceiptUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        TerminalProcessingUiState(session = sessionManager.current()),
    )
    val state: StateFlow<TerminalProcessingUiState> = _state

    init {
        runMockAuthorization()
    }

    fun runMockAuthorization(scenario: MockScenario? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(phase = TerminalProcessingUiState.Phase.PROCESSING, error = null, printMessage = null)
            }
            val token = when (scenario) {
                MockScenario.DECLINED -> "pm_test_visa_declined"
                MockScenario.TIMEOUT -> "pm_test_timeout"
                MockScenario.CANCELLED -> "pm_test_cancelled"
                MockScenario.ERROR -> "pm_test_error"
                MockScenario.SIGNATURE -> "pm_test_visa_signature"
                else -> "pm_test_visa_success"
            }
            when (val result = orchestrator.runAuthorization(token, scenario)) {
                is TerminalSessionResult.Ok -> {
                    _state.update {
                        it.copy(
                            phase = TerminalProcessingUiState.Phase.RESULT,
                            session = result.session,
                            error = null,
                        )
                    }
                    if (isSuccess(result.session)) {
                        autoPrint(result.session)
                    }
                }
                is TerminalSessionResult.Err -> {
                    _state.update {
                        it.copy(
                            phase = TerminalProcessingUiState.Phase.RESULT,
                            session = result.session ?: sessionManager.current(),
                            error = result.message,
                        )
                    }
                }
            }
        }
    }

    fun printReceipt() {
        val session = _state.value.session ?: return
        viewModelScope.launch { autoPrint(session, userInitiated = true) }
    }

    private suspend fun autoPrint(session: TerminalSession, userInitiated: Boolean = false) {
        val result = printTerminalReceipt.printSession(session)
        _state.update {
            it.copy(
                printMessage = result.fold(
                    onSuccess = { receipt ->
                        "Printed via ${receipt.printerName ?: "printer"}"
                    },
                    onFailure = { err ->
                        if (userInitiated) err.toUserMessage() else null
                    },
                ),
            )
        }
    }

    fun receiptSessionId(): String? {
        val s = _state.value.session ?: return null
        return s.remoteSessionId ?: s.id
    }

    fun isSuccess(session: TerminalSession?): Boolean {
        val s = session?.state ?: return false
        return s == TerminalTransactionState.APPROVED ||
            s == TerminalTransactionState.CAPTURED ||
            s == TerminalTransactionState.COMPLETED
    }

    fun finish() {
        sessionManager.clear()
    }
}
