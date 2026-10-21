package com.migasto.android.resumen

import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.pagos.abrirAjusteDelSistema
import com.migasto.android.pagos.leerSistema
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano
import com.migasto.domain.AvisoDeConfiguracion
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.MesElegido
import com.migasto.domain.PaymentSource
import com.migasto.domain.calcularAvisosDeConfiguracion
import com.migasto.domain.claveDePausa
import com.migasto.domain.textosDelAvisoDeConfiguracion
import kotlin.time.Clock

@Composable
fun ResumenPantalla(alVerTodos: () -> Unit = {}, alBuscar: () -> Unit = {}, alAbrir: (String) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: ResumenViewModel = viewModel(
        factory = viewModelFactory { initializer { ResumenViewModel(app.base, app.montosOcultos) } },
    )
    val ui by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var sistema by remember { mutableStateOf(leerSistema(contexto)) }
    // Al volver de los ajustes del sistema se vuelve a mirar qué quedó activado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { sistema = leerSistema(contexto) }
    val pedirPermisoDeAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { sistema = leerSistema(contexto) }

    ui?.let { resumen ->
        val aviso = calcularAvisosDeConfiguracion(
            esAndroid = true,
            lecturaActivada = sistema.lectura,
            bateriaSinRestriccion = sistema.bateria,
            ahora = Clock.System.now(),
            avisosActivados = sistema.avisos,
            avisosPospuestosEn = resumen.avisosPospuestosEn,
            bateriaPospuestaEn = resumen.bateriaPospuestaEn,
        ).firstOrNull()
        ResumenContenido(
            ui = resumen, alAlternarOculto = vm::alternarOculto, alVerTodos = alVerTodos, alBuscar = alBuscar, alAbrir = alAbrir,
            aviso = aviso,
            alActuarSobreAviso = {
                when (aviso) {
                    AvisoDeConfiguracion.LecturaApagada -> abrirAjusteDelSistema(contexto, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    AvisoDeConfiguracion.AvisosApagados ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pedirPermisoDeAvisos.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            abrirAjusteDelSistema(contexto, Settings.ACTION_APP_NOTIFICATION_SETTINGS, conPaquete = true)
                        }
                    AvisoDeConfiguracion.BateriaRestringida -> abrirAjusteDelSistema(contexto, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    null -> Unit
                }
            },
            alPosponerAviso = { aviso?.let(::claveDePausa)?.let(vm::posponer) },
            alElegirMes = vm::elegirMes,
        )
    }
}

internal const val OCULTO = "••••"

@Composable
fun ResumenContenido(
    ui: ResumenUi,
    alAlternarOculto: () -> Unit,
    alVerTodos: () -> Unit,
    alBuscar: () -> Unit,
    alAbrir: (String) -> Unit,
    aviso: AvisoDeConfiguracion? = null,
    alActuarSobreAviso: () -> Unit = {},
    alPosponerAviso: () -> Unit = {},
    alElegirMes: (MesElegido) -> Unit = {},
) {
    val c = MiGastoTheme.colores
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(ui, alAlternarOculto, alBuscar, alElegirMes)
        Column(Modifier.padding(horizontal = Espacio.xl), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Saldo(ui)
            IngresosYGastos(ui)
            aviso?.let { AvisoDeConfiguracionTarjeta(it, alActuarSobreAviso, alPosponerAviso) }
            ui.porConfirmarTexto?.let { PorConfirmar(it, if (ui.oculto) OCULTO else ui.porConfirmarMonto) }
            ui.presupuesto?.let { Presupuesto(it, ui.oculto) }
            SeccionHoy(ui, alVerTodos, alAbrir)
            Spacer(Modifier.height(Espacio.lg))
        }
    }
}

