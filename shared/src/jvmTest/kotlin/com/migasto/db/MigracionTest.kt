package com.migasto.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.migasto.domain.Categoria
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Migración real de la base: se parte de la foto del esquema v1 (`schemas/1.db`, la que se publicó),
 * se le guardan datos y se lleva a la versión actual. Los datos tienen que seguir ahí y lo nuevo tiene
 * que funcionar. Corre en la JVM porque necesita leer ese archivo; la sentencia es SQL normal de SQLite.
 */
class MigracionTest {
    private fun copiaDeLaVersion1(): File {
        val destino = Files.createTempFile("migasto_v1_", ".db").toFile()
        File("schemas/1.db").copyTo(destino, overwrite = true)
        return destino
    }

    @Test
    fun laBaseDeLaVersion1PasaALaActualSinPerderDatos() {
        val archivo = copiaDeLaVersion1()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${archivo.path}")

        driver.execute(
            null,
            "INSERT INTO movimientos (uuid, amount, merchant, category_name, source_name, date) " +
                "VALUES ('v1', 12.5, 'Tambo', 'compras', 'yape', 1790000000000)",
            0,
        )
        driver.execute(null, "INSERT INTO categorias_propias VALUES ('c1', 'Gym', 'gimnasio', 'gasto')", 0)

        MiGastoDb.Schema.migrate(driver, oldVersion = 1, newVersion = MiGastoDb.Schema.version)

        val base = BaseMiGasto(driver)
        // Lo de antes sigue ahí.
        val m = base.movimientos.todos().single()
        assertEquals("Tambo", m.merchant)
        assertEquals(12.5, m.amount)
        assertEquals(Categoria.Compras, m.category)
        assertEquals("Gym", base.categoriasPropias.todas().single().nombre)
        // Lo nuevo funciona.
        assertNull(base.ajustes.categoriaAprendida("Tambo"))
        base.ajustes.guardarPresupuesto(1500.0)
        assertEquals(1500.0, base.ajustes.presupuesto())
        base.close()
    }

    @Test
    fun laVersionActualEsLaDosYTieneUnaFotoPublicada() {
        assertEquals(2L, MiGastoDb.Schema.version)
        assertEquals(true, File("schemas/1.db").exists())
        assertEquals(true, File("schemas/2.db").exists())
    }
}
