package com.migasto.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Port de la lógica de lib/presentation/lock/lock_gate.dart y del interruptor de Ajustes. La autenticación
// (BiometricPrompt en Android, LocalAuthentication en iPhone) la hace cada sistema; qué pasa antes y después
// se decide aquí.

/** Los textos del bloqueo, iguales en las dos apps. */
object TextosDelBloqueo {
    const val TITULO = "MiGasto está bloqueado"
    const val EXPLICACION = "Usa tu huella, rostro o el PIN del teléfono para entrar."
    const val BOTON = "Desbloquear"
    const val MOTIVO_AL_ABRIR = "Desbloquea MiGasto para ver tus movimientos"
    const val MOTIVO_AL_ACTIVAR = "Confirma para activar el bloqueo de MiGasto"
    const val SIN_BLOQUEO_EN_EL_TELEFONO = "Configura un PIN, huella o rostro en tu teléfono para usar el bloqueo."
}

/**
 * Cuándo se cubre la app con la pantalla de bloqueo. Es de un solo hilo (el de la interfaz).
 *
 * - Si el bloqueo está activado, la app abre bloqueada y pide desbloqueo.
 * - Al volver de segundo plano después de [gracia] (30 s) se vuelve a bloquear.
 * - El aviso de autenticación del sistema también pausa la app: se ignora mientras dura.
 */
class ControlDeBloqueo(
    activadoAlAbrir: Boolean,
    private val gracia: Duration = 30.seconds,
    private val reloj: () -> Instant,
) {
    private var bloqueada: Boolean = activadoAlAbrir
    private var autenticando = false
    private var pasoAlFondoEn: Instant? = null

    /** `true` si hay que pedir el desbloqueo apenas abre la app. */
    fun alAbrir(): Boolean = bloqueada

    /** La pantalla de bloqueo se ve si está bloqueada y el bloqueo sigue activado. */
    fun visible(activado: Boolean): Boolean = bloqueada && activado

    fun alPasarAlFondo() {
        if (autenticando) return
        pasoAlFondoEn = reloj()
    }

    /** Devuelve `true` si hay que bloquear de nuevo y pedir el desbloqueo. */
    fun alVolver(activado: Boolean): Boolean {
        if (autenticando) return false
        val desde = pasoAlFondoEn
        pasoAlFondoEn = null
        val fuera = if (desde == null) Duration.ZERO else reloj() - desde
        if (activado && fuera >= gracia) {
            bloqueada = true
            return true
        }
        return false
    }

    /** `false` si ya hay una autenticación en curso (no se abre otra encima). */
    fun alEmpezarAutenticacion(): Boolean {
        if (autenticando) return false
        autenticando = true
        return true
    }

    fun alTerminarAutenticacion(correcta: Boolean) {
        autenticando = false
        if (correcta) bloqueada = false
    }
}

enum class DecisionAlActivar {
    /** El teléfono no tiene PIN, huella ni rostro: se explica en vez de activar. */
    Explicar,

    /** Se pide autenticarse para confirmar que el usuario puede desbloquear. */
    PedirConfirmacion,
}

fun decidirAlActivar(telefonoSoportaBloqueo: Boolean): DecisionAlActivar =
    if (telefonoSoportaBloqueo) DecisionAlActivar.PedirConfirmacion else DecisionAlActivar.Explicar

/** El bloqueo solo se activa si la confirmación salió bien. */
fun activarSiConfirmo(correcta: Boolean): Boolean = correcta
