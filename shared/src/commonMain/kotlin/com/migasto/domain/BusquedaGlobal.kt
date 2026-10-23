package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object TextosDeLaBusqueda {
    const val TITULO = "Buscar"
    const val PLACEHOLDER = "Buscar movimientos"
    const val ESCRIBE = "Escribe un comercio o un nombre para buscar entre todos tus movimientos."

    fun sinResultados(texto: String) = "No encontramos movimientos con «$texto»."
}

/** Lo que muestra la búsqueda de la barra: los movimientos que coinciden, por día, con sus textos ya armados. */
class ResultadosDeBusqueda(
    val texto: String,
    val grupos: List<GrupoDeMovimientos>,
    val cantidad: Int,
    /** `3 resultados`, o `null` si no hay nada que contar. */
    val resumen: String?,
    /** Lo que se dice cuando no hay resultados (o aún no se escribió nada), o `null`. */
    val mensaje: String?,
)

/** Busca [texto] (comercio o nombre guardado, sin importar mayúsculas) entre todos los movimientos, sin otros filtros. */
fun armarResultadosDeBusqueda(
    todos: List<Movimiento>,
    texto: String,
    alias: Map<String, String>,
    propias: List<CategoriaPropia>,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): ResultadosDeBusqueda {
    val consulta = texto.trim()
    if (consulta.isEmpty()) return ResultadosDeBusqueda(consulta, emptyList(), 0, null, TextosDeLaBusqueda.ESCRIBE)
    val hoy = ahora.toLocalDateTime(zona).date
    val encontrados = filtrarMovimientos(todos, filtrosVacios().conBusqueda(consulta), alias, propias)
    if (encontrados.isEmpty()) {
        return ResultadosDeBusqueda(consulta, emptyList(), 0, null, TextosDeLaBusqueda.sinResultados(consulta))
    }
    val grupos = agruparPorDia(encontrados, ahora, zona).map { g ->
        GrupoDeMovimientos(
            etiqueta = etiquetaDelDia(g.dia, hoy),
            totalDeGastos = g.totalGastos.takeIf { it > 0 }?.let { "${formatearSoles(it)} en gastos" },
            filas = g.movimientos.map { crearFila(it, alias, propias, zona) },
        )
    }
    val cantidad = encontrados.size
    return ResultadosDeBusqueda(consulta, grupos, cantidad, if (cantidad == 1) "1 resultado" else "$cantidad resultados", null)
}

/** Los resultados de buscar [texto]; se vuelve a emitir cada vez que cambian los movimientos. */
fun observarBusqueda(base: BaseMiGasto, texto: String): Flow<ResultadosDeBusqueda> =
    observarBusqueda(base, texto, { Clock.System.now() }, TimeZone.currentSystemDefault())

fun observarBusqueda(base: BaseMiGasto, texto: String, reloj: () -> Instant, zona: TimeZone): Flow<ResultadosDeBusqueda> =
    base.movimientos.observarTodos().map { todos ->
        armarResultadosDeBusqueda(todos, texto, base.ajustes.aliases(), base.categoriasPropias.todas(), reloj(), zona)
    }
