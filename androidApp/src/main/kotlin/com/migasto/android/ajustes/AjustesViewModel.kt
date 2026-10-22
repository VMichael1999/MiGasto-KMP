package com.migasto.android.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.AperturaDeRespaldo
import com.migasto.domain.ContenidoDeRespaldo
import com.migasto.domain.PantallaDeAjustes
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.ResultadoDelRespaldo
import com.migasto.domain.SeguimientoDeAjustes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lo de Ajustes que es igual en iPhone vive en [SeguimientoDeAjustes]; aquí solo queda avisar a la puerta de bloqueo, que es de Android. */
class AjustesViewModel(
    private val baseDiferida: Deferred<BaseMiGasto>,
    private val registroDiferido: Deferred<RegistroDeMovimientos>,
    private val bloqueoActivado: MutableStateFlow<Boolean?>,
) : ViewModel() {
    private suspend fun seguimiento() = SeguimientoDeAjustes(baseDiferida.await(), registroDiferido.await())

    private val seguimientoDiferido = viewModelScope.async(Dispatchers.Default) { seguimiento() }

    val estado: StateFlow<PantallaDeAjustes?> = flow {
        emitAll(seguimientoDiferido.await().estado)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun cambiar(bloque: (SeguimientoDeAjustes) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { bloque(seguimientoDiferido.await()) }
    }

    /** Guarda el presupuesto y devuelve el aviso. */
    suspend fun guardarPresupuesto(monto: Double): String = withContext(Dispatchers.IO) { seguimientoDiferido.await().guardarPresupuesto(monto) }

    fun alternarIngresosAutomaticos() = cambiar { it.alternarIngresosAutomaticos() }

    /** Activa o apaga el bloqueo; la puerta de bloqueo lo ve al instante. */
    fun establecerBloqueo(activado: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            seguimientoDiferido.await().establecerBloqueo(activado)
            bloqueoActivado.value = activado
        }
    }

    fun alternarMontoEnPantallaBloqueada() = cambiar { it.alternarMontoEnPantallaBloqueada() }

    fun alternarProveedor(clave: String) = cambiar { it.alternarProveedor(clave) }

    fun olvidarAprendida(comercio: String) = cambiar { it.olvidarAprendida(comercio) }

    fun olvidarTodasLasAprendidas() = cambiar { it.olvidarTodasLasAprendidas() }

    fun quitarAlias(clave: String) = cambiar { it.quitarAlias(clave) }

    fun quitarPropia(id: String) = cambiar { it.quitarPropia(id) }

    fun borrarUbicaciones(alTerminar: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { seguimientoDiferido.await().borrarUbicaciones() }
        alTerminar()
    }

    /** Todos los movimientos como CSV, sin ubicaciones. */
    suspend fun csv(): String = withContext(Dispatchers.IO) { seguimientoDiferido.await().csv() }

    /** Cifra el respaldo (en base64). Puede tardar unos segundos. */
    suspend fun crearRespaldo(contrasena: String): ResultadoDelRespaldo = seguimientoDiferido.await().crearRespaldo(contrasena)

    fun respaldoGuardado() = cambiar { it.respaldoGuardado() }

    suspend fun nombreDelArchivoDeRespaldo(): String = seguimientoDiferido.await().nombreDelArchivoDeRespaldo()

    suspend fun abrirRespaldo(datosEnBase64: String, contrasena: String): AperturaDeRespaldo =
        seguimientoDiferido.await().abrirRespaldo(datosEnBase64, contrasena)

    /** Agrega lo que falta y devuelve el aviso. */
    suspend fun restaurar(contenido: ContenidoDeRespaldo): String = withContext(Dispatchers.IO) { seguimientoDiferido.await().restaurar(contenido) }
}
