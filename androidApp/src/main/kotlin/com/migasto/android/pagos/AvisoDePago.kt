package com.migasto.android.pagos

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.migasto.android.MiGastoApp
import com.migasto.android.R
import com.migasto.domain.PagoGuardado
import com.migasto.domain.PagoLeido
import com.migasto.domain.TEXTOS_DEL_AVISO_PUBLICO
import com.migasto.domain.nombreVisible
import com.migasto.domain.textosDelAviso

/**
 * El aviso de un pago detectado (port de PaymentNotifier):
 * - Ingreso por confirmar: canal con sonido («Pagos detectados»), con botones Guardar e Ignorar.
 * - Gasto o ingreso ya guardado: canal silencioso («Pagos guardados»).
 * - En la pantalla bloqueada se oculta el monto y el nombre, salvo que el usuario lo permita.
 */
object AvisoDePago {
    private const val CANAL_DETECTADOS = "pagos_detectados"
    private const val CANAL_GUARDADOS = "pagos_guardados"
    private const val TAG = "AvisoDePago"

    fun puedeAvisar(contexto: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled()
    }

    private fun crearCanales(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CANAL_DETECTADOS, "Pagos detectados", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Ingresos que esperan tu confirmación"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_GUARDADOS, "Pagos guardados", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Gastos que MiGasto ya guardó, en silencio"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun accion(contexto: Context, accion: String, ref: String, idAviso: Int, codigo: Int): PendingIntent {
        val intent = Intent(contexto, AccionDeAvisoReceiver::class.java).apply {
            this.action = accion
            putExtra(AccionDeAvisoReceiver.EXTRA_REF, ref)
            putExtra(AccionDeAvisoReceiver.EXTRA_ID_DEL_AVISO, idAviso)
        }
        return PendingIntent.getBroadcast(contexto, codigo, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Avisa de un pago que ya se guardó (confirmado, o pendiente si es un ingreso). */
    suspend fun mostrar(contexto: Context, app: MiGastoApp, guardado: PagoGuardado, pago: PagoLeido) {
        if (!puedeAvisar(contexto)) {
            Log.w(TAG, "sin permiso para publicar notificaciones")
            return
        }
        crearCanales(contexto)
        val base = app.base.await()
        val textos = textosDelAviso(
            monto = pago.amount,
            nombre = nombreVisible(pago.peer, base.ajustes.aliases()),
            fuente = pago.provider,
            esIngreso = pago.esIngreso,
            guardado = guardado.guardado,
        )

        val abrir = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val alTocar = abrir?.let { PendingIntent.getActivity(contexto, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val canal = if (guardado.guardado) CANAL_GUARDADOS else CANAL_DETECTADOS
        val builder = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.ic_stat_boleta)
            .setContentTitle(textos.titulo)
            .setContentText(textos.texto)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (guardado.guardado) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)

        if (textos.pideConfirmacion) {
            val ref = guardado.movimiento.id
            builder.addAction(0, "Guardar", accion(contexto, AccionDeAvisoReceiver.ACCION_GUARDAR, ref, id, id * 2))
            builder.addAction(0, "Ignorar", accion(contexto, AccionDeAvisoReceiver.ACCION_IGNORAR, ref, id, id * 2 + 1))
        }

        if (base.ajustes.mostrarMontoEnPantallaBloqueada()) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            // En la pantalla bloqueada solo se ve que hubo un pago.
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            builder.setPublicVersion(
                NotificationCompat.Builder(contexto, canal)
                    .setSmallIcon(R.drawable.ic_stat_boleta)
                    .setContentTitle(TEXTOS_DEL_AVISO_PUBLICO.titulo)
                    .setContentText(TEXTOS_DEL_AVISO_PUBLICO.texto)
                    .build(),
            )
        }

        try {
            NotificationManagerCompat.from(contexto).notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "el sistema rechazó la notificación", e)
        }
    }
}
