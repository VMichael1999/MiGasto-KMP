package com.migasto.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Nuevo: Flutter solo mostraba el mes en curso; el diseño (mg-01) trae «Octubre ⌄». Aquí se decide qué ofrece.

/** Un mes del calendario (`mes` de 1 a 12). */
data class MesElegido(val anio: Int, val mes: Int) {
    internal val indice: Int get() = anio * 12 + mes - 1

    internal fun esAnteriorA(hoy: LocalDate) = indice < hoy.year * 12 + hoy.month.ordinal
}

private const val MAXIMO_DE_MESES = 12

private fun mesDe(ahora: Instant, zona: TimeZone) = ahora.toLocalDateTime(zona).date.let { MesElegido(it.year, it.month.ordinal + 1) }

/** El mes en curso y cada mes anterior que tiene movimientos, del más nuevo al más viejo (hasta 12). */
fun mesesParaElegir(movimientos: Iterable<Movimiento>, ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): List<MesElegido> {
    val actual = mesDe(ahora, zona)
    val conMovimientos = movimientos
        .map { it.date.toLocalDateTime(zona).date.let { f -> MesElegido(f.year, f.month.ordinal + 1) } }
        .filter { it.indice <= actual.indice }
        .toSet()
    return (conMovimientos + actual).sortedByDescending { it.indice }.take(MAXIMO_DE_MESES)
}

private fun esElActual(mes: MesElegido, ahora: Instant, zona: TimeZone) = mes == mesDe(ahora, zona)

private fun nombreEnMinuscula(mes: MesElegido, ahora: Instant, zona: TimeZone): String {
    val nombre = nombreDelMes(mes.mes).lowercase()
    return if (mes.anio == mesDe(ahora, zona).anio) nombre else "$nombre de ${mes.anio}"
}

/** `Octubre`, o `Diciembre 2025` si no es de este año. */
fun nombreDelMesElegido(mes: MesElegido, ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    nombreDelMes(mes.mes) + if (mes.anio == mesDe(ahora, zona).anio) "" else " ${mes.anio}"

fun textoDelSaldoDelResumen(mes: MesElegido, ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    if (esElActual(mes, ahora, zona)) "Te quedan de este mes" else "Te quedaron en ${nombreEnMinuscula(mes, ahora, zona)}"

fun tituloDeLaListaDelResumen(mes: MesElegido, ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    if (esElActual(mes, ahora, zona)) "Hoy" else "Últimos de ${nombreEnMinuscula(mes, ahora, zona)}"

fun textoSinMovimientosEnElResumen(mes: MesElegido, ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    if (esElActual(mes, ahora, zona)) "Todavía no hay movimientos hoy." else "No hay movimientos en ${nombreEnMinuscula(mes, ahora, zona)}."
