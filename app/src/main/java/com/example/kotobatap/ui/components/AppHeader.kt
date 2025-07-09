package com.example.kotobatap.ui.components

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@SuppressLint("ComposableNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appHeader(
    title: String,
    onMenuClick: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Menu")
                }
            }
        },
    )
}
