package com.pagosflow.bdvconnector.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagosflow.bdvconnector.model.AppEventLog
import com.pagosflow.bdvconnector.model.LogLevel
import com.pagosflow.bdvconnector.model.PaymentNotification

/**
 * Estados del indicador del sistema requeridos por la Fase 1.5:
 * - SERVICIO_ACTIVO
 * - ESPERANDO_NOTIFICACION
 * - PAGO_CAPTURADO
 * - ERROR
 */
enum class SystemServiceStatus {
    SERVICIO_ACTIVO,
    ESPERANDO_NOTIFICACION,
    PAGO_CAPTURADO,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentStatus: SystemServiceStatus,
    statusMessage: String,
    hasPermissionGranted: Boolean,
    latestNotification: PaymentNotification?,
    paymentHistory: List<PaymentNotification>,
    eventLogs: List<AppEventLog>,
    onRequestPermission: () -> Unit,
    onTestBdvParser: () -> Unit,
    onTestConnection: () -> Unit,
    onClearHistory: () -> Unit,
    onClearLogs: () -> Unit
) {
    var currentTab by remember { mutableStateOf(0) } // 0: Inicio/Estado, 1: Historial (20), 2: Logs
    var selectedPaymentForDetail by remember { mutableStateOf<PaymentNotification?>(null) }
    var showTestParserDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PagosFlow BDV Connector",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Fase 1.5: Pruebas en Dispositivo Real",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRequestPermission) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de Notificación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (paymentHistory.isNotEmpty()) {
                                    Badge { Text("${paymentHistory.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                        }
                    },
                    label = { Text("Historial (20)") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                val errorCount = eventLogs.count { it.level == LogLevel.ERROR }
                                if (errorCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$errorCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    },
                    label = { Text("Registro Logs") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeConfigScreen(
                    currentStatus = currentStatus,
                    statusMessage = statusMessage,
                    hasPermissionGranted = hasPermissionGranted,
                    latestNotification = latestNotification,
                    onRequestPermission = onRequestPermission,
                    onTestBdvParser = {
                        onTestBdvParser()
                        showTestParserDialog = true
                    },
                    onTestConnection = onTestConnection,
                    onViewHistory = { currentTab = 1 }
                )
                1 -> PaymentHistoryScreen(
                    history = paymentHistory,
                    onSelectPayment = { selectedPaymentForDetail = it },
                    onClearHistory = onClearHistory
                )
                2 -> EventLogsScreen(
                    logs = eventLogs,
                    onClearLogs = onClearLogs
                )
            }
        }
    }

    // Modal de detalle de pago
    if (selectedPaymentForDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedPaymentForDetail = null },
            title = { Text("Detalle de Pago BDV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Monto:", value = selectedPaymentForDetail?.monto ?: "N/A", isHighlight = true)
                    DetailRow(label = "Pagador:", value = selectedPaymentForDetail?.pagador ?: "No especificado")
                    DetailRow(label = "Referencia:", value = selectedPaymentForDetail?.referencia ?: "N/A")
                    if (selectedPaymentForDetail?.telefono != null) {
                        DetailRow(label = "Teléfono:", value = selectedPaymentForDetail!!.telefono!!)
                    }
                    DetailRow(label = "Fecha y hora:", value = "${selectedPaymentForDetail?.fecha ?: ""} ${selectedPaymentForDetail?.hora ?: ""}".trim())
                    Text(
                        text = "Texto original:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedPaymentForDetail?.textoCompleto ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPaymentForDetail = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal confirmación de prueba de parser
    if (showTestParserDialog) {
        AlertDialog(
            onDismissRequest = { showTestParserDialog = false },
            title = { Text("Prueba de Captura BDV Ejecutada") },
            text = {
                Text(
                    "Se ejecutó una simulación controlada del parser con una notificación real de Banco de Venezuela. Puedes revisar la tarjeta en pantalla de inicio, el historial de 20 pagos y el registro de logs."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTestParserDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

/**
 * 1. PANTALLA INICIAL DE CONFIGURACIÓN CON ESTADO DE PERMISOS, INDICADOR CLARO Y BOTÓN DIRECTO
 */
@Composable
fun HomeConfigScreen(
    currentStatus: SystemServiceStatus,
    statusMessage: String,
    hasPermissionGranted: Boolean,
    latestNotification: PaymentNotification?,
    onRequestPermission: () -> Unit,
    onTestBdvParser: () -> Unit,
    onTestConnection: () -> Unit,
    onViewHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Alerta de permiso con botón directo
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    contentColor = if (hasPermissionGranted) Color(0xFF1B5E20) else Color(0xFFE65100)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermissionGranted) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasPermissionGranted) "Acceso a notificaciones concedido" else "Acceso a notificaciones REQUERIDO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasPermissionGranted) {
                            "Android permite a PagosFlow escuchar las notificaciones emitidas por BDV Móvil / Banco de Venezuela."
                        } else {
                            "Debes otorgar acceso en los ajustes del teléfono para que el servicio pueda capturar los pagos móviles reales."
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Requisito 2: Botón para abrir directamente la configuración Android
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasPermissionGranted) Color(0xFF2E7D32) else Color(0xFFE65100)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasPermissionGranted) "Revisar Ajustes de Notificaciones" else "Abrir Ajustes de Notificaciones Android",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Requisito 6: INDICADOR CLARO (Servicio activo, Esperando notificación, Pago capturado, Error)
        item {
            val (statusColor, badgeText, statusIcon) = when (currentStatus) {
                SystemServiceStatus.SERVICIO_ACTIVO -> Triple(Color(0xFF10B981), "🟢 Servicio activo", Icons.Default.CheckCircle)
                SystemServiceStatus.ESPERANDO_NOTIFICACION -> Triple(Color(0xFF0EA5E9), "🔵 Esperando notificación BDV", Icons.Default.Refresh)
                SystemServiceStatus.PAGO_CAPTURADO -> Triple(Color(0xFF059669), "✅ Pago capturado exitosamente", Icons.Default.Check)
                SystemServiceStatus.ERROR -> Triple(Color(0xFFEF4444), "🔴 Error o servicio inactivo", Icons.Default.Warning)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESTADO DEL SISTEMA (INDICADOR):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = badgeText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Requisito 5: BOTÓN "PROBAR CAPTURA BDV" PARA VALIDAR EL PARSER
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Acciones de Diagnóstico y Validación",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Ejecuta una prueba sintética en caliente para validar que las expresiones regulares extraigan correctamente monto, referencia y pagador sin enviar datos externos.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTestBdvParser,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probar captura BDV", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onTestConnection,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verificar estado", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Tarjeta: Última Notificación BDV
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Última Notificación Capturada:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        TextButton(onClick = onViewHistory) {
                            Text("Ver historial")
                        }
                    }

                    if (latestNotification == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aún no se ha recibido ninguna notificación BDV.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Haz un pago móvil de prueba o usa 'Probar captura BDV'",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        DetailRow(label = "Monto recibido:", value = latestNotification.monto ?: "------", isHighlight = true)
                        HorizontalDivider()
                        DetailRow(label = "Pagador:", value = latestNotification.pagador ?: "------")
                        HorizontalDivider()
                        DetailRow(label = "Referencia:", value = latestNotification.referencia ?: "------")
                        if (latestNotification.telefono != null) {
                            HorizontalDivider()
                            DetailRow(label = "Teléfono:", value = latestNotification.telefono)
                        }
                        HorizontalDivider()
                        DetailRow(
                            label = "Fecha y hora:",
                            value = "${latestNotification.fecha ?: ""} ${latestNotification.hora ?: ""}".trim().ifEmpty { "------" }
                        )
                        HorizontalDivider()
                        Column {
                            Text(
                                text = "Texto recibido del sistema:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = latestNotification.textoCompleto,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. HISTORIAL LOCAL DE ÚLTIMOS 20 PAGOS CAPTURADOS
 */
@Composable
fun PaymentHistoryScreen(
    history: List<PaymentNotification>,
    onSelectPayment: (PaymentNotification) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Historial de Pagos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${history.size} de 20 pagos almacenados localmente",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay pagos en el historial local",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Los pagos capturados por el servicio aparecerán aquí automáticamente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { payment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPayment(payment) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = payment.monto ?: "Monto N/A",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = payment.pagador ?: "Tel: ${payment.telefono ?: "Desconocido"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Ref: ${payment.referencia ?: "N/A"} • ${payment.fecha ?: ""} ${payment.hora ?: ""}".trim(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. REGISTRO INTERNO DE EVENTOS Y ERRORES
 */
@Composable
fun EventLogsScreen(
    logs: List<AppEventLog>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Registro de Eventos y Errores",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${logs.size} eventos en memoria para depuración de campo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (logs.isNotEmpty()) {
                TextButton(onClick = onClearLogs) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vaciar Logs")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay registros ni errores capturados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    val (badgeBg, badgeText, badgeColor) = when (log.level) {
                        LogLevel.SUCCESS -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "SUCCESS")
                        LogLevel.INFO -> Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), "INFO")
                        LogLevel.WARNING -> Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), "WARN")
                        LogLevel.ERROR -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "ERROR")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = log.formattedTime,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (log.details != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = if (isHighlight) 20.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
