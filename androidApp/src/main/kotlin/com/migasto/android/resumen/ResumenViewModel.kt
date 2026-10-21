package com.migasto.android.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.ClaveDePausa
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.MesElegido
import com.migasto.domain.Movimiento
import com.migasto.domain.calcularResumenDelMes
import com.migasto.domain.formatearMonto
import com.migasto.domain.formatearSoles
import com.migasto.domain.mesesParaElegir
import com.migasto.domain.nombreDelMesElegido
import com.migasto.domain.textoDelEstadoDelPresupuesto
import com.migasto.domain.textoDelPresupuestoUsado
import com.migasto.domain.textoDelSaldoDelResumen
import com.migasto.domain.textoPorConfirmar
import com.migasto.domain.textoSinMovimientosEnElResumen
import com.migasto.domain.tituloDeLaListaDelResumen
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Un mes del menú de la barra superior. */
data class OpcionDeMes(val mes: MesElegido, val nombre: String)

data class ResumenUi(
    val mes: String,
    val saldo: String,
    val ingresos: String,
    val gastos: String,
    /** `null` si no hay nada por confirmar. */
    val porConfirmarTexto: String?,
    val porConfirmarMonto: String,
    /** `null` si el usuario no definió presupuesto. */
    val presupuesto: PresupuestoUi?,
    val hoy: List<FilaDeMovimiento>,
    val oculto: Boolean,
    /** Cuándo tocó «Ahora no» en cada aviso de configuración (para esconderlo 7 días). */
    val bateriaPospuestaEn: Instant? = null,
    val avisosPospuestosEn: Instant? = null,
    val textoDelSaldo: String = "Te quedan de este mes",
    val tituloDeLaLista: String = "Hoy",
    val textoSinMovimientos: String = "Todavía no hay movimientos hoy.",
    /** Los meses del menú, del más nuevo al más viejo. */
    val meses: List<OpcionDeMes> = emptyList(),
    val mesElegido: MesElegido? = null,
)

data class PresupuestoUi(
    val estado: EstadoDelPresupuesto,
    val titulo: String,
    val detalle: String,
    /** Cuánto de la barra se llena, de 0 a 1. */
    val relleno: Float,
)

class ResumenViewModel(
    private val baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
    private val reloj: () -> Instant = { Clock.System.now() },
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    /** Se cambia para volver a leer lo que no se observa solo (la pausa de los avisos). */
    private val refresco = MutableStateFlow(0)

    /** El mes que se está mirando; `null` es el mes en curso. */
    private val mes = MutableStateFlow<MesElegido?>(null)

    val estado: StateFlow<ResumenUi?> = flow {
        val base = baseDiferida.await()
        emitAll(combine(base.movimientos.observarTodos(), oculto, refresco, mes) { movimientos, ocultar, _, elegido -> construir(base, movimientos, ocultar, elegido) })
    }
        .flowOn(Dispatchers.Default) // la base se lee fuera del hilo principal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun alternarOculto() = oculto.update { !it }

    fun elegirMes(elegido: MesElegido) {
        mes.value = elegido
    }

    /** «Ahora no»: esconde ese aviso de configuración 7 días. */
    fun posponer(clave: ClaveDePausa) {
        viewModelScope.launch(Dispatchers.IO) {
            baseDiferida.await().ajustes.posponerAviso(clave, reloj())
            refresco.update { it + 1 }
        }
    }

    private fun construir(base: BaseMiGasto, movimientos: List<Movimiento>, ocultar: Boolean, elegido: MesElegido?): ResumenUi {
        val ahora = reloj()
        val presupuesto = base.ajustes.presupuesto()
        val resumen = calcularResumenDelMes(movimientos, presupuesto, ahora, zona, elegido)
        val alias = base.ajustes.aliases()
        val propias = base.categoriasPropias.todas()

        val hoy = ahora.toLocalDateTime(zona).date
        val vistos = elegido ?: MesElegido(hoy.year, hoy.month.ordinal + 1)
        val esElActual = vistos == MesElegido(hoy.year, hoy.month.ordinal + 1)
        // En el mes en curso, lo de hoy; en un mes anterior, sus últimos movimientos.
        val deLaLista = movimientos
            .filter { m ->
                val f = m.date.toLocalDateTime(zona).date
                if (esElActual) f == hoy else f.year == vistos.anio && f.month.ordinal + 1 == vistos.mes
            }
            .sortedByDescending { it.date }
            .take(if (esElActual) Int.MAX_VALUE else 5)
            .map { crearFila(it, alias, propias, zona) }

        val pendientes = movimientos.filter { it.estado == EstadoMovimiento.Pendiente }
        return ResumenUi(
            mes = nombreDelMesElegido(vistos, ahora, zona),
            saldo = formatearMonto(resumen.saldo),
            ingresos = formatearSoles(resumen.ingresos),
            gastos = formatearSoles(resumen.gastos),
            porConfirmarTexto = pendientes.takeIf { it.isNotEmpty() }?.let { textoPorConfirmar(it) },
            porConfirmarMonto = "+ ${formatearSoles(resumen.porConfirmar)}",
            presupuesto = if (resumen.tienePresupuesto) {
                PresupuestoUi(
                    estado = resumen.estadoDelPresupuesto,
                    titulo = textoDelEstadoDelPresupuesto(resumen.estadoDelPresupuesto),
                    detalle = textoDelPresupuestoUsado(resumen),
                    relleno = (resumen.gastos / resumen.presupuesto).toFloat().coerceIn(0f, 1f),
                )
            } else {
                null
            },
            hoy = deLaLista,
            oculto = ocultar,
            bateriaPospuestaEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Bateria),
            avisosPospuestosEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Avisos),
            textoDelSaldo = textoDelSaldoDelResumen(vistos, ahora, zona),
            tituloDeLaLista = tituloDeLaListaDelResumen(vistos, ahora, zona),
            textoSinMovimientos = textoSinMovimientosEnElResumen(vistos, ahora, zona),
            meses = mesesParaElegir(movimientos, ahora, zona).map { OpcionDeMes(it, nombreDelMesElegido(it, ahora, zona)) },
            mesElegido = vistos,
        )
    }
}
