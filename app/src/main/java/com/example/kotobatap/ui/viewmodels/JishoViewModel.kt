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
    private val _apiState = MutableStateFlow<ApiState>(ApiState.Idle)
    val searchState: StateFlow<ApiState> = _apiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _apiState.value = ApiState.Loading
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
                val url = String.format(jishoSearchUrl, encodedQuery)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                _apiState.value = ApiState.Success(response.body?.string() ?: "")
            } catch (e: Exception) {
                _apiState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
