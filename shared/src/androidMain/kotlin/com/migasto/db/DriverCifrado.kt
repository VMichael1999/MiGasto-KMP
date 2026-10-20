package com.migasto.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Abre la base de MiGasto cifrada con SQLCipher. */
fun crearDriverCifrado(context: Context, nombre: String, clave: String): SqlDriver {
    System.loadLibrary("sqlcipher")
    return AndroidSqliteDriver(
        schema = MiGastoDb.Schema,
        context = context,
        name = nombre,
        factory = SupportOpenHelperFactory(clave.toByteArray()),
    )
}
