package com.migasto.android.bloqueo

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Autenticación del teléfono (huella, rostro o PIN). Se reemplaza en las pruebas de interfaz. */
interface Autenticador {
    /** `true` si el teléfono tiene algún desbloqueo configurado. */
    fun disponible(): Boolean

    /** Pide desbloquear con [motivo]; `true` si salió bien, `false` si falló o se canceló. */
    suspend fun autenticar(motivo: String): Boolean
}

val LocalAutenticador = staticCompositionLocalOf<Autenticador> {
    object : Autenticador {
        override fun disponible() = false

        override suspend fun autenticar(motivo: String) = false
    }
}

/** El BiometricPrompt del sistema, con huella o rostro y, como alternativa, el PIN, patrón o contraseña del teléfono. */
class AutenticadorBiometrico(private val actividad: FragmentActivity) : Autenticador {
    private val permitidos = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override fun disponible(): Boolean =
        BiometricManager.from(actividad as Context).canAuthenticate(permitidos) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun autenticar(motivo: String): Boolean = suspendCancellableCoroutine { continuacion ->
        val aviso = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    if (continuacion.isActive) continuacion.resume(true)
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    if (continuacion.isActive) continuacion.resume(false)
                }
                // Un intento fallido (huella que no coincide) no cierra el aviso: el sistema deja reintentar.
            },
        )
        val datos = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MiGasto")
            .setSubtitle(motivo)
            .setAllowedAuthenticators(permitidos)
            .build()
        aviso.authenticate(datos)
        continuacion.invokeOnCancellation { aviso.cancelAuthentication() }
    }
}
