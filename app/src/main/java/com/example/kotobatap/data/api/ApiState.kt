package com.example.kotobatap.data.api

interface ApiState {
    data object Idle : ApiState

    data object Loading : ApiState

    data class Success(val data: String) : ApiState

    data class Error(val message: String) : ApiState
}
