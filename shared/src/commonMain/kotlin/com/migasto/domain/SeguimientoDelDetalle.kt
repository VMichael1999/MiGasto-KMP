package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone

/** Qué muestra el detalle: el movimiento con sus textos y las categorías propias, o que ya no existe. */
sealed interface EstadoDelDetalle {
    data object NoExiste : EstadoDelDetalle

    class Listo(val detalle: DetalleDelMovimiento, val propias: List<CategoriaPropia>, val tipo: TipoMovimiento) : EstadoDelDetalle
}

/**
 * Observa un movimiento y hace lo que el detalle permite: confirmar, cambiar la categoría, editar, ponerle otro
 * nombre, ubicación y eliminar. En Android esto vivía en DetalleViewModel; aquí lo comparten Android e iPhone.
 */
class SeguimientoDelDetalle(
    private val base: BaseMiGasto,
    private val registro: RegistroDeMovimientos,
    private val id: String,
    private val zona: TimeZone,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(base: BaseMiGasto, registro: RegistroDeMovimientos, id: String) :
        this(base, registro, id, TimeZone.currentSystemDefault())

    /** Se cambia para volver a leer el alias y las categorías propias, que la base no avisa cuando cambian. */
    private val refresco = MutableStateFlow(0)

    val estado: Flow<EstadoDelDetalle> = combine(base.movimientos.observarTodos(), refresco) { todos, _ ->
        val m = todos.firstOrNull { it.id == id }
        if (m == null) {
            EstadoDelDetalle.NoExiste
        } else {
            val propias = base.categoriasPropias.todas()
            EstadoDelDetalle.Listo(construirDetalle(m, base.ajustes.aliases(), propias, zona), propias, m.tipo)
        }
    }

    fun confirmar() = registro.confirmar(id)

    fun cambiarCategoria(categoria: Categoria, recordar: Boolean, propia: String?) = registro.cambiarCategoria(id, categoria, recordar, propia)

    /** Crea una categoría propia (o toma la que ya tenía ese nombre) y se la pone al movimiento. */
    fun crearCategoriaYElegirla(nombre: String, icono: String, tipo: TipoMovimiento, recordar: Boolean) {
        val creada = base.categoriasPropias.agregar(nombre.trim(), icono, tipo)
        registro.cambiarCategoria(id, creada.base, recordar, creada.id)
        refresco.update { it + 1 }
    }

    /** `false` si el monto o el nombre no sirven: no se guarda nada. */
    fun editar(monto: String, comercio: String, notas: String): Boolean = registro.editar(id, monto, comercio, notas)

    /** Cómo ve el usuario a este comercio en toda la app; un alias vacío vuelve al nombre original. */
    fun establecerAlias(comercio: String, alias: String) {
        base.ajustes.establecerAlias(comercio, alias)
        refresco.update { it + 1 }
    }

    fun ponerUbicacion(ubicacion: UbicacionCapturada) = registro.ponerUbicacion(id, ubicacion)

    fun quitarUbicacion() = registro.quitarUbicacion(id)

    /** Elimina el movimiento y entrega el original, por si el usuario toca «Deshacer»; `null` si ya no existía. */
    fun eliminar(): Movimiento? {
        val original = base.movimientos.todos().firstOrNull { it.id == id } ?: return null
        registro.eliminar(id)
        return original
    }
}
