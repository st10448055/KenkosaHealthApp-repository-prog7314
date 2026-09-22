package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.GET
import retrofit2.http.Query


interface FoodApi {
    @GET("api/food/search")
    suspend fun searchByBarcode(@Query("barcode") barcode: String): FoodItemDto
    @GET("api/food/search")
    suspend fun searchByQuery(@Query("query") query: String): FoodItemDto
}

