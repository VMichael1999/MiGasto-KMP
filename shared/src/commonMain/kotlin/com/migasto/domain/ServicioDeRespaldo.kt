package com.migasto.domain

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.operations.IvAuthenticatedCipher
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.datetime.TimeZone

// Port de BackupService (lib/data/services/backup_service.dart).
//
// La contraseña la elige el usuario y no se guarda en ningún lado. La clave se deriva con
// PBKDF2-HMAC-SHA256 y el contenido se cifra con AES-256-GCM, que además detecta si el archivo
// fue alterado o la contraseña no coincide.
//
// Formato del archivo (todo en bytes), idéntico al de Flutter:
// "MGB1" (4) · iteraciones (4, big endian) · sal (16) · nonce (12) · texto cifrado · etiqueta (16)
// Los primeros 24 bytes (cabecera) también quedan autenticados.

class ServicioDeRespaldo(
    val iteraciones: Int = ITERACIONES_POR_DEFECTO,
    private val zona: TimeZone = TimeZone.currentSystemDefault(),
) {
    /**
     * Cifra [contenido] con [contrasena]. El nonce se elige aquí (12 bytes al azar, uno nuevo por
     * archivo) porque el formato de Flutter lo guarda en la cabecera: por eso `encryptWithIv`.
     */
    @OptIn(DelicateCryptographyApi::class)
    suspend fun cifrar(contenido: ContenidoDeRespaldo, contrasena: String): ByteArray {
        if (contrasena.length < LARGO_MINIMO_DE_CONTRASENA) {
            throw ExcepcionDeRespaldo("La contraseña debe tener al menos $LARGO_MINIMO_DE_CONTRASENA caracteres.")
        }
        val claro = contenido.aJson(zona).encodeToByteArray()
        val sal = CryptographyRandom.nextBytes(LARGO_DE_SAL)
        val nonce = CryptographyRandom.nextBytes(LARGO_DE_NONCE)
        val cabecera = MAGIA + enteroDe4Bytes(iteraciones) + sal

        val cifrador = cifrador(derivarClave(contrasena, sal, iteraciones))
        // El resultado ya trae la etiqueta de 16 bytes al final.
        val cifradoConEtiqueta = cifrador.encryptWithIv(nonce, claro, cabecera)
        return cabecera + nonce + cifradoConEtiqueta
    }

    /**
     * Descifra un respaldo. Lanza [ExcepcionDeRespaldo] si no es un respaldo de MiGasto, si la
     * contraseña no coincide o si el archivo está dañado.
     */
    @OptIn(DelicateCryptographyApi::class)
    suspend fun descifrar(datos: ByteArray, contrasena: String): ContenidoDeRespaldo {
        if (datos.size < LARGO_DE_CABECERA + LARGO_DE_NONCE + LARGO_DE_ETIQUETA || !empiezaConMagia(datos)) {
            throw ExcepcionDeRespaldo("Este archivo no es un respaldo de MiGasto.")
        }
        val vueltas = leerEntero(datos, 4)
        // Un valor absurdo en la cabecera podría bloquear el teléfono.
        if (vueltas < 10_000 || vueltas > 5_000_000) throw ExcepcionDeRespaldo("El archivo de respaldo está dañado.")

        val sal = datos.copyOfRange(8, LARGO_DE_CABECERA)
        val nonce = datos.copyOfRange(LARGO_DE_CABECERA, LARGO_DE_CABECERA + LARGO_DE_NONCE)
        val cifradoConEtiqueta = datos.copyOfRange(LARGO_DE_CABECERA + LARGO_DE_NONCE, datos.size)
        val cabecera = datos.copyOfRange(0, LARGO_DE_CABECERA)

        val claro = try {
            cifrador(derivarClave(contrasena, sal, vueltas.toInt())).decryptWithIv(nonce, cifradoConEtiqueta, cabecera)
        } catch (e: Exception) {
            throw ExcepcionDeRespaldo("La contraseña no coincide o el archivo está dañado.")
        }
        return contenidoDeRespaldoDesdeJson(claro.decodeToString(), zona)
    }

    private suspend fun derivarClave(contrasena: String, sal: ByteArray, vueltas: Int): ByteArray =
        CryptographyProvider.Default.get(PBKDF2)
            .secretDerivation(SHA256, vueltas, 256.bits, sal)
            .deriveSecret(contrasena.encodeToByteArray())
            .toByteArray()

    private suspend fun cifrador(clave: ByteArray): IvAuthenticatedCipher {
        val llave = CryptographyProvider.Default.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, clave)
        return llave.cipher(128.bits)
    }

    companion object {
        const val ITERACIONES_POR_DEFECTO = 210_000
        const val LARGO_MINIMO_DE_CONTRASENA = 8

        /** Extensión del archivo de respaldo. */
        const val EXTENSION = "mgb"

        private val MAGIA = byteArrayOf(0x4D, 0x47, 0x42, 0x31) // "MGB1"
        private const val LARGO_DE_SAL = 16
        private const val LARGO_DE_NONCE = 12
        private const val LARGO_DE_CABECERA = 4 + 4 + LARGO_DE_SAL
        private const val LARGO_DE_ETIQUETA = 16

        private fun empiezaConMagia(datos: ByteArray) = MAGIA.indices.all { datos[it] == MAGIA[it] }

        private fun enteroDe4Bytes(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

        private fun leerEntero(d: ByteArray, desde: Int): Long =
            (0..3).fold(0L) { acc, i -> (acc shl 8) or (d[desde + i].toLong() and 0xFF) }
    }
}
