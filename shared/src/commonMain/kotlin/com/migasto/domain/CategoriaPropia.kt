package com.migasto.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Port de lib/domain/categoria_propia.dart y de CustomCategoriesNotifier (providers.dart).

/**
 * Categoría que crea el usuario, con el ícono que elija.
 *
 * Un movimiento con categoría propia guarda además su categoría de siempre (Otros / Otros
 * ingresos según el tipo), así los totales y las reglas siguen funcionando aunque se borre
 * la propia.
 */
data class CategoriaPropia(
    val id: String,
    val nombre: String,
    /** Clave del ícono que dibuja cada interfaz. */
    val icono: String,
    val tipo: TipoMovimiento,
) {
    /** La categoría de siempre que cuenta para los totales. */
    val base: Categoria get() = if (tipo == TipoMovimiento.Ingreso) Categoria.OtrosIngresos else Categoria.Otros

    fun aJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("nombre", nombre)
        put("icono", icono)
        put("tipo", tipo.clave)
    }

    companion object {
        /** `null` si el JSON no trae lo mínimo (se ignora esa categoría). */
        fun desdeJson(crudo: JsonElement?): CategoriaPropia? {
            val objeto = crudo as? JsonObject ?: return null
            val id = objeto.texto("id")
            val nombre = objeto.texto("nombre")
            if (id.isNullOrEmpty() || nombre == null || nombre.isBlank()) return null
            return CategoriaPropia(
                id = id,
                nombre = nombre.trim(),
                icono = objeto.texto("icono") ?: "",
                tipo = if (objeto.texto("tipo") == TipoMovimiento.Ingreso.clave) {
                    TipoMovimiento.Ingreso
                } else {
                    TipoMovimiento.Gasto
                },
            )
        }

        private fun JsonObject.texto(clave: String): String? =
            (this[clave] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}

/** Lee una lista guardada; lo que no se entiende se ignora. */
fun categoriasPropiasDesdeJson(crudo: JsonElement?): List<CategoriaPropia> =
    (crudo as? JsonArray)?.mapNotNull { CategoriaPropia.desdeJson(it) } ?: emptyList()

/** Las categorías propias del usuario y lo que se puede hacer con ellas. Es inmutable. */
class CategoriasPropias(val lista: List<CategoriaPropia> = emptyList()) {
    /** Resultado de [agregar]: la lista nueva y la categoría pedida (nueva o la que ya existía). */
    data class Agregada(val categorias: CategoriasPropias, val categoria: CategoriaPropia)

    /** Crea una categoría. Si ya hay una con el mismo nombre para ese tipo, devuelve esa. */
    @OptIn(ExperimentalUuidApi::class)
    fun agregar(
        nombre: String,
        icono: String,
        tipo: TipoMovimiento,
        nuevoId: () -> String = { Uuid.random().toString() },
    ): Agregada {
        val limpio = nombre.trim()
        val clave = limpio.lowercase()
        lista.firstOrNull { it.tipo == tipo && it.nombre.lowercase() == clave }
            ?.let { return Agregada(this, it) }
        val nueva = CategoriaPropia(id = nuevoId(), nombre = limpio, icono = icono, tipo = tipo)
        return Agregada(CategoriasPropias(lista + nueva), nueva)
    }

    /** Borra la categoría; los movimientos que la usaban se ven como Otros. */
    fun quitar(id: String): CategoriasPropias = CategoriasPropias(lista.filter { it.id != id })

    /** Agrega las de un respaldo que todavía no están (por id); no pisa las tuyas. */
    fun mezclar(otras: List<CategoriaPropia>): CategoriasPropias {
        val ids = lista.map { it.id }.toSet()
        val nuevas = otras.filter { it.id !in ids }
        return if (nuevas.isEmpty()) this else CategoriasPropias(lista + nuevas)
    }
}
