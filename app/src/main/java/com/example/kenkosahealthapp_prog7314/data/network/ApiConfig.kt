package com.example.kenkosahealthapp_prog7314.data.network

import com.example.kenkosahealthapp_prog7314.BuildConfig

/**
 * Where the app finds the REST API.
 *
 * The URL is chosen at build time in app/build.gradle.kts:
 *  - Default (everyone, and all release builds): the cloud-hosted API on Render (HTTPS).
 *  - Override per developer: add  api.baseUrl=http://10.0.2.2:8080/  to local.properties
 *    (that file is git-ignored, so it never affects teammates).
 */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL
}