package com.cryptopos.pos.data.mock

import com.cryptopos.pos.domain.model.AuthorizationMode
import com.cryptopos.pos.domain.model.ConnectivityMode
import com.cryptopos.pos.domain.model.EnvironmentBadge
import com.cryptopos.pos.domain.model.ProtocolPaymentMode
import com.cryptopos.pos.domain.model.ProtocolProfile
import com.cryptopos.pos.domain.model.SandboxOutcome
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.protocol.ProtocolProfileCatalog
import com.cryptopos.pos.domain.protocol.ProtocolProfileValidator
import com.cryptopos.pos.domain.protocol.ProtocolSelectionValidation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Static protocol profile catalog (sandbox / demo).
 * No card-network host messages — configuration objects only.
 */
@Singleton
class DefaultProtocolProfileCatalog @Inject constructor() : ProtocolProfileCatalog {

    override fun all(): List<ProtocolProfile> = Catalog.all.filter { it.enabled }

    override fun get(id: String): ProtocolProfile? = Catalog.byId(id)?.takeIf { it.enabled }

    override fun forTransactionType(type: TerminalTransactionType): List<ProtocolProfile> {
        val filtered = all().filter { type in it.supportedTransactionTypes }
        return filtered.ifEmpty { all() }
    }

    override fun validateSelection(
        protocolId: String,
        transactionType: TerminalTransactionType,
    ): ProtocolSelectionValidation =
        ProtocolProfileValidator.validate(get(protocolId), transactionType)
}

/**
 * Back-compat accessor used by UI helpers; prefer [DefaultProtocolProfileCatalog] via DI.
 */
object MockProtocols {
    val all: List<ProtocolProfile> get() = Catalog.all
    fun byId(id: String): ProtocolProfile? = Catalog.byId(id)
}

private object Catalog {
    private val saleFamily = setOf(
        TerminalTransactionType.SALE,
        TerminalTransactionType.REFUND,
        TerminalTransactionType.VOID,
        TerminalTransactionType.AUTH,
        TerminalTransactionType.COMPLETION,
    )

    private val authFamily = setOf(
        TerminalTransactionType.AUTH,
        TerminalTransactionType.COMPLETION,
        TerminalTransactionType.VOID,
    )

