package com.cryptopos.pos.features.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopos.pos.core.common.toUserMessage
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.repository.SettingsRepository
import com.cryptopos.pos.domain.usecase.ProcessPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PaymentPhase { INPUT, WAITING_CARD, PROCESSING, SUCCESS, FAILURE }

data class PaymentUiState(
    val amount: String = "",
    val currency: String = "SAR",
    val phase: PaymentPhase = PaymentPhase.INPUT,
    val message: String? = null,
    val transaction: PaymentTransaction? = null,
    val queuedOffline: Boolean = false,
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val processPayment: ProcessPaymentUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state
    private var paymentJob: Job? = null

    init {
        viewModelScope.launch {
            val currency = settingsRepository.settings.first().currency
            _state.update { it.copy(currency = currency) }
        }
    }

    fun onAmountChange(value: String) {
        if (value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _state.update { it.copy(amount = value, message = null) }
        }
    }

    fun setCurrency(value: String) {
        _state.update { it.copy(currency = value) }
        viewModelScope.launch { settingsRepository.setCurrency(value) }
    }

    fun appendDigit(digit: String) {
        val next = _state.value.amount + digit
        onAmountChange(next)
    }

    fun backspace() {
        _state.update { it.copy(amount = it.amount.dropLast(1), message = null) }
    }

    fun clearAmount() {
        _state.update { it.copy(amount = "", message = null) }
    }

    fun cancel() {
        paymentJob?.cancel()
        paymentJob = null
        _state.update {
            it.copy(
                phase = PaymentPhase.INPUT,
                message = null,
                transaction = null,
                queuedOffline = false,
            )
        }
    }

    fun startPayment() {
        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            _state.update {
                it.copy(phase = PaymentPhase.WAITING_CARD, message = "Waiting for card…", queuedOffline = false)
            }
            try {
                _state.update { it.copy(phase = PaymentPhase.WAITING_CARD, message = "Waiting for card…") }
                val result = processPayment(
                    amountRaw = _state.value.amount,
                    currency = _state.value.currency,
                    waitForCard = true,
                )
                _state.update {
                    it.copy(
                        phase = PaymentPhase.SUCCESS,
                        transaction = result.transaction,
                        queuedOffline = result.queuedOffline,
                        message = if (result.queuedOffline) {
                            "Payment queued offline"
                        } else {
                            "Payment ${result.transaction.status}"
                        },
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(phase = PaymentPhase.FAILURE, message = error.toUserMessage())
                }
            }
        }
    }
}
