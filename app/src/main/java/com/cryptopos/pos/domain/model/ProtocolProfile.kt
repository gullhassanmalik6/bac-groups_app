package com.cryptopos.pos.domain.model

/**
 * Application protocol profile configuration for the POS.
 *
 * Labels (101.x / 201.x / 202.x / MEIL ORD / …) come from merchant reference
 * material. They are **not** independently verified Visa/Mastercard wire specs
 * and must not be treated as official ISO message types until documented by a
 * licensed PSP/acquirer (`documented == true` only then).
 */
data class ProtocolProfile(
    val id: String,
    val code: String,
    val displayName: String,
    val description: String,
    val connectivityMode: ConnectivityMode,
    val authorizationMode: AuthorizationMode,
    val supportedTransactionTypes: Set<TerminalTransactionType>,
    val requiresOnlineAuthorization: Boolean,
    val sandboxOnly: Boolean,
    val environmentBadge: EnvironmentBadge = EnvironmentBadge.SANDBOX,
    val family: String,
    val digitGroup: Int? = null,
    val paymentMode: ProtocolPaymentMode = ProtocolPaymentMode.ONLINE,
    val sandboxOutcome: SandboxOutcome = SandboxOutcome.CAPTURE,
    val version: String = "1.0",
    val enabled: Boolean = true,
    /** True only when official processor documentation is wired. */
    val documented: Boolean = false,
    val allowsManualEntry: Boolean = true,
    val signatureLikely: Boolean = false,
    /** Short UI chip: Demo / Simulation (never implies live money while sandboxOnly). */
    val uiEnvironmentLabel: String = "Demo",
)

enum class ConnectivityMode {
    ONLINE,
    OFFLINE,
    OFFLINE_ONLINE,
    ONLINE_OFFLINE,
    SPECIAL,
}

enum class AuthorizationMode {
    SALE,
    PRE_AUTH,
    FORCE_POST,
    OFFLINE_AUTH,
    COMPLETION,
    MOTO,
    SPECIAL,
}

enum class TerminalTransactionType {
    SALE,
    REFUND,
    VOID,
    AUTH,
    COMPLETION,
}

enum class EnvironmentBadge {
    /** Visible SANDBOX / DEMO — no real money. */
    SANDBOX,
    /**
     * Reserved for a certified PSP adapter.
     * Informational only until `documented` and live credentials exist.
     */
    PROCESSOR_READY,
}

enum class ProtocolPaymentMode {
    ONLINE,
    OFFLINE_TEST,
}

/**
 * Expected sandbox processor outcome once MockPaymentProcessor is connected (Phase 4).
 * Not a live issuer response.
 */
enum class SandboxOutcome {
    CAPTURE,
    PRE_AUTH,
    FORCE_POST,
    OFFLINE_AUTH,
    COMPLETION,
    SIGNATURE,
}

data class TerminalSessionDraft(
    val amountRaw: String,
    val currency: String,
    val transactionType: TerminalTransactionType,
    val protocolId: String? = null,
)
