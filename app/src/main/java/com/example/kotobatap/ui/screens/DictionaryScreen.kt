package com.example.kotobatap.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotobatap.data.api.ApiState
import com.example.kotobatap.ui.components.appHeader
import com.example.kotobatap.ui.viewmodels.JishoViewModel
import org.json.JSONObject

@SuppressLint("ComposableNaming")
@Composable
fun dictionaryScreen(onBack: () -> Unit) {
    val viewModel: JishoViewModel = viewModel()
    var userInput by remember { mutableStateOf("") }
    val stateApi by viewModel.apiState.collectAsState()

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
                onClick = {
                    viewModel.search(userInput)
                },
                enabled = userInput.isNotBlank(),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Search")
            }

            when (val state = stateApi) {
                is ApiState.Idle -> {}

                is ApiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                is ApiState.Success -> {
                    val jsonObject = JSONObject(state.data)
                    val englishDefinitionsArray =
                        jsonObject
                            .getJSONArray("data")
                            .getJSONObject(0)
                            .getJSONArray("senses")
                            .getJSONObject(0)
                            .getJSONArray("english_definitions")

                    val japaneseReading =
                        jsonObject
                            .getJSONArray("data")
                            .getJSONObject(0)
                            .getJSONArray("japanese")
                            .getJSONObject(0)
                            .getString("reading")

                    Text(
                        text = "Reading: $japaneseReading",
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    Text(text = "English:")
                    for (i in 0 until minOf(3, englishDefinitionsArray.length())) {
                        Text(
                            text = "- ${englishDefinitionsArray.getString(i)}",
                            modifier = Modifier.padding(top = 5.dp).padding(start = 5.dp),
                        )
                    }
                }

                is ApiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
