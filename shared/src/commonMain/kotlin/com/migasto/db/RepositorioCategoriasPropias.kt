package com.migasto.db

import com.migasto.domain.CategoriaPropia
import com.migasto.domain.CategoriasPropias
import com.migasto.domain.TipoMovimiento

/** Guarda las categorías propias del usuario. La lógica de crear y mezclar es de [CategoriasPropias]. */
class RepositorioCategoriasPropias internal constructor(private val db: MiGastoDb) {
    private val consultas get() = db.categoriasPropiasQueries

    fun todas(): List<CategoriaPropia> = consultas.todas().executeAsList().map {
        CategoriaPropia(
            id = it.id,
            nombre = it.nombre,
            icono = it.icono,
            tipo = if (it.tipo == TipoMovimiento.Ingreso.clave) TipoMovimiento.Ingreso else TipoMovimiento.Gasto,
        )
    }

    /** Crea una categoría. Si ya hay una con el mismo nombre para ese tipo, devuelve esa. */
    fun agregar(nombre: String, icono: String, tipo: TipoMovimiento): CategoriaPropia {
        val resultado = CategoriasPropias(todas()).agregar(nombre, icono, tipo)
        if (resultado.categoria !in todas()) guardar(resultado.categoria)
        return resultado.categoria
    }

    fun quitar(id: String) = consultas.eliminar(id)

    /** Agrega las de un respaldo que todavía no están (por id); no pisa las tuyas. */
    fun mezclar(otras: List<CategoriaPropia>) {
        val antes = todas()
        CategoriasPropias(antes).mezclar(otras).lista.filter { it !in antes }.forEach(::guardar)
    }

    private fun guardar(c: CategoriaPropia) =
        consultas.guardar(Categorias_propias(id = c.id, nombre = c.nombre, icono = c.icono, tipo = c.tipo.clave))
}
