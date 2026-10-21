package com.migasto.android.bienvenida

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.migasto.android.R
import com.migasto.android.pagos.abrirAjusteDelSistema
import com.migasto.android.pagos.leerSistema
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.domain.PasoDeBienvenida
import com.migasto.domain.PasoUi
import com.migasto.domain.PermisosDeBienvenida
import com.migasto.domain.TextosDeBienvenida
import com.migasto.domain.pasosDeBienvenida
import com.migasto.domain.siguientePasoDeBienvenida
import com.migasto.domain.textoDelBotonDeBienvenida
import com.migasto.domain.textoDelBotonSecundarioDeBienvenida

/** Primera pantalla (port de OnboardingScreen): qué lee la app, y los permisos uno por uno. */
@Composable
fun BienvenidaPantalla(alTerminar: () -> Unit) {
    val contexto = LocalContext.current
    var sistema by remember { mutableStateOf(leerSistema(contexto)) }
    // Al volver de los ajustes del sistema se actualizan los pasos.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { sistema = leerSistema(contexto) }
    val pedirAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { sistema = leerSistema(contexto) }
    val permisos = PermisosDeBienvenida(sistema.lectura, sistema.ventana, sistema.avisos, sistema.bateria)

    BienvenidaContenido(
        permisos = permisos,
        alPulsarPrincipal = {
            when (siguientePasoDeBienvenida(permisos)) {
                PasoDeBienvenida.Lectura -> abrirAjusteDelSistema(contexto, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                PasoDeBienvenida.Ventana -> runCatching {
                    contexto.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}")),
                    )
                }
                PasoDeBienvenida.Avisos ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pedirAvisos.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        abrirAjusteDelSistema(contexto, Settings.ACTION_APP_NOTIFICATION_SETTINGS, conPaquete = true)
                    }
                PasoDeBienvenida.Bateria -> abrirAjusteDelSistema(contexto, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                PasoDeBienvenida.Terminar -> alTerminar()
            }
        },
        alPulsarSecundario = alTerminar,
    )
}

@Composable
fun BienvenidaContenido(permisos: PermisosDeBienvenida, alPulsarPrincipal: () -> Unit, alPulsarSecundario: () -> Unit) {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxSize().background(c.fondo).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Espacio.xl, end = Espacio.xl, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).semantics { contentDescription = "MiGasto" }) {
                Icon(painterResource(R.drawable.ic_logo), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(TextosDeBienvenida.TITULO, style = MaterialTheme.typography.headlineMedium, color = c.tinta)
            Text(TextosDeBienvenida.SUBTITULO, style = MaterialTheme.typography.bodyMedium, color = c.apagado)
            Spacer(Modifier.height(12.dp))
            CajaDePrivacidad()
            Spacer(Modifier.height(12.dp))
            pasosDeBienvenida(permisos).forEach { PasoDeLaLista(it) }
        }
        Column(Modifier.padding(start = Espacio.xl, end = Espacio.xl, top = 4.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton)).background(ColoresMiGasto.VerdeNeon)
                    .clickable(onClick = alPulsarPrincipal),
                contentAlignment = Alignment.Center,
            ) {
                Text(textoDelBotonDeBienvenida(permisos), style = MaterialTheme.typography.titleLarge, color = ColoresMiGasto.TintaSobreVerde)
            }
            TextButton(onClick = alPulsarSecundario) {
                Text(textoDelBotonSecundarioDeBienvenida(permisos), style = MaterialTheme.typography.labelLarge, color = c.marcaTinta)
            }
        }
    }
}

@Composable
private fun CajaDePrivacidad() {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(c.superficie).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = c.apagado, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(TextosDeBienvenida.PRIVACIDAD_TITULO, style = MaterialTheme.typography.titleMedium, color = c.tinta)
        }
        Linea(TextosDeBienvenida.LEE, TextosDeBienvenida.LEE_DETALLE)
        Linea(TextosDeBienvenida.NO_LEE, TextosDeBienvenida.NO_LEE_DETALLE)
        Linea(TextosDeBienvenida.GUARDA, TextosDeBienvenida.GUARDA_DETALLE)
    }
}

@Composable
private fun Linea(negrita: String, resto: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(negrita) }
            append(resto)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MiGastoTheme.colores.tinta,
    )
}

@Composable
private fun PasoDeLaLista(paso: PasoUi) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) { contentDescription = paso.descripcion }.clearAndSetSemantics { contentDescription = paso.descripcion },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (paso.listo) ColoresMiGasto.VerdeNeon else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.5.dp, if (paso.listo) ColoresMiGasto.VerdeNeon else c.linea, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (paso.listo) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = ColoresMiGasto.TintaSobreVerde, modifier = Modifier.size(16.dp))
            } else {
                Text("${paso.numero}", style = MaterialTheme.typography.labelLarge, color = c.tinta)
            }
        }
        Column {
            Text(paso.titulo, style = MaterialTheme.typography.titleMedium, color = c.tinta)
            Text(if (paso.listo) "Listo" else paso.pista, style = MaterialTheme.typography.bodySmall, color = c.apagado)
        }
    }
}
