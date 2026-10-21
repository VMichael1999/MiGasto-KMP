package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object TextosDeReportes {
    const val TITULO = "Reportes"
    const val CAMBIAR = "Cambiar"
    const val PERIODO_ANTERIOR = "Período anterior"
    const val PERIODO_SIGUIENTE = "Período siguiente"
    const val INGRESOS = "Ingresos"
    const val GASTOS = "Gastos"
    const val ACEPTAR = "Aceptar"
    const val CANCELAR = "Cancelar"
}

class OpcionDeModo(val modo: ModoDeReporte, val texto: String)

/** Lo que muestra Reportes: el reporte del período elegido, ya con sus textos, y qué se puede hacer con el período. */
class PantallaDeReportes(
    val reporte: Reporte,
    val seleccion: SeleccionDeReporte,
    val modo: ModoDeReporte,
    val puedeAvanzar: Boolean,
    /** El rango no se desplaza: solo el mes y el año. */
    val puedeRetroceder: Boolean,
    /** `yyyy-MM-dd`, para abrir el selector de fechas del rango. */
    val desde: String,
    val hasta: String,
    /** `S/ 780.70 más que en septiembre`; `null` si no hay período anterior con movimientos. */
    val textoDeLaComparacion: String?,
    /** Lo mismo con el monto tapado (`S/ ••••`), para cuando los montos están ocultos. */
    val textoDeLaComparacionOculta: String?,
    val opcionesDeModo: List<OpcionDeModo>,
)

/**
 * Qué período mira el usuario en Reportes y el reporte de ese período, siempre al día. En Android esto vivía en
 * ReportesViewModel; aquí lo comparten Android e iPhone.
 */
class SeguimientoDeReportes(
    private val base: BaseMiGasto,
    private val reloj: () -> Instant,
    private val zona: TimeZone,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(base: BaseMiGasto) : this(base, { Clock.System.now() }, TimeZone.currentSystemDefault())

    private val seleccion = MutableStateFlow(SeleccionDeReporte.delMesDe(reloj().toLocalDateTime(zona).date))

    val estado: Flow<PantallaDeReportes> = combine(base.movimientos.observarTodos(), seleccion) { todos, s ->
        val ahora = reloj()
        val reporte = calcularReporte(todos, s, base.categoriasPropias.todas(), ahora, zona)
        PantallaDeReportes(
            reporte = reporte,
            seleccion = s,
            modo = s.modo,
            puedeAvanzar = s.puedeAvanzar(ahora.toLocalDateTime(zona).date),
            puedeRetroceder = s.modo != ModoDeReporte.Rango,
            desde = s.desde.toString(),
            hasta = s.hasta.toString(),
            textoDeLaComparacion = reporte.comparacion?.texto(),
            textoDeLaComparacionOculta = reporte.comparacion?.texto("S/ ••••"),
            opcionesDeModo = listOf(
                OpcionDeModo(ModoDeReporte.Mes, "Mes"),
                OpcionDeModo(ModoDeReporte.Anio, "Año"),
                OpcionDeModo(ModoDeReporte.Rango, "Rango"),
            ),
        )
    }

    fun elegirModo(modo: ModoDeReporte) = seleccion.update { it.conModo(modo) }

    /** Pasa al mes o año anterior (`-1`) o siguiente (`1`). */
    fun desplazar(delta: Int) = seleccion.update { it.desplazar(delta) }

    fun elegirRango(desde: LocalDate, hasta: LocalDate) = seleccion.update { it.conRango(desde, hasta) }

    /** Como [elegirRango] con fechas `yyyy-MM-dd` (Swift no ve las fechas de Kotlin); si alguna no es fecha, no cambia nada. */
    fun elegirRango(desde: String, hasta: String) {
        val d = runCatching { LocalDate.parse(desde) }.getOrNull() ?: return
        val h = runCatching { LocalDate.parse(hasta) }.getOrNull() ?: return
        elegirRango(d, h)
    }
}
