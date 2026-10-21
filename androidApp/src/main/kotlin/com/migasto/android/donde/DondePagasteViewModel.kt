package com.migasto.android.donde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.PeriodoDeUbicaciones
import com.migasto.domain.ZonaDePagos
import com.migasto.domain.agruparPorZona
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
import kotlin.time.Clock
import kotlin.time.Instant

data class DondePagasteUi(
    val periodo: PeriodoDeUbicaciones,
    val zonas: List<ZonaDePagos>,
    val alias: Map<String, String>,
    val oculto: Boolean,
)

class DondePagasteViewModel(
    private val baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
    private val reloj: () -> Instant = { Clock.System.now() },
) : ViewModel() {
    private val periodo = MutableStateFlow(PeriodoDeUbicaciones.Semana)

    val estado: StateFlow<DondePagasteUi?> = flow {
        val base = baseDiferida.await()
        emitAll(
            combine(base.movimientos.observarTodos(), periodo, oculto) { movimientos, elegido, ocultar ->
                DondePagasteUi(elegido, agruparPorZona(movimientos, elegido, reloj()), base.ajustes.aliases(), ocultar)
            },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun elegirPeriodo(nuevo: PeriodoDeUbicaciones) = periodo.update { nuevo }
}
