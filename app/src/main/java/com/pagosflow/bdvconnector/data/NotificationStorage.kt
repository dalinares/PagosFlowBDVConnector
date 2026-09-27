package com.pagosflow.bdvconnector.data

import android.content.Context
import android.content.SharedPreferences
import com.pagosflow.bdvconnector.model.AppEventLog
import com.pagosflow.bdvconnector.model.LogLevel
import com.pagosflow.bdvconnector.model.PaymentNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Almacén local ligero para guardar:
 * 1. La última notificación BDV
 * 2. Historial de los últimos 20 pagos capturados
 * 3. Registro interno de eventos y errores (hasta 50 eventos)
 * 4. Estado de servicio y diagnóstico en dispositivo real
 */
class NotificationStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _latestNotification = MutableStateFlow<PaymentNotification?>(loadLatestNotification())
    val latestNotification: StateFlow<PaymentNotification?> = _latestNotification.asStateFlow()

    private val _paymentHistory = MutableStateFlow<List<PaymentNotification>>(loadPaymentHistory())
    val paymentHistory: StateFlow<List<PaymentNotification>> = _paymentHistory.asStateFlow()

    private val _eventLogs = MutableStateFlow<List<AppEventLog>>(loadEventLogs())
    val eventLogs: StateFlow<List<AppEventLog>> = _eventLogs.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun updateServiceStatus(connected: Boolean) {
        val previous = _isServiceConnected.value
        _isServiceConnected.value = connected
        if (previous != connected) {
            val level = if (connected) LogLevel.SUCCESS else LogLevel.WARNING
            val msg = if (connected) "Servicio conectado al sistema de notificaciones de Android" else "Servicio desconectado de notificaciones"
            logEvent(level, "ServiceStatus", msg)
        }
    }

    /**
     * Guarda un pago capturado: actualiza latestNotification y añade al historial (máximo 20).
     */
    fun saveNotification(notification: PaymentNotification) {
        _latestNotification.value = notification
        persistNotification(notification)

        // Actualizar historial asegurando un tope de 20 elementos más recientes
        val currentHistory = _paymentHistory.value.toMutableList()
        currentHistory.removeAll { it.id == notification.id || (it.referencia != null && it.referencia == notification.referencia) }
        currentHistory.add(0, notification)
        val trimmed = if (currentHistory.size > MAX_HISTORY_ITEMS) currentHistory.take(MAX_HISTORY_ITEMS) else currentHistory
        _paymentHistory.value = trimmed
        persistHistory(trimmed)

        logEvent(
            level = LogLevel.SUCCESS,
            tag = "PagoCapturado",
            message = "Pago recibido por ${notification.monto ?: "Monto desconocido"} - Ref: ${notification.referencia ?: "N/A"}",
            details = "Pagador: ${notification.pagador ?: "N/A"} | Tel: ${notification.telefono ?: "N/A"}"
        )
    }

    /**
     * Registra un evento o error del sistema en memoria y persistencia (máximo 50).
     */
    fun logEvent(level: LogLevel, tag: String, message: String, details: String? = null) {
        val log = AppEventLog(
            level = level,
            tag = tag,
            message = message,
            details = details
        )
        val current = _eventLogs.value.toMutableList()
        current.add(0, log)
        val trimmed = if (current.size > MAX_LOGS) current.take(MAX_LOGS) else current
        _eventLogs.value = trimmed
        persistLogs(trimmed)
    }

    fun clearHistory() {
        _paymentHistory.value = emptyList()
        prefs.edit().remove(KEY_PAYMENT_HISTORY).apply()
        logEvent(LogLevel.INFO, "Historial", "Historial de pagos limpiado por el usuario")
    }

    fun clearLogs() {
        _eventLogs.value = emptyList()
        prefs.edit().remove(KEY_EVENT_LOGS).apply()
        logEvent(LogLevel.INFO, "Logs", "Registro de eventos reiniciado")
    }

    // Persistencia de última notificación
    private fun persistNotification(notification: PaymentNotification) {
        prefs.edit().putString(KEY_LATEST_NOTIFICATION, notificationToJson(notification).toString()).apply()
    }

    private fun loadLatestNotification(): PaymentNotification? {
        val raw = prefs.getString(KEY_LATEST_NOTIFICATION, null) ?: return null
        return try {
            jsonToNotification(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    // Persistencia de historial (20 pagos)
    private fun persistHistory(list: List<PaymentNotification>) {
        val array = JSONArray()
        list.forEach { array.put(notificationToJson(it)) }
        prefs.edit().putString(KEY_PAYMENT_HISTORY, array.toString()).apply()
    }

    private fun loadPaymentHistory(): List<PaymentNotification> {
        val raw = prefs.getString(KEY_PAYMENT_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<PaymentNotification>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                jsonToNotification(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Persistencia de logs de eventos
    private fun persistLogs(logs: List<AppEventLog>) {
        val array = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("timestamp", log.timestamp)
                put("level", log.level.name)
                put("tag", log.tag)
                put("message", log.message)
                put("details", log.details ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_EVENT_LOGS, array.toString()).apply()
    }

    private fun loadEventLogs(): List<AppEventLog> {
        val raw = prefs.getString(KEY_EVENT_LOGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AppEventLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppEventLog(
                        id = obj.optString("id"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        level = try { LogLevel.valueOf(obj.optString("level")) } catch (_: Exception) { LogLevel.INFO },
                        tag = obj.optString("tag"),
                        message = obj.optString("message"),
                        details = obj.optString("details").ifEmpty { null }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun notificationToJson(n: PaymentNotification): JSONObject {
        return JSONObject().apply {
            put("id", n.id)
            put("pagador", n.pagador ?: "")
            put("monto", n.monto ?: "")
            put("referencia", n.referencia ?: "")
            put("telefono", n.telefono ?: "")
            put("fecha", n.fecha ?: "")
            put("hora", n.hora ?: "")
            put("textoCompleto", n.textoCompleto)
            put("paqueteOrigen", n.paqueteOrigen ?: "")
            put("timestampCaptura", n.timestampCaptura)
        }
    }

    private fun jsonToNotification(json: JSONObject): PaymentNotification? {
        return try {
            PaymentNotification(
                id = json.optString("id"),
                pagador = json.optString("pagador").ifEmpty { null },
                monto = json.optString("monto").ifEmpty { null },
                referencia = json.optString("referencia").ifEmpty { null },
                telefono = json.optString("telefono").ifEmpty { null },
                fecha = json.optString("fecha").ifEmpty { null },
                hora = json.optString("hora").ifEmpty { null },
                textoCompleto = json.optString("textoCompleto"),
                paqueteOrigen = json.optString("paqueteOrigen").ifEmpty { null },
                timestampCaptura = json.optLong("timestampCaptura", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "pagosflow_bdv_prefs"
        private const val KEY_LATEST_NOTIFICATION = "key_latest_notification"
        private const val KEY_PAYMENT_HISTORY = "key_payment_history"
        private const val KEY_EVENT_LOGS = "key_event_logs"
        private const val MAX_HISTORY_ITEMS = 20
        private const val MAX_LOGS = 50

        @Volatile
        private var INSTANCE: NotificationStorage? = null

        fun getInstance(context: Context): NotificationStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
