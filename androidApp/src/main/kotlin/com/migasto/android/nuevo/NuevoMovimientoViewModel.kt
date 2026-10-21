package com.migasto.android.nuevo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migasto.db.BaseMiGasto
import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.FormularioNuevoMovimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.TipoMovimiento
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NuevoMovimientoViewModel(
    private val base: Deferred<BaseMiGasto>,
    private val registro: Deferred<RegistroDeMovimientos>,
) : ViewModel() {
    private val _formulario = MutableStateFlow(FormularioNuevoMovimiento())
    val formulario: StateFlow<FormularioNuevoMovimiento> = _formulario.asStateFlow()

    private val _propias = MutableStateFlow<List<CategoriaPropia>>(emptyList())
    val propias: StateFlow<List<CategoriaPropia>> = _propias.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { _propias.value = base.await().categoriasPropias.todas() }
    }

    fun presionar(tecla: Char) = _formulario.update { it.conEntrada(it.entrada.presionar(tecla)) }

    fun cambiarTipo(tipo: TipoMovimiento) = _formulario.update { it.conTipo(tipo) }

    fun escribirConcepto(texto: String) = _formulario.update { it.conConcepto(texto) }

    fun elegirFuente(fuente: PaymentSource) = _formulario.update { it.conFuente(fuente) }

    fun elegirCategoria(categoria: Categoria) = _formulario.update { it.conCategoria(categoria) }

    fun elegirPropia(propia: CategoriaPropia) = _formulario.update { it.conPropia(propia) }

    /** Crea una categoría propia (o devuelve la que ya tenía ese nombre) y la deja elegida. */
    fun crearCategoria(nombre: String, icono: String) {
        val tipo = _formulario.value.tipo
        viewModelScope.launch(Dispatchers.IO) {
            val b = base.await()
            val creada = b.categoriasPropias.agregar(nombre.trim(), icono, tipo)
            _propias.value = b.categoriasPropias.todas()
            _formulario.update { it.conPropia(creada) }
        }
    }

    /** Guarda el movimiento y avisa con el mensaje que corresponde («Gasto guardado» o «Ingreso guardado»). */
    fun guardar(alTerminar: (String) -> Unit) {
        val f = _formulario.value
        if (!f.puedeGuardar || _guardando.value) return
        _guardando.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                registro.await().agregarManual(
                    tipo = f.tipo, monto = f.monto, comercio = f.conceptoLimpio,
                    categoria = f.categoria, categoriaPropia = f.categoriaPropia, fuente = f.fuente,
                )
            }
            _guardando.value = false
            alTerminar(f.mensajeAlGuardar)
        }
    }
}
