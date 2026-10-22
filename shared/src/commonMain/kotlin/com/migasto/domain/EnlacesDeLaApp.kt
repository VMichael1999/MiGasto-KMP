package com.migasto.domain

// Los enlaces `migasto://` que abren la app desde fuera: el widget de la pantalla de inicio y el control del Centro de
// control (iPhone). Es el mismo esquema que usaba la app de Flutter.
object EnlacesDeLaApp {
    const val NUEVO_MOVIMIENTO = "migasto://new"

    /** `true` si [enlace] pide abrir «Nuevo movimiento» (se ignoran mayúsculas, la barra final y los parámetros). */
    fun esNuevoMovimiento(enlace: String): Boolean {
        val base = enlace.substringBefore('?').substringBefore('#').trimEnd('/')
        return base.equals(NUEVO_MOVIMIENTO, ignoreCase = true)
    }
}
