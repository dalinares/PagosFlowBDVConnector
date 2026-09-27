package com.pagosflow.bdvconnector.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Nivel de severidad para el registro interno de eventos.
 */
enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Representa un evento o error del sistema para auditoría y diagnóstico en dispositivo real.
 */
data class AppEventLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String,
    val message: String,
    val details: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    val formattedDateTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
