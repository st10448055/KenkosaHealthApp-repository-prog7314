package com.example.kenkosahealthapp_prog7314.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val goalTrackingApi: GoalTrackingApi by lazy { retrofit.create(GoalTrackingApi::class.java) }
    val sleepLogApi: SleepLogApi by lazy { retrofit.create(SleepLogApi::class.java) }
    val foodApi: FoodApi by lazy { retrofit.create(FoodApi::class.java) }
    val diaryApi: DiaryApi by lazy { retrofit.create(DiaryApi::class.java) }
    val medicationApi: MedicationApi by lazy { retrofit.create(MedicationApi::class.java) }
    val moodApi: MoodApi by lazy { retrofit.create(MoodApi::class.java) }

    val fastingApi: FastingApi by lazy { retrofit.create(FastingApi::class.java) }

    val workoutApi: WorkoutApi by lazy { retrofit.create(WorkoutApi::class.java) }
}