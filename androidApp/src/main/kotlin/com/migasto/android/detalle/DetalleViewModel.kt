package com.migasto.android.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.DetalleDelMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.UbicacionCapturada
import com.migasto.domain.construirDetalle
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
import kotlinx.coroutines.withContext

/** Qué muestra el detalle: cargando, el movimiento, o que ya no existe. */
sealed interface DetalleUi {
    data object Cargando : DetalleUi
    data object NoExiste : DetalleUi
    data class Listo(val detalle: DetalleDelMovimiento, val propias: List<CategoriaPropia>, val tipo: TipoMovimiento) : DetalleUi
}

class DetalleViewModel(
    private val id: String,
    private val baseDiferida: Deferred<BaseMiGasto>,
    private val registroDiferido: Deferred<RegistroDeMovimientos>,
    private val oculto: MutableStateFlow<Boolean>,
) : ViewModel() {
    /** Se cambia para volver a leer el nombre (alias) y las categorías, que no se observan solos. */
    private val refresco = MutableStateFlow(0)

    val estado: StateFlow<DetalleUi> = flow {
        val base = baseDiferida.await()
        emitAll(
            combine(base.movimientos.observarTodos(), refresco) { todos, _ ->
                val m = todos.firstOrNull { it.id == id } ?: return@combine DetalleUi.NoExiste
                val propias = base.categoriasPropias.todas()
                DetalleUi.Listo(construirDetalle(m, base.ajustes.aliases(), propias), propias, m.tipo)
            },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUi.Cargando)

    val montosOcultos: StateFlow<Boolean> = oculto

    private fun hacer(bloque: suspend (BaseMiGasto, RegistroDeMovimientos) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { bloque(baseDiferida.await(), registroDiferido.await()) }
    }

    fun confirmar(alTerminar: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { registroDiferido.await().confirmar(id) }
        alTerminar()
    }

    fun cambiarCategoria(categoria: Categoria, recordar: Boolean, propia: String? = null) =
        hacer { _, registro -> registro.cambiarCategoria(id, categoria, recordar, propia) }

    /** Crea una categoría propia y se la pone al movimiento. */
    fun crearCategoriaYElegirla(nombre: String, icono: String, tipo: TipoMovimiento, recordar: Boolean) =
        hacer { base, registro ->
            val creada = base.categoriasPropias.agregar(nombre.trim(), icono, tipo)
            registro.cambiarCategoria(id, creada.base, recordar, creada.id)
            refresco.update { it + 1 }
        }

    fun editar(monto: String, comercio: String, notas: String, alTerminar: (Boolean) -> Unit) = viewModelScope.launch {
        val bien = withContext(Dispatchers.IO) { registroDiferido.await().editar(id, monto, comercio, notas) }
        alTerminar(bien)
    }

    fun establecerAlias(comercio: String, alias: String) = hacer { base, _ ->
        base.ajustes.establecerAlias(comercio, alias)
        refresco.update { it + 1 }
    }

    fun ponerUbicacion(ubicacion: UbicacionCapturada) = hacer { _, registro -> registro.ponerUbicacion(id, ubicacion) }

    fun quitarUbicacion() = hacer { _, registro -> registro.quitarUbicacion(id) }

    /** Elimina el movimiento y entrega el original, por si el usuario toca «Deshacer». */
    fun eliminar(alTerminar: (Movimiento) -> Unit) = viewModelScope.launch {
        val original = withContext(Dispatchers.IO) {
            val m = baseDiferida.await().movimientos.todos().firstOrNull { it.id == id }
            if (m != null) registroDiferido.await().eliminar(id)
            m
        }
        if (original != null) alTerminar(original)
    }
}
