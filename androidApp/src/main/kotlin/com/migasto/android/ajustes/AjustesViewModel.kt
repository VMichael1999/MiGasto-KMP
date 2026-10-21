package com.migasto.android.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.AliasGuardado
import com.migasto.domain.CategoriaAprendida
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.ContenidoDeRespaldo
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.RespaldoDeLaApp
import com.migasto.domain.ServicioDeRespaldo
import com.migasto.domain.aliasOrdenados
import com.migasto.domain.aprendidasOrdenadas
import com.migasto.domain.movimientosACsv
import com.migasto.domain.subtituloDelRespaldo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

data class AjustesUi(
    val presupuesto: Double,
    val ingresosAutomaticos: Boolean,
    val montoEnPantallaBloqueada: Boolean,
    val proveedores: Map<String, Boolean>,
    val subtituloDelRespaldo: String,
    val aprendidas: List<CategoriaAprendida>,
    val aliases: List<AliasGuardado>,
    val propias: List<CategoriaPropia>,
)

class AjustesViewModel(
    private val baseDiferida: Deferred<BaseMiGasto>,
    private val registroDiferido: Deferred<RegistroDeMovimientos>,
) : ViewModel() {
    private val refresco = MutableStateFlow(0)

    val estado: StateFlow<AjustesUi?> = flow {
        val base = baseDiferida.await()
        emitAll(
            refresco.map {
                AjustesUi(
                    presupuesto = base.ajustes.presupuesto(),
                    ingresosAutomaticos = base.ajustes.guardarIngresosAutomaticamente(),
                    montoEnPantallaBloqueada = base.ajustes.mostrarMontoEnPantallaBloqueada(),
                    proveedores = base.ajustes.proveedoresActivos(),
                    subtituloDelRespaldo = subtituloDelRespaldo(base.ajustes.ultimoRespaldo()),
                    aprendidas = aprendidasOrdenadas(base.ajustes.aprendidas()),
                    aliases = aliasOrdenados(base.ajustes.aliases()),
                    propias = base.categoriasPropias.todas(),
                )
            },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun cambiar(bloque: suspend (BaseMiGasto) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            bloque(baseDiferida.await())
            refresco.update { it + 1 }
        }
    }

    fun guardarPresupuesto(monto: Double) = cambiar { it.ajustes.guardarPresupuesto(monto) }

    fun alternarIngresosAutomaticos() = cambiar { it.ajustes.establecerGuardarIngresosAutomaticamente(!it.ajustes.guardarIngresosAutomaticamente()) }

    fun alternarMontoEnPantallaBloqueada() = cambiar { it.ajustes.establecerMostrarMontoEnPantallaBloqueada(!it.ajustes.mostrarMontoEnPantallaBloqueada()) }

    fun alternarProveedor(clave: String) = cambiar { it.ajustes.guardarProveedoresActivos(it.ajustes.proveedoresActivos() + (clave to !(it.ajustes.proveedoresActivos()[clave] ?: false))) }

    fun olvidarAprendida(comercio: String) = cambiar { it.ajustes.olvidarAprendida(comercio) }

    fun olvidarTodasLasAprendidas() = cambiar { it.ajustes.olvidarTodasLasAprendidas() }

    fun quitarAlias(clave: String) = cambiar { it.ajustes.establecerAlias(clave, "") }

    fun quitarPropia(id: String) = cambiar { it.categoriasPropias.quitar(id) }

    fun borrarUbicaciones(alTerminar: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { registroDiferido.await().quitarTodasLasUbicaciones() }
        alTerminar()
    }

    /** Todos los movimientos como CSV, sin ubicaciones. */
    suspend fun csv(): String = withContext(Dispatchers.IO) {
        val base = baseDiferida.await()
        movimientosACsv(base.movimientos.todos(), base.categoriasPropias.todas())
    }

    private suspend fun respaldoDeLaApp(): RespaldoDeLaApp {
        val b = baseDiferida.await()
        return RespaldoDeLaApp(b.movimientos, b.ajustes, b.categoriasPropias)
    }

    /** Cifra el respaldo. Puede tardar unos segundos: deriva la clave con 210 000 vueltas. */
    suspend fun crearRespaldo(contrasena: String): ByteArray = withContext(Dispatchers.Default) {
        ServicioDeRespaldo().cifrar(respaldoDeLaApp().contenido(), contrasena)
    }

    fun respaldoGuardado() = cambiar { it.ajustes.guardarUltimoRespaldo(Clock.System.now()) }

    suspend fun abrirRespaldo(datos: ByteArray, contrasena: String): ContenidoDeRespaldo =
        withContext(Dispatchers.Default) { ServicioDeRespaldo().descifrar(datos, contrasena) }

    suspend fun cuantosSonNuevos(contenido: ContenidoDeRespaldo): Int = withContext(Dispatchers.IO) { respaldoDeLaApp().cuantosSonNuevos(contenido) }

    suspend fun restaurar(contenido: ContenidoDeRespaldo): Int {
        val agregados = withContext(Dispatchers.IO) { respaldoDeLaApp().restaurar(contenido) }
        refresco.update { it + 1 }
        return agregados
    }
}