@Composable
private fun BarraSuperior(ui: ResumenUi, alAlternarOculto: () -> Unit, alBuscar: () -> Unit, alElegirMes: (MesElegido) -> Unit) {
    val c = MiGastoTheme.colores
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = Espacio.xl, end = 10.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.clip(RoundedCornerShape(Radio.campo)).clickable(enabled = ui.meses.size > 1) { menuAbierto = true }
                    .semantics { contentDescription = "Mes: ${ui.mes}. Elegir otro mes" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ui.mes, style = MaterialTheme.typography.headlineMedium, color = c.tinta)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = c.apagado, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                ui.meses.forEach { opcion ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                opcion.nombre,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (opcion.mes == ui.mesElegido) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            alElegirMes(opcion.mes)
                        },
                    )
                }
            }
        }
        BotonDeIcono(
            if (ui.oculto) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            if (ui.oculto) "Mostrar montos" else "Ocultar montos",
            alAlternarOculto,
        )
        BotonDeIcono(Icons.Outlined.Search, "Buscar movimientos", alBuscar)
    }
}

@Composable
internal fun BotonDeIcono(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, alTocar: () -> Unit) {
    Box(
        Modifier.size(Tamano.botonDeIcono).clip(RoundedCornerShape(Radio.campo)).clickable(onClick = alTocar)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = MiGastoTheme.colores.tinta, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Saldo(ui: ResumenUi) {
    val c = MiGastoTheme.colores
    Column(Modifier.padding(top = 6.dp)) {
        Text(ui.textoDelSaldo, style = MaterialTheme.typography.bodyMedium, color = c.apagado)
        Row(verticalAlignment = Alignment.Bottom) {
            if (ui.oculto) {
                Text(OCULTO, style = MaterialTheme.typography.displayLarge, color = c.tinta)
            } else {
                Text(
                    "S/",
                    style = MaterialTheme.typography.titleLarge.copy(color = c.apagado, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(end = 6.dp, bottom = 9.dp),
                )
                Text(ui.saldo, style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = CifrasTabulares), color = c.tinta)
            }
        }
    }
}

@Composable
private fun IngresosYGastos(ui: ResumenUi) {
    val c = MiGastoTheme.colores
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Resumencito("Ingresos", if (ui.oculto) OCULTO else ui.ingresos, Icons.Outlined.ArrowUpward, c.ingreso, Modifier.weight(1f))
        Resumencito("Gastos", if (ui.oculto) OCULTO else ui.gastos, Icons.Outlined.ArrowDownward, c.barraDeGastos, Modifier.weight(1f))
    }
}

@Composable
private fun Resumencito(
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    tinte: Color,
    modifier: Modifier,
) {
    val c = MiGastoTheme.colores
    Column(
        modifier.clip(RoundedCornerShape(Radio.tarjeta)).background(c.superficie).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(15.dp))
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = c.apagado)
        }
        Text(valor, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = CifrasTabulares), color = c.tinta)
    }
}

/** Avisa cuando la lectura de pagos no puede funcionar: permiso apagado o batería restringida. */
@Composable
private fun AvisoDeConfiguracionTarjeta(aviso: AvisoDeConfiguracion, alActuar: () -> Unit, alPosponer: () -> Unit) {
    val c = MiGastoTheme.colores
    val textos = remember(aviso) { textosDelAvisoDeConfiguracion(aviso) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.presupuestoCercaSuave)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = textos.descripcion },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = c.presupuestoCerca, modifier = Modifier.padding(top = 2.dp).size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(textos.titulo, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold), color = c.tinta)
                Spacer(Modifier.height(2.dp))
                Text(textos.cuerpo, style = MaterialTheme.typography.bodySmall, color = c.apagado)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (textos.sePuedePosponer) TextButton(onClick = alPosponer) { Text("Ahora no", color = c.marcaTinta) }
            TextButton(onClick = alActuar) { Text(textos.accion, color = c.marcaTinta) }
        }
    }
}

