package com.migasto.seguridad

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Almacén protegido por el Keychain de iPhone. Los valores quedan disponibles tras el primer
 * desbloqueo del teléfono (como en la app de Flutter) y no se copian a otros dispositivos.
 * [servicio] separa los valores de MiGasto de los de otras apps.
 */
@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos(private val servicio: String = "com.migasto") : AlmacenSeguro {

    override fun leer(nombre: String): String? = memScoped {
        val consulta = consultaBase(nombre)
        CFDictionaryAddValue(consulta, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(consulta, resultado.ptr)
        CFRelease(consulta)
        when (estado) {
            errSecSuccess -> {
                val datos = CFBridgingRelease(resultado.value) as NSData
                NSString.create(data = datos, encoding = NSUTF8StringEncoding) as String?
            }
            errSecItemNotFound -> null
            else -> error("Keychain: no se pudo leer $nombre (código $estado)")
        }
    }

    override fun guardar(nombre: String, valor: String) {
        eliminar(nombre)
        val atributos = consultaBase(nombre)
        CFDictionaryAddValue(atributos, kSecValueData, CFBridgingRetain(datosDe(valor)))
        CFDictionaryAddValue(atributos, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        val estado = SecItemAdd(atributos, null)
        CFRelease(atributos)
        check(estado == errSecSuccess) { "Keychain: no se pudo guardar $nombre (código $estado)" }
    }

    override fun eliminar(nombre: String) {
        val consulta = consultaBase(nombre)
        val estado = SecItemDelete(consulta)
        CFRelease(consulta)
        check(estado == errSecSuccess || estado == errSecItemNotFound) {
            "Keychain: no se pudo borrar $nombre (código $estado)"
        }
    }

    private fun consultaBase(nombre: String): CFMutableDictionaryRef {
        val consulta = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)!!
        CFDictionaryAddValue(consulta, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(consulta, kSecAttrService, CFBridgingRetain(servicio))
        CFDictionaryAddValue(consulta, kSecAttrAccount, CFBridgingRetain(nombre))
        return consulta
    }

    private fun datosDe(texto: String): NSData {
        val bytes = texto.encodeToByteArray()
        if (bytes.isEmpty()) return NSData()
        return bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
    }
}
