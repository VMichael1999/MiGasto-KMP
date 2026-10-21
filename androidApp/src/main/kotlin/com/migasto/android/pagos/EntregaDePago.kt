package com.migasto.android.pagos

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.migasto.android.MiGastoApp
import com.migasto.domain.ResultadoDeLectura
import com.migasto.domain.debeAbrirVentana

/**
 * Qué hacer con un pago nuevo (port de `deliver` de MyAccessibilityService): con el teléfono en uso y
 * desbloqueado, la ventana flotante; si no, se guarda ya y se avisa con una notificación.
 */
object EntregaDePago {
    private const val TAG = "EntregaDePago"

    /** La ventana flotante se puede ver: permiso, pantalla encendida y teléfono desbloqueado. */
    fun puedeMostrarVentana(contexto: Context): Boolean {
        if (!Settings.canDrawOverlays(contexto)) return false
        val energia = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
        val teclado = contexto.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return energia.isInteractive && !teclado.isKeyguardLocked
    }

    suspend fun entregar(app: MiGastoApp, lectura: ResultadoDeLectura.PagoNuevo) {
        val pago = lectura.pago
        if (debeAbrirVentana(pago) && puedeMostrarVentana(app)) {
            val intent = Intent(app, VentanaFlotanteService::class.java).apply {
                putExtra(VentanaFlotanteService.EXTRA_MONTO, pago.amount)
                putExtra(VentanaFlotanteService.EXTRA_COMERCIO, pago.peer)
                putExtra(VentanaFlotanteService.EXTRA_PROVEEDOR, pago.provider.clave)
                putExtra(VentanaFlotanteService.EXTRA_ES_INGRESO, pago.esIngreso)
                putExtra(VentanaFlotanteService.EXTRA_TEXTO, lectura.texto)
            }
            try {
                app.startService(intent)
                return
            } catch (e: Exception) {
                // Android puede negar iniciar el servicio desde segundo plano.
                Log.w(TAG, "No se pudo mostrar la ventana flotante", e)
            }
        }
        // Sin ventana visible: se guarda según las reglas de siempre y se avisa con una notificación.
        val guardado = app.registro.await().guardarPagoLeido(pago, lectura.texto)
        AvisoDePago.mostrar(app, app, guardado, pago)
    }
}
