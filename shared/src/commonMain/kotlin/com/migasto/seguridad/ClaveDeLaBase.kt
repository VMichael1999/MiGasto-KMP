package com.migasto.seguridad

import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * La clave con la que se cifra la base de datos. Se genera en el teléfono la primera vez (32 bytes
 * al azar, en hexadecimal) y vive en el [AlmacenSeguro]; no sale del teléfono. Port de
 * `_loadOrCreateKey` de la app de Flutter.
 */
class ClaveDeLaBase(
    private val almacen: AlmacenSeguro,
    private val nombre: String = NOMBRE,
) {
    fun obtenerOCrear(): String {
        val existente = almacen.leer(nombre)
        if (!existente.isNullOrEmpty()) return existente
        val clave = CryptographyRandom.nextBytes(LARGO_EN_BYTES).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        almacen.guardar(nombre, clave)
        return clave
    }

    /** Olvida la clave. La base cifrada con ella queda ilegible: solo para borrar todos los datos. */
    fun olvidar() = almacen.eliminar(nombre)

    companion object {
        /** El mismo nombre que usa la app de Flutter. */
        const val NOMBRE = "migasto_db_key"
        private const val LARGO_EN_BYTES = 32
    }
}
