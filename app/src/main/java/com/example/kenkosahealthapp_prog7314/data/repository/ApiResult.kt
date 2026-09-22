package com.example.kenkosahealthapp_prog7314.data.repository

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ValidationError(val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
    data class ServerError(val code: Int, val message: String) : ApiResult<Nothing>()
}