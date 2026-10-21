package com.migasto.android.pagos

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.migasto.android.MainActivity
import com.migasto.android.MiGastoApp
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.AccionDeVentana
import com.migasto.domain.PagoLeido
import com.migasto.domain.PaymentSource
import com.migasto.domain.SesionDeVentana
import com.migasto.domain.nombreVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La ventana flotante «Pago detectado» (port de OverlayService). Qué pasa con el pago, la cuenta regresiva y los
 * textos están en `SesionDeVentana` (en `shared`); aquí solo se abre la ventana del sistema y se dibuja con
 * Compose. Escribe directo en la base, sin cola.
 */
class VentanaFlotanteService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private val registroDelCiclo = LifecycleRegistry(this)
    private val controladorDeEstado = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registroDelCiclo
    override val savedStateRegistry: SavedStateRegistry get() = controladorDeEstado.savedStateRegistry

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var administrador: WindowManager? = null
    private var vista: ComposeView? = null
    private var datos by mutableStateOf<DatosDeVentana?>(null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        controladorDeEstado.performAttach()
        controladorDeEstado.performRestore(null)
        registroDelCiclo.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val monto = intent.getDoubleExtra(EXTRA_MONTO, 0.0)
        val proveedor = PaymentSource.desdeClave(intent.getStringExtra(EXTRA_PROVEEDOR) ?: "") ?: PaymentSource.Manual
        val pago = PagoLeido(monto, intent.getStringExtra(EXTRA_COMERCIO) ?: "Desconocido", proveedor, intent.getBooleanExtra(EXTRA_ES_INGRESO, false))
        val texto = intent.getStringExtra(EXTRA_TEXTO) ?: ""
        if (monto <= 0.0) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Si había otra ventana sin resolver, ese pago queda pendiente en vez de perderse.
        datos?.let { anterior -> resolver(anterior, anterior.sesion.alSerReemplazada()) }

        val app = applicationContext as MiGastoApp
        ambito.launch {
            val (sesion, nombre, etiqueta) = withContext(Dispatchers.IO) {
                val registro = app.registro.await()
                val base = app.base.await()
                val sesion = registro.nuevaSesionDeVentana(pago, texto)
                Triple(sesion, nombreVisible(pago.peer, base.ajustes.aliases()), sesion.categoria.let { c -> com.migasto.domain.etiquetaDeCategoria(c, null, emptyList()) })
            }
            datos = DatosDeVentana(sesion, nombre, etiqueta, id = System.nanoTime(), hora = com.migasto.domain.formatearHora(kotlin.time.Clock.System.now()))
            mostrar()
        }
        return START_NOT_STICKY
    }

    private fun mostrar() {
        if (vista != null) return
        administrador = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VentanaFlotanteService)
            setViewTreeSavedStateRegistryOwner(this@VentanaFlotanteService)
            setContent {
                MiGastoTheme {
                    datos?.let { d ->
                        VentanaDePagoUi(
                            clave = d.id, sesion = d.sesion, nombre = d.nombre, categoria = d.categoria, hora = d.hora,
                            alElegirAccion = { accion -> resolver(d, accion) },
                        )
                    }
                }
            }
        }
        val parametros = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = alturaDeLaBarraDeEstado() + (6 * resources.displayMetrics.density).toInt()
        }
        try {
            administrador?.addView(composeView, parametros)
        } catch (e: Exception) {
            // Sin permiso de superposición: el pago queda pendiente en la app.
            datos?.let { resolver(it, AccionDeVentana.DejarPendiente) }
            stopSelf()
            return
        }
        vista = composeView
        registroDelCiclo.currentState = Lifecycle.State.RESUMED
        vibrarUnaVez()
    }

    /** Hace lo que decidió la ventana, la cierra y, si toca, abre la app. */
    private fun resolver(d: DatosDeVentana, accion: AccionDeVentana) {
        if (datos?.id != d.id) return // ya se resolvió esta ventana
        datos = null
        val app = applicationContext as MiGastoApp
        app.ambito.launch { app.registro.await().aplicar(accion, d.sesion.pago, d.sesion.texto) }
        if (accion == AccionDeVentana.DejarPendienteYAbrirLaApp) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        ambito.launch {
            delay(SALIDA_MS) // deja terminar la animación de salida
            if (datos == null) {
                quitarVista()
                stopSelf()
            }
        }
    }

    private fun quitarVista() {
        vista?.let { runCatching { administrador?.removeView(it) } }
        vista = null
    }

    private fun alturaDeLaBarraDeEstado(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
    }

    /** Vibración corta al aparecer, para notarla sin mirar el teléfono. */
    private fun vibrarUnaVez() {
        runCatching {
            val vibrador = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!vibrador.hasVibrator()) return
            vibrador.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onDestroy() {
        quitarVista()
        registroDelCiclo.currentState = Lifecycle.State.DESTROYED
        ambito.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MONTO = "monto"
        const val EXTRA_COMERCIO = "comercio"
        const val EXTRA_PROVEEDOR = "proveedor"
        const val EXTRA_ES_INGRESO = "esIngreso"
        const val EXTRA_TEXTO = "texto"
        private const val SALIDA_MS = 260L
    }
}

/** Lo que muestra la ventana; [id] distingue una ventana de la siguiente. */
internal class DatosDeVentana(val sesion: SesionDeVentana, val nombre: String, val categoria: String, val id: Long, val hora: String)
