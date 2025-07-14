package com.example.kotobatap.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotobatap.data.api.ApiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class JishoViewModel : ViewModel() {
    private val client = OkHttpClient()
    private val jishoSearchUrl = "https://jisho.org/api/v1/search/words?keyword=%s"
    private val state = MutableStateFlow<ApiState>(ApiState.Idle)
    val apiState: StateFlow<ApiState> = state.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            state.value = ApiState.Loading
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
                val url = String.format(jishoSearchUrl, encodedQuery)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                state.value = ApiState.Success(response.body?.string() ?: "")
            } catch (e: Exception) {
                state.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
