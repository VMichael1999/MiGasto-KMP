package com.migasto.android.pagos

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.migasto.android.MiGastoApp
import kotlinx.coroutines.launch

/** Los botones «Guardar» e «Ignorar» del aviso de un ingreso por confirmar. Escribe directo en `shared`. */
class AccionDeAvisoReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val ref = intent.getStringExtra(EXTRA_REF) ?: return
        val guardar = when (intent.action) {
            ACCION_GUARDAR -> true
            ACCION_IGNORAR -> false
            else -> return
        }
        val manager = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(intent.getIntExtra(EXTRA_ID_DEL_AVISO, 0))

        val app = contexto.applicationContext as MiGastoApp
        val pendiente = goAsync()
        app.ambito.launch {
            try {
                val registro = app.registro.await()
                if (guardar) registro.confirmar(ref) else registro.eliminar(ref)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_GUARDAR = "com.migasto.PAGO_GUARDAR"
        const val ACCION_IGNORAR = "com.migasto.PAGO_IGNORAR"
        const val EXTRA_REF = "ref"
        const val EXTRA_ID_DEL_AVISO = "idDelAviso"
    }
}
