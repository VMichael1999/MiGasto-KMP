package com.migasto.android.bloqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.domain.ControlDeBloqueo
import com.migasto.domain.TextosDelBloqueo
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Cubre la app con la pantalla de bloqueo cuando el usuario activó el bloqueo (port de LockGate). Mientras no se
 * sabe si está activado ([activado] es `null`) no se muestra nada: así el contenido nunca se ve un instante.
 * Las reglas (cuándo bloquear y cuándo pedir el desbloqueo) están en `ControlDeBloqueo`, en `shared`.
 */
@Composable
fun PuertaDeBloqueo(activado: Boolean?, autenticador: Autenticador, contenido: @Composable () -> Unit) {
    val c = MiGastoTheme.colores
    if (activado == null) {
        Box(Modifier.fillMaxSize().background(c.fondo))
        return
    }
    val control = remember { ControlDeBloqueo(activadoAlAbrir = activado) { Clock.System.now() } }
    // El control no es estado de Compose: este número fuerza a redibujar cuando cambia.
    var version by remember { mutableIntStateOf(0) }
    val ambito = rememberCoroutineScope()

    fun desbloquear() {
        if (!control.alEmpezarAutenticacion()) return
        ambito.launch {
            val correcta = try {
                autenticador.autenticar(TextosDelBloqueo.MOTIVO_AL_ABRIR)
            } catch (e: Exception) {
                false
            }
            control.alTerminarAutenticacion(correcta)
            version++
        }
    }

    LaunchedEffect(Unit) { if (control.alAbrir()) desbloquear() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { control.alPasarAlFondo() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (control.alVolver(activado)) {
            version++
            desbloquear()
        }
    }

    val bloqueada = control.visible(activado).also { version }
    Box(Modifier.fillMaxSize()) {
        // Con la pantalla de bloqueo encima, lo de abajo tampoco se lee con TalkBack.
        Box(if (bloqueada) Modifier.clearAndSetSemantics { } else Modifier) { contenido() }
        if (bloqueada) PantallaDeBloqueo(alDesbloquear = ::desbloquear)
    }
}

@Composable
fun PantallaDeBloqueo(alDesbloquear: () -> Unit) {
    val c = MiGastoTheme.colores
    Box(
        Modifier.fillMaxSize().background(c.fondo)
            // Se traga los toques: nada de lo que está debajo se puede tocar.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.apagado, modifier = Modifier.size(40.dp))
            Box(Modifier.height(8.dp))
            Text(TextosDelBloqueo.TITULO, style = MaterialTheme.typography.headlineSmall, color = c.tinta, textAlign = TextAlign.Center)
            Text(TextosDelBloqueo.EXPLICACION, style = MaterialTheme.typography.bodyMedium, color = c.apagado, textAlign = TextAlign.Center)
            Box(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton)).background(ColoresMiGasto.VerdeNeon)
                    .clickable(onClick = alDesbloquear).semantics { contentDescription = TextosDelBloqueo.BOTON },
                contentAlignment = Alignment.Center,
            ) {
                Text(TextosDelBloqueo.BOTON, style = MaterialTheme.typography.titleLarge, color = ColoresMiGasto.TintaSobreVerde)
            }
        }
    }
}
