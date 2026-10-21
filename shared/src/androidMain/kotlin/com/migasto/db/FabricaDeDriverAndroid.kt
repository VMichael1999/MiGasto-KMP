package com.migasto.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Base cifrada con SQLCipher para Android. */
class FabricaDeDriverAndroid(private val context: Context) : FabricaDeDriver {
    override fun abrir(nombre: String, clave: String): SqlDriver {
        System.loadLibrary("sqlcipher")
        return AndroidSqliteDriver(
            schema = MiGastoDb.Schema,
            context = context,
            name = nombre,
            factory = SupportOpenHelperFactory(clave.toByteArray()),
        )
    }

    override fun rutaDe(nombre: String): String = context.getDatabasePath(nombre).path

    override fun borrar(nombre: String) {
        context.deleteDatabase(nombre)
    }
}
