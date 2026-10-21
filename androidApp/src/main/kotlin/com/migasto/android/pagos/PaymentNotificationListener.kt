package com.migasto.android.pagos

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.migasto.android.MiGastoApp
import com.migasto.domain.OrigenDeLectura
import com.migasto.domain.ResultadoDeLectura
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

/**
 * Lee las notificaciones de las apps de pago y escribe el pago directo en `shared`: sin cola ni lector propio.
 * Qué es un pago nuevo (frenos de duplicados, constancias ya leídas, proveedores apagados) lo decide
 * `ProcesadorDePagos`; aquí solo se entrega el texto y se avisa al usuario.
 */
class PaymentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName !in PaquetesDePago.leidos) return
        val extras = sbn.notification?.extras ?: return
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val largo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val antiguedad = (System.currentTimeMillis() - sbn.postTime).milliseconds
        val id = "n|${sbn.key}|${sbn.postTime}"
        val contexto = applicationContext
        val app = contexto as MiGastoApp

        // Un solo hilo para todo lo que lee pagos: el freno de duplicados no es seguro entre hilos.
        app.ambito.launch(app.hiloDePagos) {
            val procesador = app.procesador.await()
            if (!procesador.esNotificacionNueva(id, antiguedad)) {
                Log.d(TAG, "ignorada: notificación antigua o ya leída")
                return@launch
            }
            when (val r = procesador.procesar("$titulo $texto $largo".trim(), OrigenDeLectura.Notificaciones)) {
                is ResultadoDeLectura.PagoNuevo -> {
                    // No se registra el texto completo: puede traer datos personales.
                    Log.d(TAG, "pago detectado: ${r.pago.tipoTexto} ${r.pago.provider.clave}")
                    EntregaDePago.entregar(app, r)
                }
                else -> Log.d(TAG, "descartada: $r")
            }
        }
    }

    private companion object {
        const val TAG = "PagosListener"
    }
}
