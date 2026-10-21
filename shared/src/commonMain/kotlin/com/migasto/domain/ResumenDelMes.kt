package com.migasto.domain

import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// En Flutter estas cuentas vivían dentro de DashboardScreen (lib/presentation/dashboard). Aquí salen
// de `shared`, para que ninguna vista calcule montos.

enum class EstadoDelPresupuesto { Bien, Cerca, Pasado }

data class ResumenDelMes(
    /** Ingresos confirmados del mes. */
    val ingresos: Double,
    /** Gastos confirmados del mes. */
    val gastos: Double,
    /** Lo que queda: ingresos menos gastos. */
    val saldo: Double,
    /** Suma de todo lo que espera confirmación (de cualquier mes y tipo). */
    val porConfirmar: Double,
    val cantidadPorConfirmar: Int,
    val presupuesto: Double,
    /** Qué parte del mes ya pasó (0 a 1), para la marca de la barra del presupuesto. */
    val fraccionDelMes: Double,
) {
    val tienePresupuesto: Boolean get() = presupuesto > 0

    /** Gastos sobre presupuesto, en porcentaje entero; 0 si no hay presupuesto. */
    val porcentajeDelPresupuesto: Int
        get() = if (tienePresupuesto) (gastos / presupuesto * 100).roundToInt() else 0

    val estadoDelPresupuesto: EstadoDelPresupuesto
        get() {
            val razon = if (tienePresupuesto) gastos / presupuesto else 0.0
            return when {
                razon > 1.0 -> EstadoDelPresupuesto.Pasado
                razon >= 0.75 -> EstadoDelPresupuesto.Cerca
                else -> EstadoDelPresupuesto.Bien
            }
        }
}

/** Solo suma lo confirmado del mes de [ahora] (en la hora local de [zona]); lo pendiente va aparte. */
fun calcularResumenDelMes(
    movimientos: Iterable<Movimiento>,
    presupuesto: Double,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): ResumenDelMes {
    val hoy = ahora.toLocalDateTime(zona).date
    fun fecha(m: Movimiento) = m.date.toLocalDateTime(zona).date

    val delMes = movimientos.filter { it.isConfirmed && fecha(it).let { f -> f.year == hoy.year && f.month == hoy.month } }
    val gastos = delMes.filter { it.esGasto }.sumOf { it.amount }
    val ingresos = delMes.filter { it.esIngreso }.sumOf { it.amount }
    val pendientes = movimientos.filter { it.estado == EstadoMovimiento.Pendiente }

    return ResumenDelMes(
        ingresos = ingresos,
        gastos = gastos,
        saldo = ingresos - gastos,
        porConfirmar = pendientes.sumOf { it.amount },
        cantidadPorConfirmar = pendientes.size,
        presupuesto = presupuesto,
        fraccionDelMes = (hoy.day.toDouble() / diasDelMes(hoy)).coerceIn(0.0, 1.0),
    )
}

private fun diasDelMes(f: LocalDate): Int {
    val bisiesto = f.year % 4 == 0 && (f.year % 100 != 0 || f.year % 400 == 0)
    return when (f.month.ordinal + 1) {
        2 -> if (bisiesto) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
