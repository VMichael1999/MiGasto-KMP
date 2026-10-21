package com.migasto.db

import app.cash.sqldelight.db.QueryResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import com.migasto.domain.TipoMovimiento
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** SQLCipher de verdad: la base de iOS usa SQLCipher y el archivo en disco va cifrado. */
@OptIn(ExperimentalForeignApi::class)
class SqlCipherTest {
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
        assertTrue(!version.isNullOrBlank(), "cipher_version vacío: se está usando el SQLite de Apple, no SQLCipher")
    }

    @Test
    fun elArchivoNoEsLegible() {
        val ruta = fabrica.rutaDe(nombre)
        // Un dato cualquiera, para que la base llegue a escribirse en el archivo.
        BaseMiGasto.abrir(fabrica, nombre, "clave-correcta").use {
            it.categoriasPropias.agregar("Barbería", "spa", TipoMovimiento.Gasto)
        }
        val data = NSData.dataWithContentsOfFile(ruta)
        assertNotEquals(null, data, "no se creó el archivo en $ruta")
        val bytes = ByteArray(16)
        bytes.usePinned { memcpy(it.addressOf(0), data!!.bytes, 16u) }
        assertNotEquals("SQLite format 3", bytes.decodeToString(0, 15), "el archivo está en claro")
    }
}
