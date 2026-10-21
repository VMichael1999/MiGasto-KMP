package com.migasto.android.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.ModoDeReporte
import com.migasto.domain.Reporte
import com.migasto.domain.SeleccionDeReporte
import com.migasto.domain.calcularReporte
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class ReporteUi(
    val reporte: Reporte,
    val seleccion: SeleccionDeReporte,
    val puedeAvanzar: Boolean,
    val oculto: Boolean,
)

class ReportesViewModel(
    baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
    private val reloj: () -> Instant = { Clock.System.now() },
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val seleccion = MutableStateFlow(SeleccionDeReporte.delMesDe(reloj().toLocalDateTime(zona).date))

    val estado: StateFlow<ReporteUi?> = flow {
        val base = baseDiferida.await()
        emitAll(
            combine(base.movimientos.observarTodos(), seleccion, oculto) { todos, s, ocultar ->
                val ahora = reloj()
                ReporteUi(
                    reporte = calcularReporte(todos, s, base.categoriasPropias.todas(), ahora, zona),
                    seleccion = s,
                    puedeAvanzar = s.puedeAvanzar(ahora.toLocalDateTime(zona).date),
                    oculto = ocultar,
                )
            },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun elegirModo(modo: ModoDeReporte) = seleccion.update { it.conModo(modo) }

    fun desplazar(delta: Int) = seleccion.update { it.desplazar(delta) }

    fun elegirRango(desde: LocalDate, hasta: LocalDate) = seleccion.update { it.conRango(desde, hasta) }
}
