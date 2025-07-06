package com.example.kotobatap.ui.components

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.kotobatap.R
import com.example.kotobatap.helpers.ThemeHelper
import kotlinx.coroutines.launch

class ThemeDialog : DialogFragment() {
    private var selectedTheme = ThemeHelper.Theme.SYSTEM

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_theme_selector, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.themeRadioGroup)

        lifecycleScope.launch {
            val currentTheme = ThemeHelper.getSavedTheme(requireContext())

            when (currentTheme) {
                ThemeHelper.Theme.LIGHT -> radioGroup.check(R.id.lightThemeRadio)
                ThemeHelper.Theme.DARK -> radioGroup.check(R.id.darkThemeRadio)
                else -> radioGroup.check(R.id.systemDefaultRadio)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTheme = when (checkedId) {
                R.id.lightThemeRadio -> ThemeHelper.Theme.LIGHT
                R.id.darkThemeRadio -> ThemeHelper.Theme.DARK
                else -> ThemeHelper.Theme.SYSTEM
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                lifecycleScope.launch {
                    ThemeHelper.applyTheme(requireActivity(), selectedTheme)
                    dismiss()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
    }

    companion object {
        fun show(context: Context) {
            if (context is FragmentActivity) {
                ThemeDialog().show(context.supportFragmentManager, "ThemeDialog")
            }
        }
    }
}