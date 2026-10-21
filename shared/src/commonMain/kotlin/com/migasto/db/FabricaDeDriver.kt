package com.migasto.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Abre la base cifrada de MiGasto. Cada plataforma tiene la suya (Android y iOS), con
 * SQLCipher; el resto del código solo ve este contrato.
 */
interface FabricaDeDriver {
    /** Abre (o crea) la base [nombre] cifrada con [clave]. Una clave incorrecta falla al primer uso. */
    fun abrir(nombre: String, clave: String): SqlDriver

    /** Ruta del archivo de la base [nombre] en el teléfono. */
    fun rutaDe(nombre: String): String

    /** Borra la base [nombre] y sus archivos auxiliares, si existen. */
    fun borrar(nombre: String)
}
