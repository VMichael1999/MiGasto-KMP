package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

class OpcionDePeriodo(val periodo: PeriodoDeUbicaciones, val texto: String)

/** Una zona de la lista de «Dónde pagaste», con su subtítulo y su total ya armados. */
class FilaDeZona(
    val nombre: String,
    val cantidad: Int,
    val subtitulo: String,
    val total: Double,
    /** `S/ 105.50`. */
    val totalTexto: String,
    private val zona: ZonaDePagos,
    private val alias: Map<String, String>,
) {
    val latitudDelCentro: Double get() = zona.latitudDelCentro
    val longitudDelCentro: Double get() = zona.longitudDelCentro

    /** Lo que lee el lector de pantalla; con los montos ocultos no dice el total. */
    fun descripcion(ocultarMontos: Boolean): String = zona.descripcion(alias, ocultarMontos)
}

/** Lo que muestra «Dónde pagaste»: el período, las zonas de la lista y los límites para encuadrar el mapa. */
class PantallaDeDondePagaste(
    val titulo: String,
    val periodo: PeriodoDeUbicaciones,
    val opcionesDePeriodo: List<OpcionDePeriodo>,
    /** Las zonas con todos sus pagos, para dibujar el mapa. */
    val zonas: List<ZonaDePagos>,
    val filas: List<FilaDeZona>,
    val limites: LimitesDelMapa?,
    val textoVacio: String,
    val pie: String,
)

fun armarPantallaDeDondePagaste(zonas: List<ZonaDePagos>, periodo: PeriodoDeUbicaciones, alias: Map<String, String>): PantallaDeDondePagaste =
    PantallaDeDondePagaste(
        titulo = TextosDeDondePagaste.TITULO,
        periodo = periodo,
        opcionesDePeriodo = PeriodoDeUbicaciones.entries.map { OpcionDePeriodo(it, it.etiqueta) },
        zonas = zonas,
        filas = zonas.map {
            FilaDeZona(it.nombre, it.movimientos.size, it.subtitulo(alias), it.totalGastos, formatearSoles(it.totalGastos), it, alias)
        },
        limites = limitesDelMapa(zonas),
        textoVacio = TextosDeDondePagaste.VACIO,
        pie = TextosDeDondePagaste.PIE,
    )

/** Observa los movimientos y arma «Dónde pagaste» para el período elegido. Lo comparten Android e iPhone. */
class SeguimientoDeDondePagaste(
    private val base: BaseMiGasto,
    private val reloj: () -> Instant,
    private val zona: TimeZone,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(base: BaseMiGasto) : this(base, { Clock.System.now() }, TimeZone.currentSystemDefault())

    private val periodo = MutableStateFlow(PeriodoDeUbicaciones.Semana)

    val estado: Flow<PantallaDeDondePagaste> = combine(base.movimientos.observarTodos(), periodo) { movimientos, elegido ->
        armarPantallaDeDondePagaste(agruparPorZona(movimientos, elegido, reloj(), zona), elegido, base.ajustes.aliases())
    }

    fun elegirPeriodo(nuevo: PeriodoDeUbicaciones) = periodo.update { nuevo }
}