    val all: List<ProtocolProfile> = listOf(
        profile("101.1-6dg", "101.1", "Online 6 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 6, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.1-4dg", "101.1", "Online 4 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 4, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.2", "101.2", "Cloud Sale 6 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 6, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.3-6dg", "101.3", "Cloud Purchase 6 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 6, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.3-4dg", "101.3", "Cloud Purchase 4 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 4, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.4-6dg", "101.4", "Cloud Purchase 6 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 6, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.4-4dg", "101.4", "Cloud Purchase 4 DG", ConnectivityMode.ONLINE, AuthorizationMode.SALE, true, digitGroup = 4, outcome = SandboxOutcome.CAPTURE, ui = "Demo"),
        profile("101.5", "101.5", "MOTO / MO/TO Cloud", ConnectivityMode.ONLINE, AuthorizationMode.MOTO, true, outcome = SandboxOutcome.CAPTURE, ui = "Demo", allowsManual = true),
        profile("101.6", "101.6", "Pre-Auth", ConnectivityMode.ONLINE, AuthorizationMode.PRE_AUTH, true, types = authFamily, outcome = SandboxOutcome.PRE_AUTH, ui = "Demo"),
        profile("101.7", "101.7", "Force Post", ConnectivityMode.ONLINE, AuthorizationMode.FORCE_POST, true, outcome = SandboxOutcome.FORCE_POST, ui = "Demo"),
        profile(
            "201", "201", "Offline Auth", ConnectivityMode.OFFLINE, AuthorizationMode.OFFLINE_AUTH, false,
            types = authFamily,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "201.1", "201.1", "Cloud Completion", ConnectivityMode.OFFLINE_ONLINE, AuthorizationMode.COMPLETION, true,
            types = setOf(TerminalTransactionType.COMPLETION, TerminalTransactionType.VOID),
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.COMPLETION,
            ui = "Demo",
        ),
        profile(
            "201.2", "201.2", "Offline Post", ConnectivityMode.OFFLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "201.3", "201.3", "Offline 6 DG", ConnectivityMode.ONLINE_OFFLINE, AuthorizationMode.OFFLINE_AUTH, false,
            types = authFamily,
            digitGroup = 6,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.SIGNATURE,
            ui = "Demo",
            signatureLikely = true,
        ),
        profile(
            "201.5", "201.5", "Offline (reserved)", ConnectivityMode.OFFLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
            description = "Listed in merchant reference set — sandbox placeholder until PSP docs.",
        ),
        profile(
            "201.6", "201.6", "Offline-Online", ConnectivityMode.OFFLINE_ONLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "202.2", "202.2", "Offline-Online", ConnectivityMode.OFFLINE_ONLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "202.5", "202.5", "Offline-Online", ConnectivityMode.OFFLINE_ONLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "202.9", "202.9", "Offline-Online", ConnectivityMode.OFFLINE_ONLINE, AuthorizationMode.OFFLINE_AUTH, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.OFFLINE_AUTH,
            ui = "Simulation",
        ),
        profile(
            "MEIL_ORD", "MEIL ORD", "Special profile", ConnectivityMode.SPECIAL, AuthorizationMode.SPECIAL, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.CAPTURE,
            ui = "Demo",
            family = "SPECIAL",
            description = "Reference label from terminal receipt — not a card-network message type.",
        ),
        profile(
            "PLATFORMA_INVOIS", "PLATFORMA INVOIS", "Invoice platform", ConnectivityMode.SPECIAL, AuthorizationMode.SPECIAL, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.CAPTURE,
            ui = "Demo",
            family = "SPECIAL",
            description = "Reference label from terminal receipt — sandbox profile only.",
        ),
        profile(
            "W+_2010.1", "W+ 2010.1", "Special profile", ConnectivityMode.SPECIAL, AuthorizationMode.SPECIAL, false,
            paymentMode = ProtocolPaymentMode.OFFLINE_TEST,
            outcome = SandboxOutcome.CAPTURE,
            ui = "Demo",
            family = "SPECIAL",
            description = "Reference label from terminal receipt — sandbox profile only.",
        ),
    )

    fun byId(id: String): ProtocolProfile? = all.find { it.id == id }

    private fun profile(
        id: String,
        code: String,
        title: String,
        connectivity: ConnectivityMode,
        auth: AuthorizationMode,
        requiresOnline: Boolean,
        types: Set<TerminalTransactionType> = saleFamily,
        digitGroup: Int? = null,
        paymentMode: ProtocolPaymentMode = if (requiresOnline) ProtocolPaymentMode.ONLINE else ProtocolPaymentMode.OFFLINE_TEST,
        outcome: SandboxOutcome = SandboxOutcome.CAPTURE,
        ui: String = "Demo",
        allowsManual: Boolean = true,
        signatureLikely: Boolean = false,
        family: String? = null,
        description: String? = null,
    ): ProtocolProfile {
        val fam = family ?: code.split(".", limit = 2).firstOrNull()?.filter { it.isDigit() || it.isLetter() }
            ?.takeIf { it.isNotBlank() }
            ?: code.takeWhile { it.isDigit() }.ifBlank { "SPECIAL" }
        val desc = description
            ?: "Sandbox protocol profile $code — $title. Not a verified Visa/Mastercard wire specification."
        return ProtocolProfile(
            id = id,
            code = code,
            displayName = "$code - $title",
            description = desc,
            connectivityMode = connectivity,
            authorizationMode = auth,
            supportedTransactionTypes = types,
            requiresOnlineAuthorization = requiresOnline,
            sandboxOnly = true,
            environmentBadge = EnvironmentBadge.SANDBOX,
            family = fam,
            digitGroup = digitGroup,
            paymentMode = paymentMode,
            sandboxOutcome = outcome,
            version = "1.0",
            enabled = true,
            documented = false,
            allowsManualEntry = allowsManual,
            signatureLikely = signatureLikely,
            uiEnvironmentLabel = ui,
        )
    }
}
