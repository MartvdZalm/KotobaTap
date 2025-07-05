package com.example.kotobatap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.kotobatap.helpers.ThemeHelper
import com.example.kotobatap.ui.navigation.AppNavHost
import com.example.kotobatap.ui.theme.KotobaTapTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt(
                "app_theme",
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    val currentTheme = remember {
        mutableStateOf(ThemeHelper.getSavedTheme(context))
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "app_theme") {
                currentTheme.value = ThemeHelper.getSavedTheme(context)
            }
        }

        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    KotobaTapTheme(
        darkTheme = when (currentTheme.value) {
            ThemeHelper.Theme.LIGHT -> false
            ThemeHelper.Theme.DARK -> true
            ThemeHelper.Theme.SYSTEM -> isSystemInDarkTheme()
        }
    ) {
        val navController = rememberNavController()
        AppNavHost(navController)
    }
}