package com.example.smartattendancesystem.model

data class AttendanceModel(
    val email: String = "",
    val timestamp: Long = 0L,
    val status: String = "",
    val imageUrl: String = ""
)
