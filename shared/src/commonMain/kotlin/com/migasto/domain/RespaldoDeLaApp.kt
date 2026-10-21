package com.migasto.domain

import com.migasto.db.RepositorioAjustes
import com.migasto.db.RepositorioCategoriasPropias
import com.migasto.db.RepositorioMovimientos
import kotlin.time.Clock
import kotlin.time.Instant

/** Arma el contenido de un respaldo desde la app y lo restaura (port de ExpensesNotifier). */
class RespaldoDeLaApp(
    private val movimientos: RepositorioMovimientos,
    private val ajustes: RepositorioAjustes,
    private val categoriasPropias: RepositorioCategoriasPropias,
    private val reloj: () -> Instant = { Clock.System.now() },
) {
    /** Todo lo que va en un respaldo: movimientos, presupuesto, categorías aprendidas, alias y categorías propias. */
    fun contenido(): ContenidoDeRespaldo = ContenidoDeRespaldo(
        movimientos = movimientos.todos(),
        presupuesto = ajustes.presupuesto(),
        aprendidas = ajustes.aprendidas(),
        alias = ajustes.aliases(),
        categoriasPropias = categoriasPropias.todas(),
        creado = reloj(),
    )

    /** Cuántos movimientos de [contenido] no están todavía en la app. */
    fun cuantosSonNuevos(contenido: ContenidoDeRespaldo): Int {
        val existentes = movimientos.todos().map { it.id }.toSet()
        return contenido.movimientos.count { it.id !in existentes }
    }

    /**
     * Restaura un respaldo sin borrar nada de lo que ya hay: agrega los movimientos que faltan (los que ya
     * están, por id, se dejan como están), recupera las categorías aprendidas, los alias y las categorías
     * propias que faltan, y el presupuesto. Devuelve cuántos movimientos agregó.
     */
    fun restaurar(contenido: ContenidoDeRespaldo): Int {
        val existentes = movimientos.todos().map { it.id }.toSet()
        val nuevos = contenido.movimientos.filter { it.id !in existentes }
        nuevos.forEach(movimientos::guardar)
        contenido.aprendidas.forEach { (comercio, nombre) ->
            Categoria.desdeClave(nombre)?.let { ajustes.guardarAprendida(comercio, it) }
        }
        ajustes.mezclarAliases(contenido.alias)
        categoriasPropias.mezclar(contenido.categoriasPropias)
        ajustes.guardarPresupuesto(contenido.presupuesto)
        return nuevos.size
    }
}
