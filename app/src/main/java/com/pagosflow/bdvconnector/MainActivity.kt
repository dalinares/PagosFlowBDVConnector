package com.pagosflow.bdvconnector

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.pagosflow.bdvconnector.data.NotificationStorage
import com.pagosflow.bdvconnector.model.LogLevel
import com.pagosflow.bdvconnector.parser.BdvNotificationParser
import com.pagosflow.bdvconnector.ui.MainScreen
import com.pagosflow.bdvconnector.ui.SystemServiceStatus
import com.pagosflow.bdvconnector.ui.theme.PagosFlowTheme

class MainActivity : ComponentActivity() {

    private lateinit var storage: NotificationStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = NotificationStorage.getInstance(this)
        storage.logEvent(LogLevel.INFO, "MainActivity", "Pantalla principal iniciada")

        setContent {
            PagosFlowTheme {
                val isServiceActive by storage.isServiceConnected.collectAsState()
                val latestNotification by storage.latestNotification.collectAsState()
                val paymentHistory by storage.paymentHistory.collectAsState()
                val eventLogs by storage.eventLogs.collectAsState()
                var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled()) }

                // Actualizar estado de permiso al reanudar
                DisposableEffect(Unit) {
                    hasPermission = isNotificationServiceEnabled()
                    onDispose { }
                }

                // Cálculo de estado claro del sistema (Fase 1.5):
                // 1. ERROR: si falta permiso o no está vinculado
                // 2. PAGO_CAPTURADO: si acaba de registrarse un pago
                // 3. ESPERANDO_NOTIFICACION: servicio activo escuchando
                // 4. SERVICIO_ACTIVO: estado base conectado
                val systemStatus = when {
                    !hasPermission -> SystemServiceStatus.ERROR
                    !isServiceActive -> SystemServiceStatus.ERROR
                    latestNotification != null -> SystemServiceStatus.PAGO_CAPTURADO
                    else -> SystemServiceStatus.ESPERANDO_NOTIFICACION
                }

                val statusMessage = when (systemStatus) {
                    SystemServiceStatus.ERROR -> {
                        if (!hasPermission) {
                            "Acceso a notificaciones desactivado en los Ajustes de Android. Toca el botón de configuración para autorizar."
                        } else {
                            "Servicio esperando que el sistema Android lo vincule (ListenerService pendiente)."
                        }
                    }
                    SystemServiceStatus.PAGO_CAPTURADO -> "Último pago capturado correctamente y almacenado en el historial local."
                    SystemServiceStatus.ESPERANDO_NOTIFICACION -> "Servicio en segundo plano escuchando activamente notificaciones de BDV."
                    SystemServiceStatus.SERVICIO_ACTIVO -> "Servicio conectado y listo para procesar pagos."
                }

                MainScreen(
                    currentStatus = systemStatus,
                    statusMessage = statusMessage,
                    hasPermissionGranted = hasPermission,
                    latestNotification = latestNotification,
                    paymentHistory = paymentHistory,
                    eventLogs = eventLogs,
                    onRequestPermission = { openNotificationAccessSettings() },
                    onTestBdvParser = {
                        runSyntheticBdvParserTest()
                    },
                    onTestConnection = {
                        val active = isNotificationServiceEnabled()
                        storage.updateServiceStatus(active)
                        storage.logEvent(
                            if (active) LogLevel.SUCCESS else LogLevel.ERROR,
                            "Diagnostico",
                            "Verificación manual de estado: " + if (active) "Permiso concedido" else "Falta permiso en Android"
                        )
                    },
                    onClearHistory = {
                        storage.clearHistory()
                    },
                    onClearLogs = {
                        storage.clearLogs()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = isNotificationServiceEnabled()
        storage.updateServiceStatus(hasPermission)
    }

    /**
     * Requisito 5: Valida el parser con una notificación de prueba real sin llamada externa.
     */
    private fun runSyntheticBdvParserTest() {
        val sampleRealText = "Recibiste un PagomovilBDV de SILENE CECIRA DAVILA GONZALEZ por Bs.0,10 bajo el número de operación 007232795866"
        val packageName = "com.bancodevenezuela.bdvdigital"

        storage.logEvent(LogLevel.INFO, "PruebaParser", "Ejecutando prueba sintética de extracción con texto de BDV Móvil")
        val parsed = BdvNotificationParser.parse(sampleRealText, packageName)
        if (parsed != null) {
            storage.saveNotification(parsed)
            storage.logEvent(
                LogLevel.SUCCESS,
                "PruebaParser",
                "Parser validado con éxito: Monto=${parsed.monto}, Ref=${parsed.referencia}, Pagador=${parsed.pagador}"
            )
        } else {
            storage.logEvent(
                LogLevel.ERROR,
                "PruebaParser",
                "El parser no pudo interpretar la notificación de prueba."
            )
        }
    }

    /**
     * Requisito 1 y 6: Verifica si el usuario ha habilitado el acceso a notificaciones en los Ajustes del Sistema Android.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && packageName == cn.packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Requisito 2: Abre directamente la pantalla de Ajustes de Android para otorgar acceso a notificaciones.
     */
    private fun openNotificationAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            storage.logEvent(LogLevel.INFO, "Ajustes", "Se abrió la pantalla de Ajustes de Notificaciones de Android")
        } catch (e: Exception) {
            storage.logEvent(LogLevel.ERROR, "Ajustes", "Error al abrir ajustes de notificaciones: ${e.localizedMessage}")
        }
    }
}
