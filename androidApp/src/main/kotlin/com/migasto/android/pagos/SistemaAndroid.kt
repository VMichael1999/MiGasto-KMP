package com.migasto.android.pagos

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Qué permisos del sistema están dados ahora (se vuelve a leer al volver de los ajustes del sistema). */
data class EstadoDelSistema(
    val lectura: Boolean,
    val avisos: Boolean,
    val bateria: Boolean,
    val accesibilidad: Boolean,
    val ventana: Boolean,
)

fun leerSistema(contexto: Context): EstadoDelSistema = EstadoDelSistema(
    lectura = NotificationManagerCompat.getEnabledListenerPackages(contexto).contains(contexto.packageName),
    avisos = AvisoDePago.puedeAvisar(contexto),
    bateria = (contexto.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(contexto.packageName),
    accesibilidad = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?.contains("${contexto.packageName}/${ServicioDeAccesibilidad::class.java.name}") == true,
    ventana = Settings.canDrawOverlays(contexto),
)

fun abrirAjusteDelSistema(contexto: Context, accion: String, conPaquete: Boolean = false) {
    val intent = Intent(accion).apply {
        if (conPaquete) putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { contexto.startActivity(intent) }
}
