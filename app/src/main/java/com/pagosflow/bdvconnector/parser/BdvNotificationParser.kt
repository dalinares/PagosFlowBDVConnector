package com.pagosflow.bdvconnector.parser

import com.pagosflow.bdvconnector.model.PaymentNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object BdvNotificationParser {

    /**
     * Paquetes conocidos de Banco de Venezuela en Google Play
     */
    val BDV_PACKAGES = setOf(
        "com.bancodevenezuela.bdvdigital",
        "com.bdv.app",
        "com.bancodevenezuela.bdvenlinea",
        "com.bancodevenezuela.app"
    )

    /**
     * Palabras clave requeridas para clasificar una notificación como PagomóvilBDV recibido
     */
    private const val KEYWORD_TITLE = "PagomóvilBDV recibido"
    private const val KEYWORD_FALLBACK = "PagomovilBDV"

    /**
     * Patrón 1: Con nombre de pagador y número de operación
     * Ejemplo: "Recibiste un PagomovilBDV de SILENE CECIRA DAVILA GONZALEZ por Bs.0,10 bajo el número de operación 007232795866"
     */
    private val PATTERN_NOMBRE_OPERACION = Pattern.compile(
        """Recibiste\s+un\s+PagomovilBDV\s+de\s+(?<pagador>.+?)\s+por\s+Bs\.?\s*(?<monto>[\d\.,]+)\s+bajo\s+el\s+número\s+de\s+operación\s+(?<referencia>\d+)""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    /**
     * Patrón 2: Con monto al inicio, teléfono, Ref:, fecha y hora
     * Ejemplo: "Recibiste un PagomovilBDV de Bs.9.400,00 del 04246185118 Ref:010926821362 en fecha 01-09-2026 hora:19:48"
     */
    private val PATTERN_MONTO_TELEFONO_REF = Pattern.compile(
        """Recibiste\s+un\s+PagomovilBDV\s+de\s+Bs\.?\s*(?<monto>[\d\.,]+)\s+del\s+(?<telefono>\d{10,11})\s+Ref:\s*(?<referencia>\d+)\s+en\s+fecha\s+(?<fecha>[\d\-\/]+)\s+hora:\s*(?<hora>[\d:]+)""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    /**
     * Expresiones regulares auxiliares de respaldo si la sintaxis del banco cambia ligeramente
     */
    private val REGEX_MONTO = Pattern.compile("""Bs\.?\s*([\d\.,]+)""", Pattern.CASE_INSENSITIVE)
    private val REGEX_REFERENCIA = Pattern.compile("""(?:Ref:|número de operación\s*)([0-9]{6,16})""", Pattern.CASE_INSENSITIVE)
    private val REGEX_TELEFONO = Pattern.compile("""\b(0412|0414|0424|0416|0426)\d{7}\b""")
    private val REGEX_FECHA = Pattern.compile("""\b(\d{2}[-\/]\d{2}[-\/]\d{4})\b""")
    private val REGEX_HORA = Pattern.compile("""\b(\d{1,2}:\d{2}(?::\d{2})?)\b""")

    /**
     * Valida si el paquete y el título/contenido corresponden exclusivamente a Banco de Venezuela y PagomóvilBDV
     */
    fun isBdvPaymentNotification(packageName: String?, title: String?, text: String?): Boolean {
        val safeTitle = title ?: ""
        val safeText = text ?: ""

        // 1. Si se conoce el paquete, validar pertenencia o verificar si el contenido contiene la firma estricta
        val isFromBdvApp = packageName != null && (BDV_PACKAGES.contains(packageName) || packageName.contains("bancodevenezuela") || packageName.contains("bdv"))

        // 2. Título o texto debe contener "PagomóvilBDV recibido" o mención inequívoca
        val hasBdvTitle = safeTitle.contains(KEYWORD_TITLE, ignoreCase = true) ||
                safeTitle.contains("PagomóvilBDV", ignoreCase = true) ||
                safeTitle.contains("PagomovilBDV", ignoreCase = true)

        val hasPaymentText = safeText.contains("PagomovilBDV", ignoreCase = true) ||
                safeText.contains("PagomóvilBDV", ignoreCase = true)

        // Verificamos que sea de Banco de Venezuela O que la firma del texto sea exacta a BDV
        return (isFromBdvApp && hasPaymentText) || (hasBdvTitle && hasPaymentText)
    }

    /**
     * Extrae los campos estructurados a partir del texto de la notificación
     */
    fun parse(text: String, packageName: String? = null): PaymentNotification? {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return null

        val now = Date()
        val defaultFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now)
        val defaultHora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

        // Intento 1: Patrón con nombre y número de operación
        val matcher1 = PATTERN_NOMBRE_OPERACION.matcher(cleanText)
        if (matcher1.find()) {
            return PaymentNotification(
                pagador = matcher1.group("pagador")?.trim(),
                monto = "Bs." + (matcher1.group("monto")?.trim() ?: ""),
                referencia = matcher1.group("referencia")?.trim(),
                telefono = null,
                fecha = defaultFecha,
                hora = defaultHora,
                textoCompleto = cleanText,
                paqueteOrigen = packageName
            )
        }

        // Intento 2: Patrón con monto, teléfono, Ref, fecha y hora
        val matcher2 = PATTERN_MONTO_TELEFONO_REF.matcher(cleanText)
        if (matcher2.find()) {
            return PaymentNotification(
                pagador = null,
                monto = "Bs." + (matcher2.group("monto")?.trim() ?: ""),
                referencia = matcher2.group("referencia")?.trim(),
                telefono = matcher2.group("telefono")?.trim(),
                fecha = matcher2.group("fecha")?.trim() ?: defaultFecha,
                hora = matcher2.group("hora")?.trim() ?: defaultHora,
                textoCompleto = cleanText,
                paqueteOrigen = packageName
            )
        }

        // Intento 3: Extracción heurística resiliente
        var extractedMonto: String? = null
        val montoMatcher = REGEX_MONTO.matcher(cleanText)
        if (montoMatcher.find()) {
            extractedMonto = "Bs." + montoMatcher.group(1)?.trim()
        }

        var extractedRef: String? = null
        val refMatcher = REGEX_REFERENCIA.matcher(cleanText)
        if (refMatcher.find()) {
            extractedRef = refMatcher.group(1)?.trim()
        }

        var extractedTel: String? = null
        val telMatcher = REGEX_TELEFONO.matcher(cleanText)
        if (telMatcher.find()) {
            extractedTel = telMatcher.group(0)?.trim()
        }

        var extractedFecha: String? = null
        val fechaMatcher = REGEX_FECHA.matcher(cleanText)
        if (fechaMatcher.find()) {
            extractedFecha = fechaMatcher.group(1)?.trim()
        }

        var extractedHora: String? = null
        val horaMatcher = REGEX_HORA.matcher(cleanText)
        if (horaMatcher.find()) {
            extractedHora = horaMatcher.group(1)?.trim()
        }

        return PaymentNotification(
            pagador = null,
            monto = extractedMonto,
            referencia = extractedRef,
            telefono = extractedTel,
            fecha = extractedFecha ?: defaultFecha,
            hora = extractedHora ?: defaultHora,
            textoCompleto = cleanText,
            paqueteOrigen = packageName
        )
    }
}
