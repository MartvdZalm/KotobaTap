package com.example.kotobatap.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotobatap.ui.components.appHeader

@SuppressLint("ComposableNaming")
@Composable
fun dictionaryScreen(onBack: () -> Unit) {
    var userInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            appHeader(
                title = "Dictionary",
                showBackButton = true,
                onBackClick = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Japanese Characters") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {},
                enabled = userInput.isNotBlank(),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Search")
            }
        }
    }
}
