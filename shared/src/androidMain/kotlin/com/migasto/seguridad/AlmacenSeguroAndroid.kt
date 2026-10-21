package com.migasto.seguridad

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Almacén protegido por el Keystore de Android. Una llave AES-256 vive dentro del Keystore (no se
 * puede sacar del teléfono); con ella se cifra cada valor con AES-GCM y lo cifrado queda en
 * SharedPreferences privadas. El nombre del valor va como dato autenticado: un valor copiado a
 * otro nombre no se puede leer.
 */
class AlmacenSeguroAndroid(context: Context, private val alias: String = ALIAS_POR_DEFECTO) : AlmacenSeguro {
    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(nombre: String): String? {
        val guardado = preferencias.getString(nombre, null) ?: return null
        val (iv, cifrado) = guardado.split(SEPARADOR).map { Base64.decode(it, Base64.NO_WRAP) }
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, iv))
            updateAAD(nombre.toByteArray())
        }
        return cifrador.doFinal(cifrado).decodeToString()
    }

    override fun guardar(nombre: String, valor: String) {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.ENCRYPT_MODE, llave())
            updateAAD(nombre.toByteArray())
        }
        val cifrado = cifrador.doFinal(valor.toByteArray())
        val texto = Base64.encodeToString(cifrador.iv, Base64.NO_WRAP) + SEPARADOR +
            Base64.encodeToString(cifrado, Base64.NO_WRAP)
        // commit(): la clave de la base no puede perderse si la app se cierra justo después.
        check(preferencias.edit().putString(nombre, texto).commit()) { "No se pudo guardar $nombre" }
    }

    override fun eliminar(nombre: String) {
        preferencias.edit().remove(nombre).commit()
    }

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ALIAS_POR_DEFECTO = "migasto_llave_maestra"
        const val ARCHIVO = "migasto_almacen_seguro"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val SEPARADOR = ":"
    }
}
