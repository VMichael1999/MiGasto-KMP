package com.migasto.seguridad

/**
 * Guarda secretos pequeños (como la clave de la base) en el almacén protegido del sistema: Keychain
 * en iPhone y Keystore en Android. Lo guardado no sale del teléfono.
 */
interface AlmacenSeguro {
    /** El valor guardado con [nombre], o `null` si no hay. */
    fun leer(nombre: String): String?

    /** Guarda [valor]; si ya había uno con ese [nombre], lo reemplaza. */
    fun guardar(nombre: String, valor: String)

    /** Borra el valor; no falla si no existía. */
    fun eliminar(nombre: String)
}
