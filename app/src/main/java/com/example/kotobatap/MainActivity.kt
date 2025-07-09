package com.example.kotobatap

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.kotobatap.helpers.ThemeHelper
import com.example.kotobatap.ui.navigation.appNavHost
import com.example.kotobatap.ui.theme.kotobaTapTheme
import kotlinx.coroutines.flow.Flow

class MainActivity : FragmentActivity() {
    private lateinit var themeFlow: Flow<ThemeHelper.Theme>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        themeFlow = ThemeHelper.observeTheme(this)

        setContent {
            val currentTheme by produceState<ThemeHelper.Theme?>(
                initialValue = null,
                key1 = themeFlow,
            ) {
                themeFlow.collect { value = it }
            }

            if (currentTheme != null) {
                val isDarkTheme =
                    when (currentTheme) {
                        ThemeHelper.Theme.LIGHT -> false
                        ThemeHelper.Theme.DARK -> true
                        ThemeHelper.Theme.SYSTEM -> isSystemInDarkTheme()
                        else -> false
                    }

                kotobaTapTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    appNavHost(navController)
                }
            }
        }
    }
}
