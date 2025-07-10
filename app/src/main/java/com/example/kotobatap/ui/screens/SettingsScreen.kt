package com.example.kotobatap.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kotobatap.helpers.ReaderHelper
import com.example.kotobatap.ui.components.ThemeDialog
import com.example.kotobatap.ui.components.appHeader
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

@SuppressLint("ComposableNaming")
@Composable
fun settingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            appHeader(
                title = "Settings",
                showBackButton = true,
                onBackClick = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            settingsSection("General") {
                settingItem(
                    title = "Theme",
                    description = "Change theme of the app",
                    action = {
                        Button(onClick = { ThemeDialog.show(context) }) {
                            Text("Change Theme")
                        }
                    },
                )

                settingItem(
                    title = "Highlighting",
                    description = "Change the color of the highlighting",
                    action = {
                        Button(onClick = {
                            ColorPickerDialog.Builder(context)
                                .setTitle("ColorPicker Dialog")
                                .setPreferenceName("MyColorPickerDialog")
                                .setPositiveButton(
                                    "Confirm",
                                    ColorEnvelopeListener { envelope, fromUser ->
                                        ReaderHelper.highlightingColor =
                                            "#${envelope.hexCode.substring(2)}"
                                    },
                                )
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }) {
                            Text("Change Color")
                        }
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            settingsSection(title = "About") {
                settingItem(
                    title = "Version 1",
                    description = "1.0.0",
                    action = {},
                )
            }
        }
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun settingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        content()
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun settingItem(
    title: String,
    description: String? = null,
    action: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        action()
    }
}
