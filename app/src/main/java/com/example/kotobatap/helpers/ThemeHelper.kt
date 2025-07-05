package com.example.kotobatap.helpers

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object ThemeHelper {
    enum class Theme { LIGHT, DARK, SYSTEM }

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"

    fun applyTheme(activity: Activity, theme: Theme) {
        val mode = when (theme) {
            Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_THEME, mode)
            }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getSavedTheme(context: Context): Theme {
        val mode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> Theme.LIGHT
            AppCompatDelegate.MODE_NIGHT_YES -> Theme.DARK
            else -> Theme.SYSTEM
        }
    }
}