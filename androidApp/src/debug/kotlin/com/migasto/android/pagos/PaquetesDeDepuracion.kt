package com.migasto.android.pagos

/**
 * Solo en depuración: también se leen las notificaciones del shell de adb, para probar la lectura de pagos con
 * `adb shell cmd notification post` sin tener Yape instalado. La versión de producción no lo incluye.
 */
internal object PaquetesDeDepuracion {
    val extra: Set<String> = setOf("com.android.shell")
}
