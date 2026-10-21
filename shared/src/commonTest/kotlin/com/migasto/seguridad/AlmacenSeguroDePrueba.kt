package com.migasto.seguridad

/** El almacén real de la plataforma (Keychain en iOS, Keystore en Android) para los tests. */
expect fun almacenSeguroReal(): AlmacenSeguro

/** Un almacén en memoria, para probar la lógica sin tocar el sistema. */
class AlmacenEnMemoria : AlmacenSeguro {
    private val datos = mutableMapOf<String, String>()
    override fun leer(nombre: String): String? = datos[nombre]
    override fun guardar(nombre: String, valor: String) { datos[nombre] = valor }
    override fun eliminar(nombre: String) { datos.remove(nombre) }
}
