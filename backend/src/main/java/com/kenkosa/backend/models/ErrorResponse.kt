package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val errors: List<String>)