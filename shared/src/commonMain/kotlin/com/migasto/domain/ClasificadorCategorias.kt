package com.migasto.domain

import kotlinx.serialization.Serializable

// Port de lib/data/services/nlp_classifier_service.dart. Clasifica por palabras clave (no es
// IA). Las reglas están en category_rules.json, el mismo archivo de la app de Flutter. Se busca
// la palabra o frase completa: "Luz María" no es "Servicios" ni "Metropolitano" es "Compras".

@Serializable
internal class GrupoDeReglas(val category: String, val keywords: List<String>)

@Serializable
internal class ReglasDeCategorias(
    val gasto: List<GrupoDeReglas>,
    val ingreso: List<GrupoDeReglas>,
    val gastoDefault: String,
    val ingresoDefault: String,
)

class ClasificadorCategorias private constructor(reglas: ReglasDeCategorias) {
    private class Grupo(val categoria: Categoria, val patrones: List<Regex>)

    private val gastos = grupos(reglas.gasto)
    private val ingresos = grupos(reglas.ingreso)
    private val gastoPorDefecto = Categoria.desdeClave(reglas.gastoDefault) ?: Categoria.Otros
    private val ingresoPorDefecto = Categoria.desdeClave(reglas.ingresoDefault) ?: Categoria.TransferenciaRecibida

    private fun grupos(lista: List<GrupoDeReglas>): List<Grupo> = lista.map { g ->
        Grupo(
            categoria = Categoria.desdeClave(g.category) ?: Categoria.Otros,
            patrones = g.keywords.map {
                Regex("(?<![\\p{L}\\p{N}])${Regex.escape(it)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            },
        )
    }

    private fun buscar(grupos: List<Grupo>, texto: String): Categoria? =
        grupos.firstOrNull { g -> g.patrones.any { it.containsMatchIn(texto) } }?.categoria

    /** Categoría de un gasto. */
    fun clasificarGasto(texto: String, nombre: String): Categoria =
        buscar(gastos, "$texto $nombre") ?: gastoPorDefecto

    /** Categoría de un ingreso. */
    fun clasificarIngreso(texto: String, nombre: String): Categoria =
        buscar(ingresos, "$texto $nombre") ?: ingresoPorDefecto

    companion object {
        fun desdeJson(json: String): ClasificadorCategorias =
            ClasificadorCategorias(jsonDeReglas.decodeFromString<ReglasDeCategorias>(json))
    }
}
