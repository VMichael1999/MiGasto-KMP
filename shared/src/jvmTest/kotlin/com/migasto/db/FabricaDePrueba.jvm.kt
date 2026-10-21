package com.migasto.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.nio.file.Files

/**
 * SQLite normal sobre la JVM, sin cifrar: sirve para probar la lógica de los repositorios en el
 * computador y medir la cobertura. El cifrado se prueba en Android e iOS.
 */
class FabricaDeDriverJvm : FabricaDeDriver {
    private val carpeta = Files.createTempDirectory("migasto_db_").toFile()

    override fun abrir(nombre: String, clave: String): SqlDriver {
        val archivo = File(rutaDe(nombre))
        val nueva = !archivo.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${archivo.path}")
        if (nueva) MiGastoDb.Schema.create(driver)
        return driver
    }

    override fun rutaDe(nombre: String): String = File(carpeta, nombre).path

    override fun borrar(nombre: String) {
        File(rutaDe(nombre)).delete()
    }
}

actual fun fabricaDePrueba(): FabricaDeDriver = FabricaDeDriverJvm()
