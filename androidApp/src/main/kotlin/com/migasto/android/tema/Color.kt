package com.migasto.android.tema

import androidx.compose.ui.graphics.Color

/**
 * Colores de MiGasto (oscuro y claro). Son los de `lib/core/theme/app_colors.dart` y `theme.dart`
 * de la app de Flutter, con los mismos nombres.
 */
data class ColoresMiGasto(
    val fondo: Color,
    val superficie: Color,
    val tinta: Color,
    val apagado: Color,
    val linea: Color,
    val error: Color,
    /** Verde de marca como texto o ícono (en claro es más oscuro). */
    val marcaTinta: Color,
    val marcaSuave: Color,
    val elevado: Color,
    /** Fondo del cuadrito del ícono de una fila (se distingue de la tarjeta en claro y en oscuro). */
    val fondoDeIcono: Color,
    val presupuestoBien: Color,
    val presupuestoBienSuave: Color,
    val presupuestoCerca: Color,
    val presupuestoCercaSuave: Color,
    val presupuestoPasado: Color,
    val presupuestoPasadoSuave: Color,
    val ingreso: Color,
    val ingresoSuave: Color,
    val porConfirmar: Color,
    val porConfirmarSuave: Color,
    val barra: Color,
    val barraDeGastos: Color,
    val yape: Color,
    val plin: Color,
    val tarjeta: Color,
    val googlePay: Color,
    val manual: Color,
) {
    companion object {
        /** El verde de marca de los botones principales y el botón de agregar. */
        val VerdeNeon = Color(0xFF8CE885)
        val TintaSobreVerde = Color(0xFF0F0E13)

        val Oscuro = ColoresMiGasto(
            fondo = Color(0xFF0F0E13), superficie = Color(0xFF1B1922), tinta = Color(0xFFF2F0F5),
            apagado = Color(0xFFA5A1B0), linea = Color(0xFF2A2733), error = Color(0xFFF07070),
            marcaTinta = Color(0xFF8CE885), marcaSuave = Color(0xFF1D2A1E), elevado = Color(0xFF23212B), fondoDeIcono = Color(0xFF23212B),
            presupuestoBien = Color(0xFF3FCB5B), presupuestoBienSuave = Color(0xFF16261A),
            presupuestoCerca = Color(0xFFFAB219), presupuestoCercaSuave = Color(0xFF2E2412),
            presupuestoPasado = Color(0xFFF07070), presupuestoPasadoSuave = Color(0xFF321A1C),
            ingreso = Color(0xFF7FB6FF), ingresoSuave = Color(0xFF18233A),
            porConfirmar = Color(0xFF7FB6FF), porConfirmarSuave = Color(0xFF18233A),
            barra = Color(0xFF4A4656), barraDeGastos = Color(0xFF9A96A6),
            yape = Color(0xFF9B4FD6), plin = Color(0xFF10BFAF), tarjeta = Color(0xFFE8B04A),
            googlePay = Color(0xFF5B9BF8), manual = Color(0xFF8A8794),
        )

        val Claro = ColoresMiGasto(
            fondo = Color(0xFFF4F3F6), superficie = Color(0xFFFFFFFF), tinta = Color(0xFF16141B),
            apagado = Color(0xFF5B5866), linea = Color(0xFFE1DFE6), error = Color(0xFFC23434),
            marcaTinta = Color(0xFF1E7A36), marcaSuave = Color(0xFFE4F6E1), elevado = Color(0xFFFFFFFF), fondoDeIcono = Color(0xFFF1F0F4),
            presupuestoBien = Color(0xFF1C8A34), presupuestoBienSuave = Color(0xFFE4F6E1),
            presupuestoCerca = Color(0xFF9A6400), presupuestoCercaSuave = Color(0xFFFCEFD2),
            presupuestoPasado = Color(0xFFC23434), presupuestoPasadoSuave = Color(0xFFFBE4E4),
            ingreso = Color(0xFF2458C6), ingresoSuave = Color(0xFFE3EBFA),
            porConfirmar = Color(0xFF2458C6), porConfirmarSuave = Color(0xFFE3EBFA),
            barra = Color(0xFFCFCCD7), barraDeGastos = Color(0xFF8B8896),
            yape = Color(0xFF742284), plin = Color(0xFF00857A), tarjeta = Color(0xFFA56A00),
            googlePay = Color(0xFF1A73E8), manual = Color(0xFF6B6876),
        )
    }
}
