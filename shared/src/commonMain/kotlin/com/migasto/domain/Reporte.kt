package com.migasto.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Instant

// Port de la lógica de lib/presentation/reports/reports_screen.dart: períodos, comparación con el período
// anterior, barras por mes y tablas por categoría. Las pantallas solo dibujan.

enum class ModoDeReporte { Mes, Anio, Rango }

/** Qué período mira el usuario. Es inmutable: cada cambio devuelve otra selección. */
data class SeleccionDeReporte(
    val modo: ModoDeReporte,
    val anio: Int,
    /** 1 a 12; solo cuenta en el modo [ModoDeReporte.Mes]. */
    val mes: Int,
    val desde: LocalDate,
    val hasta: LocalDate,
) {
    fun conModo(nuevo: ModoDeReporte) = copy(modo = nuevo)

    fun conRango(desde: LocalDate, hasta: LocalDate) = copy(modo = ModoDeReporte.Rango, desde = desde, hasta = hasta)

    /** Pasa al mes o año anterior (`-1`) o siguiente (`1`). El rango no se desplaza. */
    fun desplazar(delta: Int): SeleccionDeReporte = when (modo) {
        ModoDeReporte.Mes -> {
            val primero = LocalDate(anio, mes, 1).plus(DatePeriod(months = delta))
            copy(anio = primero.year, mes = primero.month.ordinal + 1)
        }
        ModoDeReporte.Anio -> copy(anio = anio + delta)
        ModoDeReporte.Rango -> this
    }

    /** Solo se puede ir al siguiente si ese mes (o año) ya empezó. */
    fun puedeAvanzar(hoy: LocalDate): Boolean = when (modo) {
        ModoDeReporte.Mes -> LocalDate(anio, mes, 1).plus(DatePeriod(months = 1)) <= hoy
        ModoDeReporte.Anio -> anio < hoy.year
        ModoDeReporte.Rango -> false
    }

    /** Inicio (incluido) y fin (excluido) del período. */
    internal val periodo: Pair<LocalDate, LocalDate>
        get() = when (modo) {
            ModoDeReporte.Mes -> LocalDate(anio, mes, 1).let { it to it.plus(DatePeriod(months = 1)) }
            ModoDeReporte.Anio -> LocalDate(anio, 1, 1) to LocalDate(anio + 1, 1, 1)
            ModoDeReporte.Rango -> desde to hasta.plus(1, DateTimeUnit.DAY)
        }

    /** El período anterior de la misma duración, para comparar. */
    internal val anterior: Pair<LocalDate, LocalDate>
        get() {
            val (inicio, fin) = periodo
            return when (modo) {
                ModoDeReporte.Mes -> inicio.minus(DatePeriod(months = 1)) to inicio
                ModoDeReporte.Anio -> LocalDate(inicio.year - 1, 1, 1) to inicio
                ModoDeReporte.Rango -> inicio.minus(inicio.daysUntil(fin), DateTimeUnit.DAY) to inicio
            }
        }

    companion object {
        fun delMesDe(hoy: LocalDate) =
            SeleccionDeReporte(ModoDeReporte.Mes, hoy.year, hoy.month.ordinal + 1, hoy.minus(30, DateTimeUnit.DAY), hoy)
    }
}

data class ComparacionDeReporte(
    /** Saldo de este período menos el del anterior. */
    val diferencia: Double,
    /** Cómo se llama el período anterior: `septiembre`, `2025` o `el período anterior`. */
    val anterior: String,
) {
    /** `S/ 780.70 más que en septiembre`; [monto] permite poner `S/ ••••` con los montos ocultos. */
    fun texto(monto: String = formatearSoles(abs(diferencia))): String {
        if (abs(diferencia) < 0.005) return "Igual que en $anterior"
        return "$monto ${if (diferencia > 0) "más" else "menos"} que en $anterior"
    }
}

data class CuboMensual(val mes: Int, val etiqueta: String, val ingresos: Double, val gastos: Double, val esUltimo: Boolean)

data class FilaDeReporte(val clave: String, val etiqueta: String, val monto: Double)

data class Reporte(
    val etiquetaDelPeriodo: String,
    /** `Octubre`, `2026` o `este rango`: va en los títulos de las tablas. */
    val nombreDelPeriodo: String,
    val textoDelSaldo: String,
    val ingresos: Double,
    val gastos: Double,
    val saldo: Double,
    val saldoNegativo: Boolean,
    val comparacion: ComparacionDeReporte?,
    val cubos: List<CuboMensual>,
    /** El tope del eje vertical: el siguiente múltiplo de 1,000, y nunca menos de 1,000. */
    val topDelEje: Double,
    val descripcionDelGrafico: String,
    val tituloDeIngresos: String,
    val tituloDeGastos: String,
    val ingresosPorCategoria: List<FilaDeReporte>,
    val gastosPorCategoria: List<FilaDeReporte>,
    val textoSinIngresos: String = "Sin ingresos en este período.",
    val textoSinGastos: String = "Sin gastos en este período.",
)

private val mesesLargos = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val mesesDelGrafico = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic")
private val mesesCortosEnRango = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

private fun LocalDate.mesNumero() = month.ordinal + 1

