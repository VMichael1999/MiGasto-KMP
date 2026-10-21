package com.migasto.android.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.ModoDeReporte
import com.migasto.domain.OpcionDeModo
import com.migasto.domain.Reporte
import com.migasto.domain.SeguimientoDeReportes
import com.migasto.domain.SeleccionDeReporte
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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ReporteUi(
    val reporte: Reporte,
    val seleccion: SeleccionDeReporte,
    val puedeAvanzar: Boolean,
    val oculto: Boolean,
    val textoDeLaComparacion: String? = null,
    val textoDeLaComparacionOculta: String? = null,
    val opcionesDeModo: List<OpcionDeModo> = emptyList(),
)

class ReportesViewModel(
    baseDiferida: Deferred<BaseMiGasto>,
    private val oculto: MutableStateFlow<Boolean>,
) : ViewModel() {
    /** El período y el reporte viven en `shared`; aquí solo se espera a que la base esté abierta. */
    private val seguimiento: Deferred<SeguimientoDeReportes> =
        viewModelScope.async(Dispatchers.IO) { SeguimientoDeReportes(baseDiferida.await()) }

    val estado: StateFlow<ReporteUi?> = flow {
        val s = seguimiento.await()
        emitAll(
            combine(s.estado, oculto) { p, ocultar ->
                ReporteUi(
                    reporte = p.reporte,
                    seleccion = p.seleccion,
                    puedeAvanzar = p.puedeAvanzar,
                    oculto = ocultar,
                    textoDeLaComparacion = p.textoDeLaComparacion,
                    textoDeLaComparacionOculta = p.textoDeLaComparacionOculta,
                    opcionesDeModo = p.opcionesDeModo,
                )
            },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun hacer(bloque: suspend (SeguimientoDeReportes) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { bloque(seguimiento.await()) }
    }

    fun elegirModo(modo: ModoDeReporte) = hacer { it.elegirModo(modo) }

    fun desplazar(delta: Int) = hacer { it.desplazar(delta) }

    fun elegirRango(desde: LocalDate, hasta: LocalDate) = hacer { it.elegirRango(desde, hasta) }
}