@Composable
private fun PorConfirmar(texto: String, monto: String) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.tarjeta)).background(c.porConfirmarSuave)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.porConfirmar, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold))
                append(texto)
                pop()
                append(" por confirmar")
            },
            style = MaterialTheme.typography.bodyMedium, color = c.porConfirmar, modifier = Modifier.weight(1f),
        )
        Text(
            monto,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = CifrasTabulares), color = c.porConfirmar,
        )
    }
}

@Composable
private fun Presupuesto(p: PresupuestoUi, oculto: Boolean) {
    val c = MiGastoTheme.colores
    val color = when (p.estado) {
        EstadoDelPresupuesto.Bien -> c.presupuestoBien
        EstadoDelPresupuesto.Cerca -> c.presupuestoCerca
        EstadoDelPresupuesto.Pasado -> c.presupuestoPasado
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(c.superficie).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (p.estado == EstadoDelPresupuesto.Bien) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null, tint = color, modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(p.titulo, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.weight(1f))
            // Con los montos ocultos tampoco se ve cuánto del presupuesto va gastado.
            if (!oculto) Text(p.detalle, style = MaterialTheme.typography.labelMedium, color = c.apagado)
        }
        Box(Modifier.fillMaxWidth().height(Tamano.pista).clip(CircleShape).background(c.linea)) {
            if (!oculto) Box(Modifier.fillMaxHeight().fillMaxWidth(p.relleno).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun SeccionHoy(ui: ResumenUi, alVerTodos: () -> Unit, alAbrir: (String) -> Unit) {
    val c = MiGastoTheme.colores
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ui.tituloDeLaLista, style = MaterialTheme.typography.titleLarge, color = c.tinta, modifier = Modifier.weight(1f))
            Text(
                "Ver todos", style = MaterialTheme.typography.labelLarge, color = c.marcaTinta,
                modifier = Modifier.clickable(onClick = alVerTodos).padding(vertical = 6.dp),
            )
        }
        if (ui.hoy.isEmpty()) {
            Text(ui.textoSinMovimientos, style = MaterialTheme.typography.bodyMedium, color = c.apagado)
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(c.superficie)) {
                ui.hoy.forEachIndexed { i, fila ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.linea))
                    FilaDelMovimiento(fila, ui.oculto) { alAbrir(fila.id) }
                }
            }
        }
    }
}

@Composable
internal fun FilaDelMovimiento(f: FilaDeMovimiento, oculto: Boolean, alTocar: () -> Unit) {
    val c = MiGastoTheme.colores
    Row(Modifier.fillMaxWidth().clickable(onClick = alTocar).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(Tamano.iconoDeCategoria).clip(RoundedCornerShape(Radio.icono))
                .background(if (f.esIngreso) c.ingresoSuave else c.fondoDeIcono),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                f.iconoPropio?.let { iconoPropio(it) } ?: iconoDeCategoria(f.categoria),
                contentDescription = null, tint = if (f.esIngreso) c.ingreso else c.apagado, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.nombre, style = MaterialTheme.typography.titleSmall, color = c.tinta, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(colorDeLaFuente(f.fuente), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(f.fuenteYHora, style = MaterialTheme.typography.bodySmall, color = c.apagado, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (oculto) OCULTO else f.monto,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = CifrasTabulares),
                color = if (f.esIngreso) c.ingreso else c.tinta,
            )
            if (f.porConfirmar) {
                Text(
                    "Por confirmar",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MaterialTheme.typography.labelSmall.fontFamily),
                    color = c.porConfirmar,
                    modifier = Modifier.padding(top = 3.dp).clip(RoundedCornerShape(Radio.chip)).background(c.porConfirmarSuave)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun colorDeLaFuente(fuente: PaymentSource): Color {
    val c = MiGastoTheme.colores
    return when (fuente) {
        PaymentSource.Yape -> c.yape
        PaymentSource.Plin -> c.plin
        PaymentSource.Tarjeta -> c.tarjeta
        PaymentSource.GooglePay -> c.googlePay
        else -> c.manual
    }
}
