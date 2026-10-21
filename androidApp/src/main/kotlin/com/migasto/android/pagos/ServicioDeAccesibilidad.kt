package com.migasto.android.pagos

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.migasto.android.MiGastoApp
import com.migasto.domain.OrigenDeLectura
import com.migasto.domain.ProcesadorDePagos
import com.migasto.domain.ResultadoDeLectura
import kotlinx.coroutines.launch

/**
 * Lee la pantalla de las apps de pago (solo las de `accessibility_service_config.xml`) para registrar los pagos
 * que el usuario envía: la constancia de Yape no llega como notificación. También recibe el aviso de las
 * notificaciones como respaldo. Qué es un pago nuevo lo decide `ProcesadorDePagos`, igual que en el listener.
 */
class ServicioDeAccesibilidad : AccessibilityService() {
    // Ignora cambios de contenido de ventana a menos de 500 ms del anterior.
    private var ultimaLectura = 0L

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        if (evento == null) return
        when (evento.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val notificacion = evento.parcelableData as? Notification ?: return
                val extras = notificacion.extras
                val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val largo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                procesar("$titulo $texto $largo".trim(), OrigenDeLectura.Accesibilidad)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val ahora = System.currentTimeMillis()
                if (ahora - ultimaLectura < DEBOUNCE_MS) return
                ultimaLectura = ahora
                leerVentanaActiva()
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> leerVentanaActiva()
        }
    }

    override fun onInterrupt() = Unit

    /** Junta el texto de la pantalla y lo lee como una sola frase: el monto y «¡Yapeaste!» están en vistas distintas. */
    private fun leerVentanaActiva() {
        val raiz = rootInActiveWindow ?: return
        val texto = StringBuilder()
        juntarTexto(raiz, texto)
        if (texto.isNotEmpty()) procesar(texto.toString(), OrigenDeLectura.Pantalla)
    }

    private fun juntarTexto(nodo: AccessibilityNodeInfo?, salida: StringBuilder) {
        if (nodo == null || salida.length >= ProcesadorDePagos.LARGO_MAXIMO_DEL_TEXTO) return
        nodo.text?.toString()?.takeIf { it.isNotBlank() }?.let {
            if (salida.isNotEmpty()) salida.append(' ')
            salida.append(it)
        }
        for (i in 0 until nodo.childCount) juntarTexto(nodo.getChild(i), salida)
    }

    private fun procesar(texto: String, origen: OrigenDeLectura) {
        val app = applicationContext as MiGastoApp
        app.ambito.launch(app.hiloDePagos) {
            when (val r = app.procesador.await().procesar(texto, origen)) {
                is ResultadoDeLectura.PagoNuevo -> {
                    Log.d(TAG, "pago detectado: ${r.pago.tipoTexto} ${r.pago.provider.clave} ($origen)")
                    EntregaDePago.entregar(app, r)
                }
                else -> Unit
            }
        }
    }

    private companion object {
        const val TAG = "PagosAccesibilidad"
        const val DEBOUNCE_MS = 500L
    }
}
