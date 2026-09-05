package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PaymentProgressScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.TransactionDetailScreen
import com.example.ui.viewmodel.PaymentViewModel
import com.example.ui.viewmodel.PaymentViewModelFactory

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val viewModel: PaymentViewModel = viewModel(
        factory = PaymentViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPayment = {
                    navController.navigate(Screen.Payment.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToDetails = { txId ->
                    navController.navigate(Screen.TransactionDetails(txId).route)
                }
            )
        }

        composable(Screen.Payment.route) {
            PaymentScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPaymentInitiated = { txId ->
                    navController.navigate(Screen.PaymentProgress(txId).route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.PaymentProgress.ROUTE_PATTERN,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getString("transactionId") ?: ""
            PaymentProgressScreen(
                transactionId = txId,
                viewModel = viewModel,
                onNavigateToResult = { targetTxId ->
                    navController.navigate(Screen.Result(targetTxId).route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Result.ROUTE_PATTERN,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getString("transactionId") ?: ""
            ResultScreen(
                transactionId = txId,
                viewModel = viewModel,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onViewDetails = { detailsId ->
                    navController.navigate(Screen.TransactionDetails(detailsId).route)
                },
                onTryAgain = {
                    navController.navigate(Screen.Payment.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onCheckAgain = {
                    navController.navigate(Screen.PaymentProgress(txId).route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { txId ->
                    navController.navigate(Screen.TransactionDetails(txId).route)
                }
            )
        }

        composable(
            route = Screen.TransactionDetails.ROUTE_PATTERN,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailScreen(
                transactionId = txId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
