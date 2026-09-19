package com.cryptopos.pos.domain.terminal

import com.cryptopos.pos.BuildConfig
import com.cryptopos.pos.data.repository.TerminalRemoteGateway
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.error.toPosError
import com.cryptopos.pos.domain.model.SandboxOutcome
import com.cryptopos.pos.domain.processor.MockScenario
import com.cryptopos.pos.domain.processor.PaymentProcessor
import com.cryptopos.pos.domain.processor.ProcessorAuthorizeRequest
import com.cryptopos.pos.domain.processor.ProcessorCaptureRequest
import com.cryptopos.pos.domain.processor.ProcessorCompleteRequest
import com.cryptopos.pos.domain.processor.ProcessorStatus
import com.cryptopos.pos.domain.protocol.ProtocolExecutionGuard
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import com.cryptopos.pos.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prefer backend terminal session APIs (Phase 5/6). Fall back to on-device [PaymentProcessor] if remote fails.
 * Production never dummy-approves undocumented protocol catalog labels.
 */
@Singleton
class TerminalPaymentOrchestrator @Inject constructor(
    private val sessionManager: TerminalSessionManager,
    private val processor: PaymentProcessor,
    private val protocolCatalog: ProtocolProfileCatalog,
    private val remoteRepository: TerminalRemoteGateway,
    private val historyRepository: HistoryRepository,
) {

    /**
     * PROTOCOL_SELECTED → authorize via API (or local mock) → final session state.
     */
    suspend fun runAuthorization(
        paymentMethodToken: String = "pm_test_visa_success",
        scenarioOverride: MockScenario? = null,
    ): TerminalSessionResult {
        val session = sessionManager.current()
            ?: return TerminalSessionResult.Err("No active session", null)
        if (session.state != TerminalTransactionState.PROTOCOL_SELECTED) {
            return TerminalSessionResult.Err(
                "Authorize requires PROTOCOL_SELECTED (was ${session.state})",
                session,
            )
        }

        val profile = session.protocolId?.let { protocolCatalog.get(it) }
            ?: session.protocolCode?.let { code ->
                protocolCatalog.all().firstOrNull { it.code == code }
            }
        val documented = profile?.documented == true
        if (!ProtocolExecutionGuard.allowSandboxSimulation(BuildConfig.PAYMENT_ENVIRONMENT, documented)) {
            return TerminalSessionResult.Err(
                ProtocolExecutionGuard.PROVIDER_CONFIGURATION_REQUIRED,
                session,
            )
        }

        if (remoteRepository.remoteEnabled) {
            val remote = runCatching {
                runRemoteAuthorization(session, paymentMethodToken, scenarioOverride)
            }
            remote.getOrNull()?.let { return it }
            val err = remote.exceptionOrNull()?.toPosError()
            if (err is PosError.Server || err is PosError.Gateway) {
                // Fall through to local mock.
            }
        }

        return runLocalAuthorization(paymentMethodToken, scenarioOverride)
    }

    private suspend fun runRemoteAuthorization(
        session: TerminalSession,
        paymentMethodToken: String,
        scenarioOverride: MockScenario?,
    ): TerminalSessionResult {
        sessionManager.presentCard().let { if (it is TerminalSessionResult.Err) return it }
        sessionManager.startAuthorizing().let { if (it is TerminalSessionResult.Err) return it }

        val current = sessionManager.current()!!
        val amountMinor = current.amountMinor
            ?: return TerminalSessionResult.Err("Missing amount", current)
        val currency = current.currency
            ?: return TerminalSessionResult.Err("Missing currency", current)
        val txnType = current.transactionType
            ?: return TerminalSessionResult.Err("Missing transaction type", current)

        val remoteSession = remoteRepository.createAndAuthorize(
            amountMinor = amountMinor,
            currency = currency,
            transactionType = txnType.name,
            protocolCode = current.protocolId ?: current.protocolCode,
            paymentMethodToken = paymentMethodToken,
            scenario = scenarioOverride?.name?.lowercase(),
        )
        val annotated = remoteSession.copy(
            processorMessage = listOfNotNull(
                remoteSession.processorMessage,
                "Authorized via backend API",
            ).joinToString(" · "),
        )
        sessionManager.applyRemoteSnapshot(annotated)
        recordHistory(annotated)
        return TerminalSessionResult.Ok(annotated)
    }

    private suspend fun runLocalAuthorization(
        paymentMethodToken: String,
        scenarioOverride: MockScenario?,
    ): TerminalSessionResult {
        // Reset to PROTOCOL_SELECTED if we already advanced for a failed remote attempt.
        val current = sessionManager.current()
            ?: return TerminalSessionResult.Err("No active session", null)
        if (current.state != TerminalTransactionState.PROTOCOL_SELECTED) {
            // Re-enter local path only from AUTHORIZING/CARD_PRESENTED after failed remote.
            if (current.state == TerminalTransactionState.AUTHORIZING ||
                current.state == TerminalTransactionState.CARD_PRESENTED
            ) {
                // Force snapshot back for local retry.
                sessionManager.applyRemoteSnapshot(
                    current.copy(state = TerminalTransactionState.PROTOCOL_SELECTED),
                )
            } else if (current.state != TerminalTransactionState.PROTOCOL_SELECTED) {
                return TerminalSessionResult.Err(
                    "Cannot fall back to local mock from ${current.state}",
                    current,
                )
            }
        }

        sessionManager.presentCard().let { if (it is TerminalSessionResult.Err) return it }
        sessionManager.startAuthorizing().let { if (it is TerminalSessionResult.Err) return it }

        val live = sessionManager.current()!!
        val outcome = live.protocolId
            ?.let { protocolCatalog.get(it)?.sandboxOutcome }
            ?: live.protocolSandboxOutcome?.let { runCatching { SandboxOutcome.valueOf(it) }.getOrNull() }

        val processorResult = try {
            processor.authorize(
                ProcessorAuthorizeRequest(
                    sessionId = live.id,
                    amountMinor = live.amountMinor ?: 0L,
                    currency = live.currency ?: "CAD",
                    transactionType = live.transactionType
                        ?: return TerminalSessionResult.Err("Missing transaction type", live),
                    protocolId = live.protocolId,
                    protocolCode = live.protocolCode,
                    sandboxOutcome = outcome,
                    paymentMethodToken = paymentMethodToken,
                    scenarioOverride = scenarioOverride,
                ),
            )
        } catch (ex: Exception) {
            sessionManager.fail(ex.message ?: "Processor error")
            return TerminalSessionResult.Err(ex.message ?: "Processor error", sessionManager.current())
        }

        val withMeta: (TerminalSession) -> TerminalSession = { s ->
            s.copy(
                authorizationCode = processorResult.authorizationCode,
                processorReference = processorResult.processorReference,
                processorStatus = processorResult.status.name,
                signatureRequired = processorResult.signatureRequired,
                cardBrand = processorResult.cardBrand,
                cardLast4 = processorResult.cardLast4,
                processorMessage = listOfNotNull(
                    processorResult.message,
                    "Local mock (API unavailable)",
                ).joinToString(" · "),
                usedRemoteProcessor = false,
            )
        }

        return when (processorResult.status) {
            ProcessorStatus.APPROVED -> {
                val result = applyApproved(processorResult.processorReference, outcome, withMeta)
                recordHistoryFromResult(result)
                result
            }
            ProcessorStatus.DECLINED ->
                sessionManager.decline(processorResult.message ?: "Declined").mapSession(withMeta)
                    .also { recordHistoryFromResult(it) }
            ProcessorStatus.TIMEOUT, ProcessorStatus.ERROR ->
                sessionManager.fail(processorResult.message ?: processorResult.status.name).mapSession(withMeta)
                    .also { recordHistoryFromResult(it) }
            ProcessorStatus.CANCELLED ->
                sessionManager.cancel(processorResult.message ?: "Cancelled").mapSession(withMeta)
                    .also { recordHistoryFromResult(it) }
            ProcessorStatus.PROCESSING ->
                TerminalSessionResult.Err("Processor still processing", sessionManager.current())
        }
    }

    private suspend fun recordHistory(session: TerminalSession) {
        runCatching { historyRepository.recordTerminalSession(session) }
    }

    private suspend fun recordHistoryFromResult(result: TerminalSessionResult) {
        val session = when (result) {
            is TerminalSessionResult.Ok -> result.session
            is TerminalSessionResult.Err -> result.session
        } ?: return
        recordHistory(session)
    }

    private suspend fun applyApproved(
        processorReference: String,
        outcome: SandboxOutcome?,
        withMeta: (TerminalSession) -> TerminalSession,
    ): TerminalSessionResult {
        val approved = sessionManager.approve("Processor APPROVED (SANDBOX)")
            .mapSession(withMeta)
        if (approved is TerminalSessionResult.Err) return approved

        val session = sessionManager.current()!!
        return when (outcome) {
            SandboxOutcome.PRE_AUTH, SandboxOutcome.OFFLINE_AUTH, SandboxOutcome.SIGNATURE -> approved
            SandboxOutcome.COMPLETION -> {
                processor.completeAuthorization(
                    ProcessorCompleteRequest(
                        sessionId = session.id,
                        processorReference = processorReference,
                        amountMinor = session.amountMinor ?: 0L,
                        currency = session.currency ?: "CAD",
                    ),
                )
                sessionManager.complete("Completion (SANDBOX)").mapSession(withMeta)
            }
            SandboxOutcome.FORCE_POST, SandboxOutcome.CAPTURE, null -> {
                processor.capture(
                    ProcessorCaptureRequest(
                        sessionId = session.id,
                        processorReference = processorReference,
                        amountMinor = session.amountMinor ?: 0L,
                        currency = session.currency ?: "CAD",
                    ),
                )
                val captured = sessionManager.capture("Captured (SANDBOX)").mapSession(withMeta)
                if (captured is TerminalSessionResult.Err) return captured
                sessionManager.complete("Completed (SANDBOX)").mapSession(withMeta)
            }
        }
    }

    private fun TerminalSessionResult.mapSession(
        transform: (TerminalSession) -> TerminalSession,
    ): TerminalSessionResult = when (this) {
        is TerminalSessionResult.Ok -> {
            val updated = transform(session)
            sessionManager.replaceMetadata(updated)
            TerminalSessionResult.Ok(sessionManager.current() ?: updated)
        }
        is TerminalSessionResult.Err -> {
            session?.let { sessionManager.replaceMetadata(transform(it)) }
            TerminalSessionResult.Err(message, sessionManager.current())
        }
    }
}
