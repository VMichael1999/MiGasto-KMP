package com.migasto.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Base cifrada con SQLCipher para iOS. Por defecto vive en Application Support/MiGasto;
 * [directorio] permite otra carpeta (los tests usan la temporal).
 */
@OptIn(ExperimentalForeignApi::class)
class FabricaDeDriverIos(directorio: String? = null) : FabricaDeDriver {
    private val directorio: String = directorio?.trimEnd('/') ?: carpetaPorDefecto()

    override fun abrir(nombre: String, clave: String): SqlDriver {
        NSFileManager.defaultManager.createDirectoryAtPath(directorio, true, null, null)
        return NativeSqliteDriver(
            schema = MiGastoDb.Schema,
            name = nombre,
            onConfiguration = {
                it.copy(
                    extendedConfig = it.extendedConfig.copy(basePath = directorio),
                    encryptionConfig = DatabaseConfiguration.Encryption(key = clave),
                )
            },
        )
    }

    override fun rutaDe(nombre: String): String = "$directorio/$nombre"

    override fun borrar(nombre: String) {
        for (sufijo in listOf("", "-wal", "-shm", "-journal")) {
            NSFileManager.defaultManager.removeItemAtPath(rutaDe(nombre) + sufijo, null)
        }
    }

    private fun carpetaPorDefecto(): String {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: error("No hay carpeta Application Support")
        return "$base/MiGasto"
    }
}
