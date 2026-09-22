package com.example.kenkosahealthapp_prog7314.data.network

data class DiaryNoteDto(
    val date: String,
    val noteText: String
)

data class UpdateNoteRequest(
    val noteText: String
)