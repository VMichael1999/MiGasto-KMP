package com.migasto.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.migasto.android.bienvenida.BienvenidaPantalla
import com.migasto.android.bloqueo.AutenticadorBiometrico
import com.migasto.android.bloqueo.LocalAutenticador
import com.migasto.android.bloqueo.PuertaDeBloqueo
import com.migasto.android.tema.MiGastoTheme

// FragmentActivity (y no solo ComponentActivity): el BiometricPrompt del sistema lo necesita.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MiGastoApp
        setContent {
            val autenticador = remember { AutenticadorBiometrico(this) }
            val bloqueo by app.bloqueoActivado.collectAsStateWithLifecycle()
            val bienvenida by app.bienvenidaCompleta.collectAsStateWithLifecycle()
            MiGastoTheme {
                CompositionLocalProvider(LocalAutenticador provides autenticador) {
                    // Primero la bienvenida (solo la primera vez); luego, si está activado, el bloqueo.
                    if (bienvenida == false) {
                        BienvenidaPantalla(alTerminar = app::terminarBienvenida)
                    } else {
                        PuertaDeBloqueo(if (bienvenida == null) null else bloqueo, autenticador) { MiGastoNavegacion() }
                    }
                }
            }
        }
    }
}
