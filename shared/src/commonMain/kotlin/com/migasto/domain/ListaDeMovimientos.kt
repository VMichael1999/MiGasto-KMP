package com.migasto.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Port de _filtered, _buildList y _dayLabel de lib/presentation/expenses/expenses_screen.dart: lo que
// la pantalla de Movimientos filtra, agrupa y rotula sale de `shared`.

enum class FiltroDeTipo { Todo, Gastos, Ingresos, PorConfirmar }

data class FiltrosDeMovimientos(
    val tipo: FiltroDeTipo = FiltroDeTipo.Todo,
    val busqueda: String = "",
    /** Clave de [claveDeCategoria]: la categoría propia o la de siempre. */
    val categoria: String? = null,
    val fuente: PaymentSource? = null,
    val montoMinimo: Double? = null,
    val montoMaximo: Double? = null,
) {
    /** Cuántos filtros de la hoja de filtros están puestos (el tipo y la búsqueda se cuentan aparte). */
    val cantidadDeFiltrosActivos: Int
        get() = listOf(categoria != null, fuente != null, montoMinimo != null || montoMaximo != null).count { it }

    val hayAlgunFiltro: Boolean
        get() = cantidadDeFiltrosActivos > 0 || busqueda.isNotBlank() || tipo != FiltroDeTipo.Todo
}

/** Los movimientos que pasan los [filtros], del más reciente al más antiguo. */
fun filtrarMovimientos(
    todos: List<Movimiento>,
    filtros: FiltrosDeMovimientos,
    alias: Map<String, String>,
    propias: List<CategoriaPropia>,
): List<Movimiento> {
    val consulta = filtros.busqueda.trim().lowercase()
    return todos.filter { m ->
        val delTipo = when (filtros.tipo) {
            FiltroDeTipo.Todo -> true
            FiltroDeTipo.Gastos -> m.esGasto
            FiltroDeTipo.Ingresos -> m.esIngreso
            FiltroDeTipo.PorConfirmar -> m.estado == EstadoMovimiento.Pendiente
        }
        delTipo &&
            (consulta.isEmpty() || consulta in m.merchant.lowercase() || consulta in nombreVisible(m.merchant, alias).lowercase()) &&
            (filtros.categoria == null || claveDeCategoria(m, propias) == filtros.categoria) &&
            (filtros.fuente == null || m.source == filtros.fuente) &&
            (filtros.montoMinimo == null || m.amount >= filtros.montoMinimo) &&
            (filtros.montoMaximo == null || m.amount <= filtros.montoMaximo)
    }.sortedByDescending { it.date }
}

data class GrupoDelDia(
    val dia: LocalDate,
    /** Suma de los gastos confirmados del día. */
    val totalGastos: Double,
    val movimientos: List<Movimiento>,
)

/** Agrupa por día en la hora local de [zona], del más reciente al más antiguo. */
fun agruparPorDia(
    movimientos: List<Movimiento>,
    @Suppress("UNUSED_PARAMETER") ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): List<GrupoDelDia> =
    movimientos.sortedByDescending { it.date }
        .groupBy { it.date.toLocalDateTime(zona).date }
        .map { (dia, delDia) ->
            GrupoDelDia(
                dia = dia,
                totalGastos = delDia.filter { it.esGasto && it.isConfirmed }.sumOf { it.amount },
                movimientos = delDia,
            )
        }

private val diasDeLaSemana = mapOf(
    DayOfWeek.MONDAY to "lunes", DayOfWeek.TUESDAY to "martes", DayOfWeek.WEDNESDAY to "miércoles",
    DayOfWeek.THURSDAY to "jueves", DayOfWeek.FRIDAY to "viernes", DayOfWeek.SATURDAY to "sábado",
    DayOfWeek.SUNDAY to "domingo",
)

/** `Hoy, miércoles 21`, `Ayer, martes 20` o `Jueves 15 de octubre`. */
fun etiquetaDelDia(dia: LocalDate, hoy: LocalDate): String {
    val diaDeLaSemana = diasDeLaSemana.getValue(dia.dayOfWeek)
    return when (dia) {
        hoy -> "Hoy, $diaDeLaSemana ${dia.day}"
        hoy.minus(1, DateTimeUnit.DAY) -> "Ayer, $diaDeLaSemana ${dia.day}"
        else -> "${diaDeLaSemana.replaceFirstChar { it.uppercase() }} ${dia.day} de ${nombreDelMes(dia.month.ordinal + 1).lowercase()}"
    }
}

/** Lo que ofrece la hoja «Filtrar movimientos» (port de `_showFilterSheet` de Flutter). */
object OpcionesDeFiltro {
    val fuentes: List<PaymentSource> = listOf(
        PaymentSource.Yape, PaymentSource.Plin, PaymentSource.GooglePay,
        PaymentSource.Tarjeta, PaymentSource.Efectivo, PaymentSource.Manual,
    )

    const val MONTO_MINIMO = 0.0
    const val MONTO_MAXIMO = 500.0

    /** El deslizador avanza de 10 en 10 (50 divisiones): 49 marcas entre los extremos. */
    const val PASOS_INTERMEDIOS = 49

    /** Para los ingresos, las categorías de ingreso; para lo demás, las de gasto. */
    fun categorias(tipo: FiltroDeTipo): List<Categoria> = if (tipo == FiltroDeTipo.Ingresos) Categoria.ingresos else Categoria.gastos

    fun propias(tipo: FiltroDeTipo, todas: List<CategoriaPropia>): List<CategoriaPropia> {
        val deIngreso = tipo == FiltroDeTipo.Ingresos
        return todas.filter { (it.tipo == TipoMovimiento.Ingreso) == deIngreso }
    }

    fun textoDelRango(desde: Double, hasta: Double): String = "S/ ${desde.toInt()} – S/ ${hasta.toInt()}"
}

/** Aplica lo elegido en la hoja; el tipo y la búsqueda no cambian. [rango] es `null` si no se tocó el deslizador. */
fun FiltrosDeMovimientos.conLaHoja(categoria: String?, fuente: PaymentSource?, rango: ClosedFloatingPointRange<Double>?) =
    copy(categoria = categoria, fuente = fuente, montoMinimo = rango?.start, montoMaximo = rango?.endInclusive)

/** Quita categoría, fuente y rango; el tipo y la búsqueda se quedan. */
fun FiltrosDeMovimientos.sinLoDeLaHoja() = copy(categoria = null, fuente = null, montoMinimo = null, montoMaximo = null)

/** El rango con el que se abre la hoja, o `null` si no hay rango puesto. Un extremo suelto toma el de la hoja. */
val FiltrosDeMovimientos.rangoDeLaHoja: ClosedFloatingPointRange<Double>?
    get() = if (montoMinimo == null && montoMaximo == null) {
        null
    } else {
        (montoMinimo ?: OpcionesDeFiltro.MONTO_MINIMO)..(montoMaximo ?: OpcionesDeFiltro.MONTO_MAXIMO)
    }
