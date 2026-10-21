package com.migasto.android.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.android.resumen.FilaDeMovimiento
import com.migasto.android.resumen.crearFila
import com.migasto.db.BaseMiGasto
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.FiltrosDeMovimientos
import com.migasto.domain.Movimiento
import com.migasto.domain.agruparPorDia
import com.migasto.domain.etiquetaDelDia
import com.migasto.domain.filtrarMovimientos
import com.migasto.domain.formatearSoles
import kotlin.time.Clock
import kotlin.time.Instant
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class GrupoUi(
    val etiqueta: String,
    /** `S/ 144.70 en gastos`, o `null` si ese día no hubo gastos confirmados. */
    val totalDeGastos: String?,
    val filas: List<FilaDeMovimiento>,
)

data class MovimientosUi(
    val tipo: FiltroDeTipo,
    val busqueda: String,
    val cantidadPorConfirmar: Int,
    val grupos: List<GrupoUi>,
    val hayFiltro: Boolean,
    val oculto: Boolean,
)

class MovimientosViewModel(
    baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
    private val reloj: () -> Instant = { Clock.System.now() },
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val filtros = MutableStateFlow(FiltrosDeMovimientos())

    val estado: StateFlow<MovimientosUi?> = flow {
        val base = baseDiferida.await()
        emitAll(
            combine(base.movimientos.observarTodos(), filtros, oculto) { todos, f, ocultar -> construir(base, todos, f, ocultar) },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun elegirTipo(tipo: FiltroDeTipo) = filtros.update { it.copy(tipo = tipo) }

    fun buscar(texto: String) = filtros.update { it.copy(busqueda = texto) }

    fun limpiar() = filtros.update { FiltrosDeMovimientos() }

    private fun construir(base: BaseMiGasto, todos: List<Movimiento>, f: FiltrosDeMovimientos, ocultar: Boolean): MovimientosUi {
        val ahora = reloj()
        val alias = base.ajustes.aliases()
        val propias = base.categoriasPropias.todas()
        val hoy = ahora.toLocalDateTime(zona).date
        val grupos = agruparPorDia(filtrarMovimientos(todos, f, alias, propias), ahora, zona).map { g ->
            GrupoUi(
                etiqueta = etiquetaDelDia(g.dia, hoy),
                totalDeGastos = g.totalGastos.takeIf { it > 0 }?.let { "${formatearSoles(it)} en gastos" },
                filas = g.movimientos.map { crearFila(it, alias, propias, zona) },
            )
        }
        return MovimientosUi(
            tipo = f.tipo,
            busqueda = f.busqueda,
            cantidadPorConfirmar = todos.count { it.estado == EstadoMovimiento.Pendiente },
            grupos = grupos,
            hayFiltro = f.hayAlgunFiltro,
            oculto = ocultar,
        )
    }
}