fun calcularReporte(
    movimientos: List<Movimiento>,
    seleccion: SeleccionDeReporte,
    propias: List<CategoriaPropia>,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): Reporte {
    val hoy = ahora.toLocalDateTime(zona).date
    val confirmados = movimientos.filter { it.isConfirmed }
    fun dia(m: Movimiento) = m.date.toLocalDateTime(zona).date
    fun dentro(p: Pair<LocalDate, LocalDate>) = confirmados.filter { dia(it) >= p.first && dia(it) < p.second }

    val actuales = dentro(seleccion.periodo)
    val previos = dentro(seleccion.anterior)
    fun suma(l: List<Movimiento>, tipo: TipoMovimiento) = l.filter { it.tipo == tipo }.sumOf { it.amount }
    val ingresos = suma(actuales, TipoMovimiento.Ingreso)
    val gastos = suma(actuales, TipoMovimiento.Gasto)
    val saldo = ingresos - gastos
    val saldoPrevio = suma(previos, TipoMovimiento.Ingreso) - suma(previos, TipoMovimiento.Gasto)

    val nombreDelMes = mesesLargos[seleccion.mes - 1]
    val esMesActual = seleccion.modo == ModoDeReporte.Mes && seleccion.anio == hoy.year && seleccion.mes == hoy.mesNumero()
    val esPasado = !esMesActual && seleccion.modo != ModoDeReporte.Rango

    val textoDelSaldo = when (seleccion.modo) {
        ModoDeReporte.Mes -> "En $nombreDelMes te ${if (esPasado) "quedaron" else "quedan"}"
        ModoDeReporte.Anio -> "En ${seleccion.anio} te ${if (seleccion.anio < hoy.year) "quedaron" else "quedan"}"
        ModoDeReporte.Rango -> "En este rango te quedaron"
    }
    val comparacion = if (previos.isEmpty()) {
        null
    } else {
        ComparacionDeReporte(
            diferencia = saldo - saldoPrevio,
            anterior = when (seleccion.modo) {
                ModoDeReporte.Mes -> mesesLargos[seleccion.anterior.first.mesNumero() - 1]
                ModoDeReporte.Anio -> "${seleccion.anio - 1}"
                ModoDeReporte.Rango -> "el período anterior"
            },
        )
    }

    val cubos = cubosDelGrafico(confirmados, seleccion, ::dia)
    val maximo = cubos.fold(0.0) { m, b -> max(m, max(b.ingresos, b.gastos)) }
    val descripcion = "Ingresos y gastos por mes. " +
        cubos.joinToString(". ") { "${it.etiqueta}: ${formatearMonto(it.ingresos)} de ingresos y ${formatearMonto(it.gastos)} de gastos" } +
        " soles."

    val sufijo = when (seleccion.modo) {
        ModoDeReporte.Mes -> nombreDelMes
        ModoDeReporte.Anio -> "${seleccion.anio}"
        ModoDeReporte.Rango -> "este rango"
    }
    fun porCategoria(l: List<Movimiento>): List<FilaDeReporte> =
        l.groupBy { claveDeCategoria(it, propias) }
            .map { (clave, ms) ->
                FilaDeReporte(
                    clave,
                    if (clave == Categoria.TransferenciaRecibida.clave) "Transferencias recibidas" else etiquetaDeClave(clave, propias),
                    ms.sumOf { it.amount },
                )
            }
            .sortedByDescending { it.monto }

    return Reporte(
        etiquetaDelPeriodo = when (seleccion.modo) {
            ModoDeReporte.Mes -> "${nombreDelMes.replaceFirstChar { it.uppercase() }} ${seleccion.anio}"
            ModoDeReporte.Anio -> "${seleccion.anio}"
            ModoDeReporte.Rango -> "${diaYMes(seleccion.desde)} – ${diaYMes(seleccion.hasta)}"
        },
        nombreDelPeriodo = if (seleccion.modo == ModoDeReporte.Mes) nombreDelMes.replaceFirstChar { it.uppercase() } else sufijo,
        textoDelSaldo = textoDelSaldo,
        ingresos = ingresos,
        gastos = gastos,
        saldo = saldo,
        saldoNegativo = saldo < 0,
        comparacion = comparacion,
        cubos = cubos,
        topDelEje = max(1000.0, ceil(maximo / 1000) * 1000.0),
        descripcionDelGrafico = descripcion,
        tituloDeIngresos = "Ingresos de $sufijo",
        tituloDeGastos = "Gastos de $sufijo",
        ingresosPorCategoria = porCategoria(actuales.filter { it.esIngreso }),
        gastosPorCategoria = porCategoria(actuales.filter { it.esGasto }),
    )
}

private fun diaYMes(f: LocalDate) = "${f.day} ${mesesCortosEnRango[f.mesNumero() - 1]}"

private fun cubosDelGrafico(confirmados: List<Movimiento>, s: SeleccionDeReporte, dia: (Movimiento) -> LocalDate): List<CuboMensual> {
    val meses: List<LocalDate> = when (s.modo) {
        ModoDeReporte.Mes -> (3 downTo 0).map { LocalDate(s.anio, s.mes, 1).minus(DatePeriod(months = it)) }
        ModoDeReporte.Anio -> (1..12).map { LocalDate(s.anio, it, 1) }
        ModoDeReporte.Rango -> {
            val (inicio, fin) = s.periodo
            generateSequence(LocalDate(inicio.year, inicio.month.ordinal + 1, 1)) { it.plus(DatePeriod(months = 1)) }
                .takeWhile { it < fin }.take(12).toList()
        }
    }
    return meses.mapIndexed { i, mes ->
        val delMes = confirmados.filter { dia(it).year == mes.year && dia(it).month == mes.month }
        CuboMensual(
            mes = mes.mesNumero(),
            etiqueta = mesesDelGrafico[mes.mesNumero() - 1],
            ingresos = delMes.filter { it.esIngreso }.sumOf { it.amount },
            gastos = delMes.filter { it.esGasto }.sumOf { it.amount },
            esUltimo = i == meses.lastIndex,
        )
    }
}
