package com.migasto.db

import app.cash.sqldelight.db.QueryResult
import com.migasto.domain.TipoMovimiento
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** SQLCipher de verdad: la base de Android usa SQLCipher y el archivo en disco va cifrado. */
class SqlCipherAndroidTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-sqlcipher.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun sqlcipherEstaActivo() {
        val driver = fabrica.abrir(nombre, "clave-correcta")
        val version = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA cipher_version;",
            mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
            parameters = 0,
        ).value
        driver.close()
        assertTrue(!version.isNullOrBlank(), "cipher_version vacío: se está usando el SQLite de Android")
    }

    @Test
    fun elArchivoNoEsLegible() {
        // Un dato cualquiera, para que la base llegue a escribirse en el archivo.
        BaseMiGasto.abrir(fabrica, nombre, "clave-correcta").use {
            it.categoriasPropias.agregar("Barbería", "spa", TipoMovimiento.Gasto)
        }
        val archivo = File(fabrica.rutaDe(nombre))
        assertTrue(archivo.exists(), "no se creó el archivo ${archivo.path}")
        val cabecera = archivo.inputStream().use { it.readNBytes(15) }.decodeToString()
        assertNotEquals("SQLite format 3", cabecera, "el archivo está en claro")
    }
}
