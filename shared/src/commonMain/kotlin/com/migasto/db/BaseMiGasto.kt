package com.migasto.db

import app.cash.sqldelight.db.SqlDriver

/** La base abierta y los repositorios que la usan. Hay que cerrarla con [close]. */
class BaseMiGasto(private val driver: SqlDriver) : AutoCloseable {
    private val db = MiGastoDb(driver)

    val movimientos = RepositorioMovimientos(db)
    val categoriasPropias = RepositorioCategoriasPropias(db)
    val ajustes = RepositorioAjustes(db)

    override fun close() = driver.close()

    /** Solo para tests: ejecuta una sentencia SQL suelta. */
    internal fun ejecutarSql(sql: String) {
        driver.execute(null, sql, 0)
    }

    companion object {
        fun abrir(fabrica: FabricaDeDriver, nombre: String, clave: String): BaseMiGasto =
            BaseMiGasto(fabrica.abrir(nombre, clave))
    }
}
