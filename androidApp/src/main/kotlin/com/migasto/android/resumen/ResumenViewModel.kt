package com.migasto.android.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.ClaveDePausa
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.calcularResumenDelMes
import com.migasto.domain.formatearMonto
import com.migasto.domain.formatearSoles
import com.migasto.domain.nombreDelMes
import com.migasto.domain.textoDelEstadoDelPresupuesto
import com.migasto.domain.textoDelPresupuestoUsado
import com.migasto.domain.textoPorConfirmar
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

    val estado: StateFlow<ResumenUi?> = flow {
        val base = baseDiferida.await()
        emitAll(combine(base.movimientos.observarTodos(), oculto, refresco) { movimientos, ocultar, _ -> construir(base, movimientos, ocultar) })
    }
        .flowOn(Dispatchers.Default) // la base se lee fuera del hilo principal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun alternarOculto() = oculto.update { !it }

    /** «Ahora no»: esconde ese aviso de configuración 7 días. */
    fun posponer(clave: ClaveDePausa) {
        viewModelScope.launch(Dispatchers.IO) {
            baseDiferida.await().ajustes.posponerAviso(clave, reloj())
            refresco.update { it + 1 }
        }
    }

    private fun construir(base: BaseMiGasto, movimientos: List<Movimiento>, ocultar: Boolean): ResumenUi {
        val ahora = reloj()
        val presupuesto = base.ajustes.presupuesto()
        val resumen = calcularResumenDelMes(movimientos, presupuesto, ahora, zona)
        val alias = base.ajustes.aliases()
        val propias = base.categoriasPropias.todas()

        val hoy = ahora.toLocalDateTime(zona).date
        val deHoy = movimientos
            .filter { it.date.toLocalDateTime(zona).date == hoy }
            .sortedByDescending { it.date }
            .map { crearFila(it, alias, propias, zona) }

        val pendientes = movimientos.filter { it.estado == EstadoMovimiento.Pendiente }
        return ResumenUi(
            mes = nombreDelMes(hoy.month.ordinal + 1),
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
            hoy = deHoy,
            oculto = ocultar,
            bateriaPospuestaEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Bateria),
            avisosPospuestosEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Avisos),
        )
    }
}
