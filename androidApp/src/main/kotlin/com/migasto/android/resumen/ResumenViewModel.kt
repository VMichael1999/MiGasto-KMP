package com.migasto.android.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.calcularResumenDelMes
import com.migasto.domain.etiqueta
import com.migasto.domain.formatearHora
import com.migasto.domain.formatearMonto
import com.migasto.domain.formatearSoles
import com.migasto.domain.formatearSolesConSigno
import com.migasto.domain.nombreDelMes
import com.migasto.domain.nombreVisible
import com.migasto.domain.textoDelEstadoDelPresupuesto
import com.migasto.domain.textoDelPresupuestoUsado
import com.migasto.domain.textoPorConfirmar
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Una fila de «Hoy». Todo el texto ya viene armado desde `shared`: la vista no calcula nada. */
data class FilaDeMovimiento(
    val id: String,
    val nombre: String,
    val fuente: PaymentSource,
    val fuenteYHora: String,
    val monto: String,
    val esIngreso: Boolean,
    val porConfirmar: Boolean,
    val categoria: Categoria,
    /** Clave del ícono si la categoría es propia del usuario. */
    val iconoPropio: String?,
)

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
)

data class PresupuestoUi(
    val estado: EstadoDelPresupuesto,
    val titulo: String,
    val detalle: String,
    /** Cuánto de la barra se llena, de 0 a 1. */
    val relleno: Float,
)

class ResumenViewModel(
    private val base: BaseMiGasto,
    private val reloj: () -> Instant = { Clock.System.now() },
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val oculto = MutableStateFlow(false)

    val estado: StateFlow<ResumenUi?> = combine(base.movimientos.observarTodos(), oculto) { movimientos, ocultar ->
        construir(movimientos, ocultar)
    }
        .flowOn(Dispatchers.Default) // la base se lee fuera del hilo principal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun alternarOculto() = oculto.update { !it }

    private fun construir(movimientos: List<Movimiento>, ocultar: Boolean): ResumenUi {
        val ahora = reloj()
        val presupuesto = base.ajustes.presupuesto()
        val resumen = calcularResumenDelMes(movimientos, presupuesto, ahora, zona)
        val alias = base.ajustes.aliases()
        val propias = base.categoriasPropias.todas()

        val hoy = ahora.toLocalDateTime(zona).date
        val deHoy = movimientos
            .filter { it.date.toLocalDateTime(zona).date == hoy }
            .sortedByDescending { it.date }
            .map { m ->
                FilaDeMovimiento(
                    id = m.id,
                    nombre = nombreVisible(m.merchant, alias),
                    fuente = m.source,
                    fuenteYHora = "${m.source.etiqueta} · ${formatearHora(m.date, zona)}",
                    monto = formatearSolesConSigno(m.amount, m.esIngreso),
                    esIngreso = m.esIngreso,
                    porConfirmar = m.estado == EstadoMovimiento.Pendiente,
                    categoria = m.category,
                    iconoPropio = propias.firstOrNull { it.id == m.categoriaPropia }?.icono,
                )
            }

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
        )
    }
}
