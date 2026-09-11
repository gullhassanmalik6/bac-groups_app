package com.cryptopos.pos.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cryptopos.pos.core.analytics.AnalyticsTracker
import com.cryptopos.pos.features.auth.LoginRoute
import com.cryptopos.pos.features.dashboard.DashboardRoute
import com.cryptopos.pos.features.payment.PaymentRoute
import com.cryptopos.pos.features.profile.ProfileRoute
import com.cryptopos.pos.features.receipts.ReceiptRoute
import com.cryptopos.pos.features.receipts.TerminalReceiptRoute
import com.cryptopos.pos.features.settings.SettingsRoute
import com.cryptopos.pos.features.splash.SplashRoute
import com.cryptopos.pos.features.support.AboutRoute
import com.cryptopos.pos.features.support.SupportRoute
import com.cryptopos.pos.features.terminal.ProtocolSelectionRoute
import com.cryptopos.pos.features.terminal.TerminalAmountRoute
import com.cryptopos.pos.features.terminal.TerminalProcessingRoute
import com.cryptopos.pos.features.transactions.TransactionDetailRoute
import com.cryptopos.pos.features.transactions.TransactionsRoute
import com.cryptopos.pos.features.wallet.WalletRoute
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.hardware.security.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Dashboard = "dashboard"
    /** Legacy charge flow (still available). */
    const val Payment = "payment"
    /** Terminal: amount → protocol → mock authorize → result. */
    const val TerminalAmount = "terminal/amount"
    const val TerminalProtocol =
        "terminal/protocol?amount={amount}&currency={currency}&txnType={txnType}"
    const val TerminalProcessing = "terminal/processing"
    const val History = "history"
    const val Wallet = "wallet"
    const val Settings = "settings"
    const val Profile = "profile"
    const val Support = "support"
    const val About = "about"
    const val TransactionDetail = "transaction/{id}"
    const val HistoryDetail = "history/{source}/{id}"
    const val Receipt = "receipt/{id}"
    const val TerminalReceipt = "terminal/receipt/{id}"

    fun transaction(id: String) = "transaction/$id"
    fun historyDetail(source: String, id: String) = "history/$source/$id"
    fun receipt(id: String) = "receipt/$id"
    fun terminalReceipt(id: String) = "terminal/receipt/$id"

    fun terminalProtocol(amount: String, currency: String, txnType: String): String =
        "terminal/protocol?amount=${Uri.encode(amount)}&currency=${Uri.encode(currency)}&txnType=${Uri.encode(txnType)}"
}

@HiltViewModel
class NavAnalyticsViewModel @Inject constructor(
    private val analytics: AnalyticsTracker,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    fun track(screen: String) {
        analytics.screen(screen)
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        viewModelScope.launch {
            val ok = biometricAuthenticator.authenticate(
                activity = activity,
                title = "Unlock Bonyan POS",
                subtitle = "Confirm your identity to continue",
            )
            if (ok) onSuccess() else onFailure()
        }
    }
}

@Composable
fun CryptoPosNavHost(
    analyticsViewModel: NavAnalyticsViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            analyticsViewModel.track(destination.route ?: "unknown")
        }
    }

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashRoute(
                onAuthenticated = { biometricRequired ->
                    val goDashboard = {
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                    val goLogin = {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                    if (biometricRequired && activity != null) {
                        analyticsViewModel.unlockWithBiometric(
                            activity = activity,
                            onSuccess = goDashboard,
                            onFailure = goLogin,
                        )
                    } else {
                        goDashboard()
                    }
                },
                onUnauthenticated = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Login) {
            LoginRoute(
                onLoggedIn = {
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Dashboard) {
            DashboardRoute(
                onPay = { navController.navigate(Routes.TerminalAmount) },
                onLegacyPay = { navController.navigate(Routes.Payment) },
                onHistory = { navController.navigate(Routes.History) },
                onWallet = { navController.navigate(Routes.Wallet) },
                onSettings = { navController.navigate(Routes.Settings) },
                onProfile = { navController.navigate(Routes.Profile) },
                onSupport = { navController.navigate(Routes.Support) },
                onTransaction = { navController.navigate(Routes.transaction(it)) },
            )
        }
        composable(Routes.TerminalAmount) {
            TerminalAmountRoute(
                onContinue = { amount, currency, txnType ->
                    navController.navigate(Routes.terminalProtocol(amount, currency, txnType))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TerminalProtocol,
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType },
                navArgument("txnType") { type = NavType.StringType },
            ),
        ) { entry ->
            val amount = entry.arguments?.getString("amount").orEmpty()
            val currency = entry.arguments?.getString("currency") ?: "CAD"
            val txnType = entry.arguments?.getString("txnType") ?: "SALE"
            ProtocolSelectionRoute(
                onContinue = { _ ->
                    navController.navigate(Routes.TerminalProcessing) {
                        popUpTo(Routes.TerminalAmount)
                    }
                },
                onHistory = { navController.navigate(Routes.History) },
                onWallet = { navController.navigate(Routes.Wallet) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TerminalProcessing) {
            TerminalProcessingRoute(
                onDone = {
                    navController.popBackStack(Routes.Dashboard, inclusive = false)
                },
                onBack = { navController.popBackStack() },
                onViewReceipt = { id ->
                    navController.navigate(Routes.terminalReceipt(id))
                },
            )
        }
        composable(Routes.Payment) {
            PaymentRoute(
                onSuccess = { id ->
                    navController.navigate(Routes.receipt(id)) {
                        popUpTo(Routes.Dashboard)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.History) {
            TransactionsRoute(
                onOpen = { source, id ->
                    navController.navigate(Routes.historyDetail(source.name.lowercase(), id))
                },
            )
        }
        composable(Routes.Wallet) { WalletRoute() }
        composable(Routes.Profile) { ProfileRoute() }
        composable(Routes.Support) { SupportRoute() }
        composable(Routes.About) { AboutRoute() }
        composable(Routes.Settings) {
            SettingsRoute(
                onLoggedOut = {
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSupport = { navController.navigate(Routes.Support) },
                onAbout = { navController.navigate(Routes.About) },
                onProfile = { navController.navigate(Routes.Profile) },
            )
        }
        composable(
            route = Routes.HistoryDetail,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
            ),
        ) { entry ->
            val sourceRaw = entry.arguments?.getString("source").orEmpty()
            val id = entry.arguments?.getString("id").orEmpty()
            val source = runCatching { HistorySource.valueOf(sourceRaw.uppercase()) }
                .getOrDefault(HistorySource.LEGACY)
            TransactionDetailRoute(
                source = source,
                id = id,
                onReceipt = { navController.navigate(Routes.receipt(it)) },
                onTerminalReceipt = { navController.navigate(Routes.terminalReceipt(it)) },
            )
        }
        composable(
            route = Routes.TransactionDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TransactionDetailRoute(
                source = HistorySource.LEGACY,
                id = id,
                onReceipt = { navController.navigate(Routes.receipt(it)) },
                onTerminalReceipt = { navController.navigate(Routes.terminalReceipt(it)) },
            )
        }
        composable(
            route = Routes.Receipt,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            ReceiptRoute(transactionId = entry.arguments?.getString("id").orEmpty())
        }
        composable(
            route = Routes.TerminalReceipt,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TerminalReceiptRoute(
                source = HistorySource.TERMINAL,
                id = id,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
