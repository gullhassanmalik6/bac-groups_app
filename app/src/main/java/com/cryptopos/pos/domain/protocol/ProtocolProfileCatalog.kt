package com.cryptopos.pos.domain.protocol

import com.cryptopos.pos.domain.model.ProtocolProfile
import com.cryptopos.pos.domain.model.TerminalTransactionType

sealed class ProtocolSelectionValidation {
    data class Ok(val profile: ProtocolProfile) : ProtocolSelectionValidation()
    data class Err(val message: String) : ProtocolSelectionValidation()
}

/**
 * Catalog + validation for protocol profiles (Phase 3).
 */
interface ProtocolProfileCatalog {
    fun all(): List<ProtocolProfile>
    fun get(id: String): ProtocolProfile?
    fun forTransactionType(type: TerminalTransactionType): List<ProtocolProfile>
    fun validateSelection(
        protocolId: String,
        transactionType: TerminalTransactionType,
    ): ProtocolSelectionValidation
}

object ProtocolProfileValidator {
    fun validate(
        profile: ProtocolProfile?,
        transactionType: TerminalTransactionType,
    ): ProtocolSelectionValidation {
        if (profile == null) {
            return ProtocolSelectionValidation.Err("Unknown protocol profile")
        }
        if (!profile.enabled) {
            return ProtocolSelectionValidation.Err("Protocol ${profile.code} is disabled")
        }
        if (transactionType !in profile.supportedTransactionTypes) {
            return ProtocolSelectionValidation.Err(
                "Protocol ${profile.displayName} does not support $transactionType",
            )
        }
        if (profile.sandboxOnly && profile.environmentBadge == com.cryptopos.pos.domain.model.EnvironmentBadge.PROCESSOR_READY && !profile.documented) {
            // Defensive: PROCESSOR_READY without docs must not be treated as live.
            return ProtocolSelectionValidation.Err(
                "Protocol marked processor-ready but not documented — blocked",
            )
        }
        return ProtocolSelectionValidation.Ok(profile)
    }
}
