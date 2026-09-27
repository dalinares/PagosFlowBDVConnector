package com.pagosflow.bdvconnector.model

/**
 * Modelo de datos inmutable que representa un pago móvil recibido procesado desde BDV.
 * Contiene los datos extraídos y el texto original para auditoría y reenvío a Base44.
 */
data class PaymentNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pagador: String? = null,
    val monto: String? = null,
    val referencia: String? = null,
    val telefono: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
    val textoCompleto: String,
    val paqueteOrigen: String? = null,
    val timestampCaptura: Long = System.currentTimeMillis()
)
