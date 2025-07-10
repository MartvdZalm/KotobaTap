package com.example.kotobatap.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kotobatap.ui.screens.dictionaryScreen
import com.example.kotobatap.ui.screens.homeScreen
import com.example.kotobatap.ui.screens.readerScreen
import com.example.kotobatap.ui.screens.settingsScreen

@SuppressLint("ComposableNaming")
@Composable
fun appNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            homeScreen(
                navController = navController,
                onNavigateToReader = { encodedUrl ->
                    navController.navigate("reader?url=$encodedUrl")
                },
            )
        }
        composable(
            "reader?url={url}",
            arguments =
                listOf(
                    navArgument("url") {
                        type = NavType.StringType
                        nullable = false
                    },
                ),
        ) { backStackEntry ->
            readerScreen(
                url = backStackEntry.arguments?.getString("url") ?: "",
                onBack = { navController.popBackStack() },
            )
        }

        composable("dictionary") {
            dictionaryScreen(onBack = { navController.popBackStack() })
        }

        composable("settings") {
            settingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
