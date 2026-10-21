package com.migasto.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.migasto.domain.CanalMovimiento
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.categoriaAlmacenada
import com.migasto.domain.leerCategoriaAlmacenada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** Guarda y lee los movimientos. Reemplaza a `DatabaseManager` de la app de Flutter. */
class RepositorioMovimientos internal constructor(private val db: MiGastoDb) {
    private val consultas get() = db.movimientosQueries

    /** Del más reciente al más antiguo. */
    fun todos(): List<Movimiento> = consultas.todos().executeAsList().map { it.aDominio() }

    /** Emite la lista completa cada vez que cambia. */
    fun observarTodos(): Flow<List<Movimiento>> =
        consultas.todos().asFlow().mapToList(Dispatchers.Default).map { filas -> filas.map { it.aDominio() } }

    /** Crea el movimiento o, si el id ya existe, lo reemplaza. */
    fun guardar(m: Movimiento) {
        consultas.guardar(
            Movimientos(
                uuid = m.id,
                amount = m.amount,
                merchant = m.merchant,
                category_name = categoriaAlmacenada(m.category, m.categoriaPropia),
                source_name = m.source.clave,
                date = m.date.toEpochMilliseconds(),
                notes = m.notes,
                tipo_name = m.tipo.clave,
                estado_name = m.estado.clave,
                canal_name = m.canal.clave,
                tarjeta = m.tarjeta,
                latitud = m.latitud,
                longitud = m.longitud,
                precision = m.precision,
                lugar = m.lugar,
                texto_original = m.textoOriginal,
            ),
        )
    }

    fun eliminar(id: String) = consultas.eliminar(id)

    fun cantidad(): Long = consultas.contar().executeAsOne()
}

/** Lee una fila. Un valor desconocido (de una versión distinta) cae en un valor seguro. */
private fun Movimientos.aDominio(): Movimiento {
    val fuente = PaymentSource.desdeClave(source_name) ?: PaymentSource.Manual
    val categoria = leerCategoriaAlmacenada(category_name, Categoria.Otros)
    return Movimiento(
        id = uuid,
        amount = amount,
        merchant = merchant,
        category = categoria.categoria,
        categoriaPropia = categoria.propia,
        source = fuente,
        date = Instant.fromEpochMilliseconds(date),
        notes = notes,
        tipo = TipoMovimiento.entries.firstOrNull { it.clave == tipo_name } ?: TipoMovimiento.Gasto,
        canal = CanalMovimiento.entries.firstOrNull { it.clave == canal_name } ?: CanalMovimiento.Manual,
        estado = EstadoMovimiento.entries.firstOrNull { it.clave == estado_name } ?: EstadoMovimiento.Pendiente,
        tarjeta = tarjeta,
        latitud = latitud,
        longitud = longitud,
        precision = precision,
        lugar = lugar,
        textoOriginal = texto_original,
    )
}
