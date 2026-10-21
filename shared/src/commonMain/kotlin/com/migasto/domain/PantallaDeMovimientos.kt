package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// En Android este armado vivía en MovimientosViewModel. Aquí sale de la vista para que Android y iPhone muestren lo
// mismo: cada pantalla recibe los grupos por día con sus textos ya hechos.

class GrupoDeMovimientos(
    /** `Hoy, jueves 1`, `Ayer, miércoles 30` o `Martes 15 de septiembre`. */
    val etiqueta: String,
    /** `S/ 144.70 en gastos`, o `null` si ese día no hubo gastos confirmados. */
    val totalDeGastos: String?,
    val filas: List<FilaDeMovimiento>,
)

/** Un chip del filtro por tipo. */
class OpcionDeTipo(val tipo: FiltroDeTipo, val texto: String, val esPorConfirmar: Boolean)

object TextosDeMovimientos {
    const val TITULO = "Movimientos"
    const val FILTRAR = "Filtrar movimientos"
    const val QUITAR_FILTROS = "Quitar filtros"
    const val LIMPIAR_FILTROS = "Limpiar filtros"
    const val APLICAR = "Aplicar"
    const val CATEGORIA = "Categoría"
    const val FUENTE = "Fuente"
    const val RANGO = "Rango de monto"
    const val TODAS = "Todas"

    /** Para el lector de pantalla, en la insignia del botón de filtros. */
    fun filtrosPuestos(cantidad: Int) = "$cantidad filtros puestos"
}

class PantallaDeMovimientos(
    val tipo: FiltroDeTipo,
    val busqueda: String,
    /** Cuántos movimientos esperan confirmación, con o sin filtro. */
    val cantidadPorConfirmar: Int,
    val grupos: List<GrupoDeMovimientos>,
    val hayFiltro: Boolean,
    /** Todos los filtros puestos: la hoja de filtros se abre con lo que ya está elegido. */
    val filtros: FiltrosDeMovimientos,
    val propias: List<CategoriaPropia>,
    /** Todo, Gastos, Ingresos y «Por confirmar · N». */
    val opcionesDeTipo: List<OpcionDeTipo>,
    /** Lo que se dice cuando no hay nada que mostrar (distinto si hay filtros puestos). */
    val textoSinMovimientos: String,
)

fun armarPantallaDeMovimientos(
    todos: List<Movimiento>,
    filtros: FiltrosDeMovimientos,
    alias: Map<String, String>,
    propias: List<CategoriaPropia>,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): PantallaDeMovimientos {
    val hoy = ahora.toLocalDateTime(zona).date
    val grupos = agruparPorDia(filtrarMovimientos(todos, filtros, alias, propias), ahora, zona).map { g ->
        GrupoDeMovimientos(
            etiqueta = etiquetaDelDia(g.dia, hoy),
            totalDeGastos = g.totalGastos.takeIf { it > 0 }?.let { "${formatearSoles(it)} en gastos" },
            filas = g.movimientos.map { crearFila(it, alias, propias, zona) },
        )
    }
    val porConfirmar = todos.count { it.estado == EstadoMovimiento.Pendiente }
    return PantallaDeMovimientos(
        tipo = filtros.tipo,
        busqueda = filtros.busqueda,
        cantidadPorConfirmar = porConfirmar,
        grupos = grupos,
        hayFiltro = filtros.hayAlgunFiltro,
        filtros = filtros,
        propias = propias,
        opcionesDeTipo = listOf(
            OpcionDeTipo(FiltroDeTipo.Todo, "Todo", false),
            OpcionDeTipo(FiltroDeTipo.Gastos, "Gastos", false),
            OpcionDeTipo(FiltroDeTipo.Ingresos, "Ingresos", false),
            OpcionDeTipo(FiltroDeTipo.PorConfirmar, "Por confirmar · $porConfirmar", true),
        ),
        textoSinMovimientos = if (filtros.hayAlgunFiltro) "No hay movimientos con esos filtros." else "Todavía no hay movimientos.",
    )
}

/** La pantalla de Movimientos con esos [filtros]; se vuelve a emitir cada vez que cambian los movimientos. */
fun observarPantallaDeMovimientos(base: BaseMiGasto, filtros: FiltrosDeMovimientos): Flow<PantallaDeMovimientos> =
    observarPantallaDeMovimientos(base, filtros, { Clock.System.now() }, TimeZone.currentSystemDefault())

fun observarPantallaDeMovimientos(base: BaseMiGasto, filtros: FiltrosDeMovimientos, reloj: () -> Instant, zona: TimeZone): Flow<PantallaDeMovimientos> =
    base.movimientos.observarTodos().map { todos ->
        armarPantallaDeMovimientos(todos, filtros, base.ajustes.aliases(), base.categoriasPropias.todas(), reloj(), zona)
    }
