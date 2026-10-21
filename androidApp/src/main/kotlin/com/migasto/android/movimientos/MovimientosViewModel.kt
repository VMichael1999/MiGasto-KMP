package com.migasto.android.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.FiltrosDeMovimientos
import com.migasto.domain.GrupoDeMovimientos
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.armarPantallaDeMovimientos
import com.migasto.domain.conLaHoja
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
import kotlin.time.Clock
import kotlin.time.Instant

data class MovimientosUi(
    val tipo: FiltroDeTipo,
    val busqueda: String,
    val cantidadPorConfirmar: Int,
    val grupos: List<GrupoDeMovimientos>,
    val hayFiltro: Boolean,
    val oculto: Boolean,
    /** Todos los filtros puestos: la hoja se abre con lo que ya está elegido. */
    val filtros: FiltrosDeMovimientos = FiltrosDeMovimientos(),
    val propias: List<CategoriaPropia> = emptyList(),
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

    /** Aplica lo elegido en la hoja «Filtrar movimientos». */
    fun aplicarFiltros(categoria: String?, fuente: PaymentSource?, rango: ClosedFloatingPointRange<Double>?) =
        filtros.update { it.conLaHoja(categoria, fuente, rango) }

    fun limpiarFiltrosDeLaHoja() = filtros.update { it.sinLoDeLaHoja() }

    private fun construir(base: BaseMiGasto, todos: List<Movimiento>, f: FiltrosDeMovimientos, ocultar: Boolean): MovimientosUi {
        val p = armarPantallaDeMovimientos(todos, f, base.ajustes.aliases(), base.categoriasPropias.todas(), reloj(), zona)
        return MovimientosUi(
            tipo = p.tipo,
            busqueda = p.busqueda,
            cantidadPorConfirmar = p.cantidadPorConfirmar,
            grupos = p.grupos,
            hayFiltro = p.hayFiltro,
            oculto = ocultar,
            filtros = p.filtros,
            propias = p.propias,
        )
    }
}
