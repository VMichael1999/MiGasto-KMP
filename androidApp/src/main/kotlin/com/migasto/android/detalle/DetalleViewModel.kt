package com.migasto.android.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.DetalleDelMovimiento
import com.migasto.domain.EstadoDelDetalle
import com.migasto.domain.Movimiento
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.SeguimientoDelDetalle
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.UbicacionCapturada
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Qué muestra el detalle: cargando, el movimiento, o que ya no existe. */
sealed interface DetalleUi {
    data object Cargando : DetalleUi
    data object NoExiste : DetalleUi
    data class Listo(val detalle: DetalleDelMovimiento, val propias: List<CategoriaPropia>, val tipo: TipoMovimiento) : DetalleUi
}

class DetalleViewModel(
    private val id: String,
    baseDiferida: Deferred<BaseMiGasto>,
    registroDiferido: Deferred<RegistroDeMovimientos>,
    private val oculto: MutableStateFlow<Boolean>,
) : ViewModel() {
    /** Lo que observa y hace el detalle vive en `shared`; aquí solo se espera a que la base esté abierta. */
    private val seguimiento: Deferred<SeguimientoDelDetalle> =
        viewModelScope.async(Dispatchers.IO) { SeguimientoDelDetalle(baseDiferida.await(), registroDiferido.await(), id) }

    val estado: StateFlow<DetalleUi> = flow { emitAll(seguimiento.await().estado.map { it.aUi() }) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUi.Cargando)

    val montosOcultos: StateFlow<Boolean> = oculto

    private fun hacer(bloque: suspend (SeguimientoDelDetalle) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { bloque(seguimiento.await()) }
    }

    fun confirmar(alTerminar: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { seguimiento.await().confirmar() }
        alTerminar()
    }

    fun cambiarCategoria(categoria: Categoria, recordar: Boolean, propia: String? = null) =
        hacer { it.cambiarCategoria(categoria, recordar, propia) }

    /** Crea una categoría propia y se la pone al movimiento. */
    fun crearCategoriaYElegirla(nombre: String, icono: String, tipo: TipoMovimiento, recordar: Boolean) =
        hacer { it.crearCategoriaYElegirla(nombre, icono, tipo, recordar) }

    fun editar(monto: String, comercio: String, notas: String, alTerminar: (Boolean) -> Unit) = viewModelScope.launch {
        val bien = withContext(Dispatchers.IO) { seguimiento.await().editar(monto, comercio, notas) }
        alTerminar(bien)
    }

    fun establecerAlias(comercio: String, alias: String) = hacer { it.establecerAlias(comercio, alias) }

    fun ponerUbicacion(ubicacion: UbicacionCapturada) = hacer { it.ponerUbicacion(ubicacion) }

    fun quitarUbicacion() = hacer { it.quitarUbicacion() }

    /** Elimina el movimiento y entrega el original, por si el usuario toca «Deshacer». */
    fun eliminar(alTerminar: (Movimiento) -> Unit) = viewModelScope.launch {
        val original = withContext(Dispatchers.IO) { seguimiento.await().eliminar() }
        if (original != null) alTerminar(original)
    }
}

private fun EstadoDelDetalle.aUi(): DetalleUi = when (this) {
    EstadoDelDetalle.NoExiste -> DetalleUi.NoExiste
    is EstadoDelDetalle.Listo -> DetalleUi.Listo(detalle, propias, tipo)
}
