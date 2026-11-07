package com.example.data

data class InterceptionLog(
    val id: String,
    val timestamp: Long,
    val triggerReason: String,
    val timeToRestoreSeconds: Float,
    val restoredVolumePercent: Int
)
