package com.example.kenkosahealthapp_prog7314.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Singleton object responsible for configuring Retrofit and providing
// access to all of the application's backend API interfaces.
object RetrofitInstance {

    // Base URL of the deployed Kenkosa Health backend hosted on Azure.
    // All Retrofit API endpoints are appended to this URL.
    private const val BASE_URL = "http://localhost:8080/"
    // HTTP logging interceptor used to log network requests and responses.
    // BODY logs request and response details, which is useful during development
    // and debugging API communication.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp client used by Retrofit to handle HTTP communication.
    private val okHttpClient = OkHttpClient.Builder()

        // Adds the Firebase authentication token to API requests.
        // The backend uses this token to authenticate the current user.
        .addInterceptor(AuthInterceptor())

        // Adds HTTP request and response logging.
        .addInterceptor(loggingInterceptor)

        // Maximum amount of time allowed to establish a connection.
        .connectTimeout(15, TimeUnit.SECONDS)

        // Maximum amount of time allowed to receive data from the server.
        .readTimeout(15, TimeUnit.SECONDS)

        .build()

    // Creates the Retrofit instance using the Azure backend URL,
    // configured OkHttp client and Gson converter.
    private val retrofit = Retrofit.Builder()

        // Sets the base URL used for all API requests.
        .baseUrl(BASE_URL)

        // Uses the configured OkHttp client for network communication.
        .client(okHttpClient)

        // Gson automatically converts JSON responses into Kotlin data classes
        // and Kotlin request objects into JSON.
        .addConverterFactory(GsonConverterFactory.create())

        .build()

    // Retrofit implementation of the goal tracking API.
    // 'lazy' means the API object is created only when it is first used.
    val goalTrackingApi: GoalTrackingApi by lazy {
        retrofit.create(GoalTrackingApi::class.java)
    }

    // Retrofit implementation of the sleep log API.
    val sleepLogApi: SleepLogApi by lazy {
        retrofit.create(SleepLogApi::class.java)
    }

    // Retrofit implementation of the food API.
    val foodApi: FoodApi by lazy {
        retrofit.create(FoodApi::class.java)
    }

    // Retrofit implementation of the diary API.
    val diaryApi: DiaryApi by lazy {
        retrofit.create(DiaryApi::class.java)
    }

    // Retrofit implementation of the medication API.
    val medicationApi: MedicationApi by lazy {
        retrofit.create(MedicationApi::class.java)
    }

    // Retrofit implementation of the mood API.
    val moodApi: MoodApi by lazy {
        retrofit.create(MoodApi::class.java)
    }

    // Retrofit implementation of the fasting API.
    val fastingApi: FastingApi by lazy {
        retrofit.create(FastingApi::class.java)
    }

    // Retrofit implementation of the workout API.
    val workoutApi: WorkoutApi by lazy {
        retrofit.create(WorkoutApi::class.java)
    }

    // Retrofit implementation of the water API.
    val waterApi: WaterApi by lazy {
        retrofit.create(WaterApi::class.java)
    }

    // Retrofit implementation of the notes API.
    val notesApi: NotesApi by lazy {
        retrofit.create(NotesApi::class.java)
    }
}

// ------------------ END OF FILE ------------------ //