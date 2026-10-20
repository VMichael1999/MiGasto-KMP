package com.migasto.db

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SqlCipherAndroidTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "prueba-sqlcipher.db"

    private fun abrir(clave: String): SqlDriver = crearDriverCifrado(context, nombre, clave)

    @AfterTest
    fun limpiar() {
        context.deleteDatabase(nombre)
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
        assertTrue(!version.isNullOrBlank(), "cipher_version vacío: se está usando el SQLite de Android")
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
        val archivo: File = context.getDatabasePath(nombre)
        assertTrue(archivo.exists(), "no se creó el archivo ${archivo.path}")
        val cabecera = archivo.inputStream().use { it.readNBytes(15) }.decodeToString()
        assertNotEquals("SQLite format 3", cabecera, "el archivo está en claro")
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
