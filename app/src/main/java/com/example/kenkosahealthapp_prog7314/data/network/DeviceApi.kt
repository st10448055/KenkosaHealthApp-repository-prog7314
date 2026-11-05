package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

interface DeviceApi {

    @PUT("api/devices/token")
    suspend fun registerToken(@Body request: RegisterDeviceRequest): Response<Unit>

    // Retrofit's @DELETE cannot carry a body, so @HTTP is used instead.
    @HTTP(method = "DELETE", path = "api/devices/token", hasBody = true)
    suspend fun unregisterToken(@Body request: UnregisterDeviceRequest): Response<Unit>

    @POST("api/notifications/test")
    suspend fun sendTestNotification(): SendResultDto
}

data class RegisterDeviceRequest(
    val token: String,
    val platform: String = "android"
)

data class UnregisterDeviceRequest(val token: String)

data class SendResultDto(
    val devices: Int,
    val sent: Int
)