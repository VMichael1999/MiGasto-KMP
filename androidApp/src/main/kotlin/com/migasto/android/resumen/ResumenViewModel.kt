package com.migasto.android.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.ClaveDePausa
import com.migasto.domain.FilaDeMovimiento
import com.migasto.domain.MesElegido
import com.migasto.domain.Movimiento
import com.migasto.domain.OpcionDeMes
import com.migasto.domain.PresupuestoDelResumen
import com.migasto.domain.armarPantallaDelResumen
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
    val presupuesto: PresupuestoDelResumen?,
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
        val p = armarPantallaDelResumen(
            movimientos, base.ajustes.presupuesto(), base.ajustes.aliases(), base.categoriasPropias.todas(), reloj(), zona, elegido,
        )
        return ResumenUi(
            mes = p.mes,
            saldo = p.saldo,
            ingresos = p.ingresos,
            gastos = p.gastos,
            porConfirmarTexto = p.porConfirmarTexto,
            porConfirmarMonto = p.porConfirmarMonto,
            presupuesto = p.presupuesto,
            hoy = p.hoy,
            oculto = ocultar,
            bateriaPospuestaEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Bateria),
            avisosPospuestosEn = base.ajustes.avisoPospuestoEn(ClaveDePausa.Avisos),
            textoDelSaldo = p.textoDelSaldo,
            tituloDeLaLista = p.tituloDeLaLista,
            textoSinMovimientos = p.textoSinMovimientos,
            meses = p.meses,
            mesElegido = p.mesElegido,
        )
    }
}
