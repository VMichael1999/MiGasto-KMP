package com.migasto.android.ubicacion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.migasto.domain.MotivoDeFallo
import com.migasto.domain.UbicacionCapturada
import kotlinx.coroutines.launch

/** El botón «Agregar ubicación»: pide el permiso si falta, toma la ubicación una vez y avisa el resultado. */
class AgregarUbicacion(val capturando: Boolean, val iniciar: () -> Unit)

@Composable
fun rememberAgregarUbicacion(alCapturar: (UbicacionCapturada) -> Unit, alFallar: (MotivoDeFallo) -> Unit): AgregarUbicacion {
    val contexto = LocalContext.current
    val servicio = remember { ServicioDeUbicacion(contexto) }
    val ambito = rememberCoroutineScope()
    var capturando by remember { mutableStateOf(false) }

    fun capturar() {
        capturando = true
        ambito.launch {
            val resultado = servicio.capturar()
            capturando = false
            when (resultado) {
                is ResultadoDeCaptura.Ok -> alCapturar(resultado.ubicacion)
                is ResultadoDeCaptura.Fallo -> alFallar(resultado.motivo)
            }
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { dados ->
        when {
            dados.values.any { it } -> capturar()
            // Si el sistema ya no vuelve a preguntar, el permiso quedó bloqueado: hay que ir a los ajustes.
            (contexto as? Activity)?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.ACCESS_FINE_LOCATION) } == true ->
                alFallar(MotivoDeFallo.DenegadaParaSiempre)
            else -> alFallar(MotivoDeFallo.Denegada)
        }
    }

    return AgregarUbicacion(capturando) {
        when {
            capturando -> Unit
            !servicio.serviciosEncendidos() -> alFallar(MotivoDeFallo.ServiciosApagados)
            servicio.tienePermiso() -> capturar()
            else -> pedirPermiso.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}

fun abrirAjustesDeLaApp(contexto: android.content.Context) {
    runCatching {
        contexto.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
