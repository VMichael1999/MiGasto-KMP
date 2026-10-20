package com.migasto.domain

// Port de lib/domain/aliases.dart.
// Nombres que el usuario le pone a una persona o comercio ("Michael" en vez de
// "MICHAEL ANTHONY VALDIVIEZO MAZA"). El movimiento conserva el nombre original:
// el alias solo cambia cómo se ve.

private val espacios = Regex("\\s+")

/** Clave con la que se busca el alias: sin mayúsculas ni espacios de más. */
fun claveDeAlias(nombre: String): String =
    nombre.lowercase().trim().replace(espacios, " ")

/** El alias si lo hay; si no, el nombre original. */
fun nombreVisible(comercio: String, alias: Map<String, String>): String {
    val elegido = alias[claveDeAlias(comercio)]?.trim()
    return if (elegido.isNullOrEmpty()) comercio else elegido
}
