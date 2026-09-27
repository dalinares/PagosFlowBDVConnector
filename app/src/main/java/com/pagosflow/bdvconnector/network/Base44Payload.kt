package com.pagosflow.bdvconnector.network

import com.pagosflow.bdvconnector.model.PaymentNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estructura de datos preparada para el envío HTTP POST al backend Base44 (Fase 2).
 * Endpoint previsto: https://pagos-bdv.base44.app/functions/procesarTextoBDV
 */
data class Base44Payload(
    val texto: String,
    val canal_origen: String = "notificacion_bdv",
    val dispositivo_origen: String = "android_connector",
    val fecha_captura: String
) {
    companion object {
        const val BACKEND_ENDPOINT = "https://pagos-bdv.base44.app/functions/procesarTextoBDV"

        /**
         * Crea el payload esperado a partir del modelo PaymentNotification
         */
        fun fromNotification(notification: PaymentNotification): Base44Payload {
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
            val fechaActualIso = isoDateFormat.format(Date(notification.timestampCaptura))

            return Base44Payload(
                texto = notification.textoCompleto,
                canal_origen = "notificacion_bdv",
                dispositivo_origen = "android_connector",
                fecha_captura = fechaActualIso
            )
        }
    }
}
