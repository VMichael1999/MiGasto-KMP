package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// Port de la agrupación de lib/presentation/expenses/where_screen.dart (_Zone y el filtro por período).

enum class PeriodoDeUbicaciones(val etiqueta: String) { Semana("Semana"), Mes("Mes"), Todo("Todo") }

object TextosDeDondePagaste {
    const val TITULO = "Dónde pagaste"
    const val VACIO = "Todavía no hay pagos con ubicación en este período. Agrégala desde el detalle de un movimiento."
    const val PIE = "Solo aparecen los pagos con ubicación. Los que no la tienen no se muestran."
    const val SIN_CLAVE = "El mapa necesita la clave de Google Maps."
}

private const val SIN_DIRECCION = "Sin dirección"

data class ZonaDePagos(val nombre: String, val movimientos: List<Movimiento>) {
    val totalGastos: Double get() = movimientos.filter { it.esGasto }.sumOf { it.amount }
    val latitudDelCentro: Double get() = movimientos.sumOf { it.latitud!! } / movimientos.size
    val longitudDelCentro: Double get() = movimientos.sumOf { it.longitud!! } / movimientos.size

    /** `2 pagos · S/ 105.50`. */
    val resumen: String get() = "${movimientos.size} ${if (movimientos.size == 1) "pago" else "pagos"} · ${formatearSoles(totalGastos)}"

    /** «Wong y 2 más»: el comercio de mayor monto y cuántos hay además. */
    fun subtitulo(alias: Map<String, String>): String {
        val mayor = movimientos.maxByOrNull { it.amount }!!
        val otros = movimientos.size - 1
        val nombre = nombreVisible(mayor.merchant, alias)
        return if (otros <= 0) nombre else "$nombre y $otros más"
    }

    /** Lo que lee el lector de pantalla. */
    fun descripcion(alias: Map<String, String>): String =
        "$nombre, ${movimientos.size} ${if (movimientos.size == 1) "pago" else "pagos"}, ${formatearSoles(totalGastos)}. ${subtitulo(alias)}"
}

/** Los pagos con ubicación del [periodo], por zona; primero la zona con más pagos. */
fun agruparPorZona(
    movimientos: Iterable<Movimiento>,
    periodo: PeriodoDeUbicaciones,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): List<ZonaDePagos> {
    val hoy = ahora.toLocalDateTime(zona)
    fun dentro(fecha: Instant) = when (periodo) {
        PeriodoDeUbicaciones.Semana -> fecha > ahora - 7.days
        PeriodoDeUbicaciones.Mes -> fecha.toLocalDateTime(zona).let { it.year == hoy.year && it.month == hoy.month }
        PeriodoDeUbicaciones.Todo -> true
    }
    fun nombreDeLaZona(lugar: String?) = when {
        lugar == null -> SIN_DIRECCION
        ',' in lugar -> lugar.substringAfterLast(',').trim()
        else -> lugar
    }
    return movimientos
        .filter { it.tieneUbicacion && dentro(it.date) }
        .groupBy { nombreDeLaZona(it.lugar) }
        .map { (nombre, lista) -> ZonaDePagos(nombre, lista) }
        .sortedByDescending { it.movimientos.size }
}

/** El rectángulo que abarca todos los pagos, para encuadrar el mapa. */
data class LimitesDelMapa(val sur: Double, val norte: Double, val oeste: Double, val este: Double, val puntos: Int)

fun limitesDelMapa(zonas: List<ZonaDePagos>): LimitesDelMapa? {
    val todos = zonas.flatMap { it.movimientos }
    if (todos.isEmpty()) return null
    val lat = todos.map { it.latitud!! }
    val lng = todos.map { it.longitud!! }
    return LimitesDelMapa(lat.min(), lat.max(), lng.min(), lng.max(), todos.size)
}
