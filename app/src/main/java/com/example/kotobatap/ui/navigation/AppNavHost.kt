package com.example.kotobatap.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kotobatap.ui.screens.HomeScreen
import com.example.kotobatap.ui.screens.ReaderScreen
import com.example.kotobatap.ui.screens.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                navController = navController,
                onNavigateToReader = { encodedUrl ->
                    navController.navigate("reader?url=$encodedUrl")
                }
            )
        }
        composable(
            "reader?url={url}",
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            ReaderScreen(
                url = backStackEntry.arguments?.getString("url") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}