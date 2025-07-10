package com.example.kotobatap.helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.kotobatap.managers.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object ThemeHelper {
    enum class Theme { LIGHT, DARK, SYSTEM }

    private const val KEY_THEME = "app_theme"

    suspend fun applyTheme(
        context: Context,
        theme: Theme,
    ) {
        val mode =
            when (theme) {
                Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

        DataStoreManager.putValue(context, KEY_THEME, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    suspend fun getSavedTheme(context: Context): Theme {
        val mode = DataStoreManager.getIntValue(context, KEY_THEME).first()

        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> Theme.LIGHT
            AppCompatDelegate.MODE_NIGHT_YES -> Theme.DARK
            else -> Theme.SYSTEM
        }
    }

    fun observeTheme(context: Context): Flow<Theme> {
        return DataStoreManager.getIntValue(context, KEY_THEME).map { mode ->
            when (mode) {
                AppCompatDelegate.MODE_NIGHT_NO -> Theme.LIGHT
                AppCompatDelegate.MODE_NIGHT_YES -> Theme.DARK
                else -> Theme.SYSTEM
            }
        }
    }
}
