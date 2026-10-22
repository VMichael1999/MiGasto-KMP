package com.migasto.android.donde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.PantallaDeDondePagaste
import com.migasto.domain.PeriodoDeUbicaciones
import com.migasto.domain.SeguimientoDeDondePagaste
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** La pantalla ya armada por `shared` y si los montos están ocultos (eso es de la app, no de los datos). */
data class DondePagasteUi(val pantalla: PantallaDeDondePagaste, val oculto: Boolean)

class DondePagasteViewModel(
    baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
) : ViewModel() {
    private val seguimiento = viewModelScope.async(Dispatchers.Default) { SeguimientoDeDondePagaste(baseDiferida.await()) }

    val estado: StateFlow<DondePagasteUi?> = flow {
        emitAll(combine(seguimiento.await().estado, oculto) { pantalla, ocultar -> DondePagasteUi(pantalla, ocultar) })
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun elegirPeriodo(nuevo: PeriodoDeUbicaciones) {
        viewModelScope.async(Dispatchers.Default) { seguimiento.await().elegirPeriodo(nuevo) }
    }
}
