package com.cryptopos.pos.core.navigation

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
import com.cryptopos.pos.features.settings.SettingsRoute
import com.cryptopos.pos.features.splash.SplashRoute
import com.cryptopos.pos.features.support.AboutRoute
import com.cryptopos.pos.features.support.SupportRoute
import com.cryptopos.pos.features.transactions.TransactionDetailRoute
import com.cryptopos.pos.features.transactions.TransactionsRoute
import com.cryptopos.pos.features.wallet.WalletRoute
import com.cryptopos.pos.hardware.security.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val Payment = "payment"
    const val History = "history"
    const val Wallet = "wallet"
    const val Settings = "settings"
    const val Profile = "profile"
    const val Support = "support"
    const val About = "about"
    const val TransactionDetail = "transaction/{id}"
    const val Receipt = "receipt/{id}"

    fun transaction(id: String) = "transaction/$id"
    fun receipt(id: String) = "receipt/$id"
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
                title = "Unlock CryptoPOS",
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
                onPay = { navController.navigate(Routes.Payment) },
                onHistory = { navController.navigate(Routes.History) },
                onWallet = { navController.navigate(Routes.Wallet) },
                onSettings = { navController.navigate(Routes.Settings) },
                onProfile = { navController.navigate(Routes.Profile) },
                onSupport = { navController.navigate(Routes.Support) },
                onTransaction = { navController.navigate(Routes.transaction(it)) },
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
            TransactionsRoute(onOpen = { navController.navigate(Routes.transaction(it)) })
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
            route = Routes.TransactionDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TransactionDetailRoute(
                id = id,
                onReceipt = { navController.navigate(Routes.receipt(it)) },
            )
        }
        composable(
            route = Routes.Receipt,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            ReceiptRoute(transactionId = entry.arguments?.getString("id").orEmpty())
        }
    }
}
