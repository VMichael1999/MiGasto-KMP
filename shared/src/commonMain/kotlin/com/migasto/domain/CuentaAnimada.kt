package com.migasto.domain

import kotlin.math.pow

/**
 * La animación de los montos grandes del Resumen al volver a mostrarlos: el número sube desde 0 hasta su valor. Es la de
 * `BigAmount` en Flutter (900 ms, `easeOutCubic`); la vista solo avanza el tiempo y pide el texto de cada instante.
 */
object CuentaAnimada {
    const val DURACION_EN_MILISEGUNDOS = 900

    /** Qué parte del camino lleva el número cuando pasó [t] (de 0 a 1) del tiempo. */
    fun progreso(t: Double): Double = 1.0 - (1.0 - t.coerceIn(0.0, 1.0)).pow(3)

    /** El monto en el instante [t], sin prefijo: `2,323.39`. */
    fun texto(valor: Double, t: Double): String = formatearMonto(valor * progreso(t))

    /** Como [texto], con `S/ ` delante (las tarjetas de Ingresos y Gastos). */
    fun textoConSoles(valor: Double, t: Double): String = formatearSoles(valor * progreso(t))
}
