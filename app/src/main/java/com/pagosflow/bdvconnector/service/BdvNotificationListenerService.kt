package com.pagosflow.bdvconnector.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pagosflow.bdvconnector.data.NotificationStorage
import com.pagosflow.bdvconnector.model.LogLevel
import com.pagosflow.bdvconnector.parser.BdvNotificationParser

/**
 * Servicio de escucha de notificaciones de Android (Fase 1.5 - Preparado para pruebas en teléfono real).
 * Se ejecuta en segundo plano cuando el usuario otorga el permiso especial de Acceso a Notificaciones.
 * Filtra estrictamente las notificaciones para procesar ÚNICAMENTE las de PagomóvilBDV del Banco de Venezuela
 * y registra detalladamente cada evento y descarte para facilitar el diagnóstico.
 */
class BdvNotificationListenerService : NotificationListenerService() {

    private val tag = "BdvNotifListener"
    private lateinit var storage: NotificationStorage

    override fun onCreate() {
        super.onCreate()
        storage = NotificationStorage.getInstance(this)
        storage.logEvent(LogLevel.INFO, tag, "BdvNotificationListenerService iniciado en Android")
        Log.d(tag, "BdvNotificationListenerService inicializado.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(tag, "Servicio conectado al sistema de notificaciones de Android.")
        storage.updateServiceStatus(true)
        storage.logEvent(LogLevel.SUCCESS, tag, "Acceso a notificaciones activo y vinculado al sistema")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(tag, "Servicio desconectado del sistema de notificaciones.")
        storage.updateServiceStatus(false)
        storage.logEvent(LogLevel.WARNING, tag, "Servicio desconectado del sistema de notificaciones")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extraer título y contenido textual de la notificación
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Seleccionar el texto más completo disponible
        val fullContent = if (bigText.length > text.length) bigText else text

        // FILTRO DE SEGURIDAD ESTRICTO:
        // No procesar otras aplicaciones (WhatsApp, SMS, llamadas, redes sociales, etc.)
        if (!BdvNotificationParser.isBdvPaymentNotification(packageName, title, fullContent)) {
            // Si el paquete es de BDV pero no es un pago, lo registramos a nivel informativo para pruebas de compatibilidad
            if (packageName.contains("bancodevenezuela", ignoreCase = true) || packageName.contains("bdv", ignoreCase = true)) {
                storage.logEvent(
                    LogLevel.INFO,
                    "NotifBDVIgnorada",
                    "Notificación de BDV no reconocida como PagomóvilBDV: \"$title\"",
                    details = "Contenido: $fullContent | Paquete: $packageName"
                )
            }
            return
        }

        storage.logEvent(
            LogLevel.INFO,
            "CapturaDetectada",
            "Notificación PagomóvilBDV detectada de $packageName",
            details = "Título: $title | Longitud: ${fullContent.length} caracteres"
        )

        // Parsear el texto a nuestro modelo de datos
        val parsedPayment = BdvNotificationParser.parse(fullContent, packageName)
        if (parsedPayment != null) {
            storage.saveNotification(parsedPayment)
            Log.i(tag, "Pago registrado localmente: Monto=${parsedPayment.monto}, Ref=${parsedPayment.referencia}, Pagador=${parsedPayment.pagador}")
        } else {
            storage.logEvent(
                LogLevel.ERROR,
                "ErrorParsing",
                "Error analizando la estructura de la notificación BDV",
                details = "Texto original que falló: $fullContent"
            )
            Log.w(tag, "No se pudieron extraer los campos del pago a pesar del filtro.")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No requerimos acción destructiva al descartarse la notificación en la barra de estado
    }
}
