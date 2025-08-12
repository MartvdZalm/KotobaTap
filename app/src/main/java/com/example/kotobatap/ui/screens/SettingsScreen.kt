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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kotobatap.managers.DataStoreManager
import com.example.kotobatap.ui.components.ThemeDialog
import com.example.kotobatap.ui.components.appHeader
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.launch

@SuppressLint("ComposableNaming")
@Composable
fun settingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val autoHighlight by DataStoreManager.getBooleanValue(context, "auto_highlight", true).collectAsState(initial = true)
    val fontSize by DataStoreManager.getFloatValue(context, "font_size", 16f).collectAsState(initial = 16f)
    val lineSpacing by DataStoreManager.getFloatValue(context, "line_spacing", 1.2f).collectAsState(initial = 1.2f)

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
            settingsSection("Reader") {
                settingItem(
                    title = "Highlight Japanese",
                    description = "Highlight Japanese words when page loads",
                    action = {
                        Switch(
                            checked = autoHighlight,
                            onCheckedChange = {
                                scope.launch { DataStoreManager.putValue(context, "auto_highlight", it) }
                            },
                        )
                    },
                )

                settingItem(
                    title = "Font Size",
                    description = "Adjust text size for better readability (${fontSize.toInt()}px)",
                    action = {
                        Slider(
                            value = fontSize,
                            onValueChange = { scope.launch { DataStoreManager.putValue(context, "font_size", it) } },
                            valueRange = 12f..24f,
                            steps = 11,
                            modifier = Modifier.weight(1f),
                        )
                    },
                )

                settingItem(
                    title = "Line Spacing",
                    description = "Adjust spacing between lines (${String.format("%.1f", lineSpacing)}x)",
                    action = {
                        Slider(
                            value = lineSpacing,
                            onValueChange = { scope.launch { DataStoreManager.putValue(context, "line_spacing", it) } },
                            valueRange = 1f..2f,
                            steps = 9,
                            modifier = Modifier.weight(1f),
                        )
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            settingsSection("Appearance") {
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
                    title = "Highlighting Color",
                    description = "Customize Japanese word highlighting color",
                    action = {
                        Button(onClick = {
                            ColorPickerDialog.Builder(context)
                                .setTitle("Choose Highlight Color")
                                .setPreferenceName("HighlightColorPicker")
                                .setPositiveButton(
                                    "Confirm",
                                    ColorEnvelopeListener { envelope, fromUser ->
                                        scope.launch {
                                            DataStoreManager.putValue(context, "highlight_color", "#${envelope.hexCode.substring(2)}")
                                        }
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

            settingsSection("Advanced") {
                settingItem(
                    title = "Clear Data",
                    description = "Remove all saved data",
                    action = {
                        Button(
                            onClick = {
                                scope.launch {
                                    DataStoreManager.clearDataStore(context)
                                }
                            },
                        ) {
                            Text("Reset")
                        }
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            settingsSection(title = "About") {
                settingItem(
                    title = "Version",
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
