package com.migasto.android.pagos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.migasto.android.resumen.colorDeLaFuente
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.AccionDeVentana
import com.migasto.domain.SesionDeVentana
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** La ventana «Pago detectado». Dibuja lo que decide [SesionDeVentana] y avisa de cada toque. */
@Composable
internal fun VentanaDePagoUi(
    clave: Long,
    sesion: SesionDeVentana,
    nombre: String,
    categoria: String,
    hora: String,
    alElegirAccion: (AccionDeVentana) -> Unit,
) {
    val c = MiGastoTheme.colores
    val oscuro = isSystemInDarkTheme()
    val textos = remember(clave) { sesion.textos(nombre, categoria) }
    var progreso by remember(clave) { mutableFloatStateOf(1f) }
    var pausada by remember(clave) { mutableStateOf(false) }
    val visible = remember(clave) { MutableTransitionState(false).apply { targetState = true } }
    var arrastre by remember(clave) { mutableFloatStateOf(0f) }
    val umbral = with(LocalDensity.current) { 56.dp.toPx() }

    fun cerrar(accion: AccionDeVentana) {
        visible.targetState = false
        alElegirAccion(accion)
    }

    // La cuenta regresiva: un paso cada 100 ms. Al llegar a cero, la sesión pide guardar.
    LaunchedEffect(clave) {
        while (true) {
            delay(SesionDeVentana.PASO_MS)
            val accion = sesion.avanzar()
            progreso = sesion.progreso
            pausada = sesion.pausada
            if (accion != null) {
                cerrar(accion)
                break
            }
        }
    }
    // Un ingreso sin respuesta queda pendiente después de un minuto.
    if (!sesion.tieneCuentaRegresiva) {
        LaunchedEffect(clave) {
            delay(SesionDeVentana.ESPERA_DEL_INGRESO_MS)
            cerrar(sesion.alExpirarLaEspera())
        }
    }

    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, arrastre.roundToInt()) }
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.superficie)
                    // Tocar la ventana en cualquier punto pausa la cuenta.
                    .pointerInput(clave) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            sesion.pausar()
                            pausada = sesion.pausada
                        }
                    }
                    // Deslizarla hacia arriba hace lo mismo que su primer botón.
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { arrastre = minOf(0f, arrastre + it) },
                        onDragStopped = {
                            if (arrastre < -umbral) cerrar(sesion.alDeslizar()) else arrastre = 0f
                        },
                    )
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
            ) {
                val colorDeLaFuente = colorDeLaFuente(sesion.pago.provider)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        textos.fuente, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = colorDeLaFuente,
                        modifier = Modifier.clip(CircleShape).background(colorDeLaFuente.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(sesion.textoDeLaHora(hora).let { if (pausada) "En pausa" else it }, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = c.apagado)
                }
                Spacer(Modifier.height(14.dp))
                val colorDelMonto = if (textos.esIngreso) c.ingreso else c.tinta
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics { contentDescription = textos.montoAccesible }) {
                    Text(textos.prefijoDelMonto, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = colorDelMonto, modifier = Modifier.padding(bottom = 5.dp))
                    Text(textos.monto, style = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp, fontFeatureSettings = CifrasTabulares), color = colorDelMonto)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        append(textos.preposicion)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(textos.nombre)
                        pop()
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = c.tinta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${textos.categoria}  ›",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = c.tinta,
                    modifier = Modifier.clip(CircleShape).background(c.fondo).clickable { cerrar(sesion.alEditar()) }
                        .padding(horizontal = 10.dp, vertical = 8.dp).semantics { contentDescription = textos.descripcionDeLaCategoria },
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    textos.botones.dropLast(1).forEachIndexed { i, texto ->
                        BotonCallado(texto, Modifier.weight(1f)) { cerrar(if (i == 0) sesion.alPrimerBoton() else sesion.alEditar()) }
                    }
                    BotonPrincipal(
                        texto = textos.botones.last(), esIngreso = textos.esIngreso, conCuenta = sesion.tieneCuentaRegresiva,
                        progreso = progreso, oscuro = oscuro, descripcion = textos.descripcionDelBotonPrincipal, modifier = Modifier.weight(1.7f),
                    ) { cerrar(sesion.alGuardar()) }
                }
            }
        }
    }
}

@Composable
private fun BotonCallado(texto: String, modifier: Modifier, alTocar: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = alTocar).semantics { contentDescription = texto },
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MiGastoTheme.colores.apagado, maxLines = 1)
    }
}

/** El botón principal: con cuenta regresiva se vacía de color fuerte a tenue. */
@Composable
private fun BotonPrincipal(
    texto: String,
    esIngreso: Boolean,
    conCuenta: Boolean,
    progreso: Float,
    oscuro: Boolean,
    descripcion: String,
    modifier: Modifier,
    alTocar: () -> Unit,
) {
    val fuerte = if (esIngreso) (if (oscuro) Color(0xFF7FB6FF) else Color(0xFF86B4F5)) else ColoresMiGasto.VerdeNeon
    val tenue = if (esIngreso) (if (oscuro) Color(0xFF4A7DB8) else Color(0xFFD5E4FB)) else (if (oscuro) Color(0xFF4F8A4C) else Color(0xFFD7F3D4))
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).drawBehind {
            if (conCuenta) {
                drawRect(tenue)
                drawRect(fuerte, topLeft = Offset.Zero, size = Size(size.width * progreso.coerceIn(0f, 1f), size.height))
            } else {
                drawRect(fuerte)
            }
        }.clickable(onClick = alTocar).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = ColoresMiGasto.TintaSobreVerde, maxLines = 1)
    }
}
