package com.migasto.domain

// Port de lib/shared/labels.dart (categoryLabel) y lib/shared/category_display.dart, sin los
// íconos: cada interfaz dibuja los suyos a partir de la clave de ícono.

/** Nombre visible de cada categoría. */
val Categoria.etiqueta: String
    get() = when (this) {
        Categoria.Alimentacion -> "Alimentación"
        Categoria.Transporte -> "Transporte"
        Categoria.Compras -> "Compras"
        Categoria.Servicios -> "Servicios"
        Categoria.Entretenimiento -> "Entretenimiento"
        Categoria.Otros, Categoria.OtrosIngresos -> "Otros"
        Categoria.Sueldo -> "Sueldo"
        Categoria.TransferenciaRecibida -> "Transferencia recibida"
        Categoria.Venta -> "Venta"
    }

/**
 * Si [propiaId] es una categoría propia que todavía existe, su nombre; si no (o si se
 * borró), el de la categoría de siempre.
 */
fun etiquetaDeCategoria(categoria: Categoria, propiaId: String?, propias: List<CategoriaPropia>): String =
    propias.firstOrNull { it.id == propiaId }?.nombre ?: categoria.etiqueta

/** Clave para agrupar y filtrar: la propia si existe, si no la de siempre. */
fun claveDeCategoria(movimiento: Movimiento, propias: List<CategoriaPropia>): String {
    val propia = movimiento.categoriaPropia
    return if (propia != null && propias.any { it.id == propia }) propia else movimiento.category.clave
}

/** Nombre visible de una clave de [claveDeCategoria]. */
fun etiquetaDeClave(clave: String, propias: List<CategoriaPropia>): String {
    propias.firstOrNull { it.id == clave }?.let { return it.nombre }
    return (Categoria.desdeClave(clave) ?: Categoria.Otros).etiqueta
}
