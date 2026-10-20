package com.migasto.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class SqlCipherTest {
    private val dir = NSTemporaryDirectory().trimEnd('/')
    private val nombre = "prueba-sqlcipher.db"
    private val ruta = "$dir/$nombre"

    private fun abrir(clave: String): SqlDriver = NativeSqliteDriver(
        schema = MiGastoDb.Schema,
        name = nombre,
        onConfiguration = {
            it.copy(
                extendedConfig = it.extendedConfig.copy(basePath = dir),
                encryptionConfig = DatabaseConfiguration.Encryption(key = clave),
            )
        },
    )

    @AfterTest
    fun limpiar() {
        NSFileManager.defaultManager.removeItemAtPath(ruta, null)
    }

    @Test
    fun sqlcipherEstaActivo() {
        val driver = abrir("clave-correcta")
        val version = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA cipher_version;",
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
            },
            parameters = 0,
        ).value
        driver.close()
        assertTrue(!version.isNullOrBlank(), "cipher_version vacío: se está usando el SQLite de Apple, no SQLCipher")
    }

    @Test
    fun guardaYLeeConLaClaveCorrecta() {
        abrir("clave-correcta").also { MiGastoDb(it).gastoQueries.insertar(1250, "Menú") }.close()
        val driver = abrir("clave-correcta")
        val filas = MiGastoDb(driver).gastoQueries.todos().executeAsList()
        driver.close()
        assertEquals(1, filas.size)
        assertEquals(1250L, filas[0].monto_centimos)
        assertEquals("Menú", filas[0].comentario)
    }

    @Test
    fun elArchivoNoEsLegible() {
        abrir("clave-correcta").also { MiGastoDb(it).gastoQueries.insertar(1250, "Menú") }.close()
        val data = NSData.dataWithContentsOfFile(ruta)
        assertNotEquals(null, data, "no se creó el archivo en $ruta")
        val bytes = ByteArray(16)
        bytes.usePinned { memcpy(it.addressOf(0), data!!.bytes, 16u) }
        assertNotEquals("SQLite format 3", bytes.decodeToString(0, 15), "el archivo está en claro")
    }

    @Test
    fun unaClaveIncorrectaFalla() {
        abrir("clave-correcta").also { MiGastoDb(it).gastoQueries.insertar(1250, "Menú") }.close()
        assertFailsWith<Throwable> {
            val driver = abrir("clave-equivocada")
            try {
                MiGastoDb(driver).gastoQueries.todos().executeAsList()
            } finally {
                driver.close()
            }
        }
    }
}
