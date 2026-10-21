package com.migasto.android.ajustes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.pagos.AvisoDePago
import com.migasto.android.pagos.ServicioDeAccesibilidad
import com.migasto.android.resumen.iconoPropio
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano
import com.migasto.domain.ContenidoDeRespaldo
import com.migasto.domain.ExcepcionDeRespaldo
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.mensajeDeRestauracion
import com.migasto.domain.nombreDelArchivoDeRespaldo
import com.migasto.domain.parsearPresupuesto
import com.migasto.domain.presupuestoParaEditar
import com.migasto.domain.textoDeRestauracion
import com.migasto.domain.textoDelPresupuestoEnAjustes
import com.migasto.domain.validarContrasenaDeRespaldo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

@Composable
fun AjustesPantalla(alAvisar: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: AjustesViewModel = viewModel(factory = viewModelFactory { initializer { AjustesViewModel(app.base, app.registro) } })
    val ui by vm.estado.collectAsStateWithLifecycle()
    ui?.let { AjustesContenido(it, vm, alAvisar) }
}

private data class EstadoDelSistema(val lectura: Boolean, val avisos: Boolean, val bateria: Boolean, val accesibilidad: Boolean, val ventana: Boolean)

private fun leerSistema(contexto: Context): EstadoDelSistema = EstadoDelSistema(
    lectura = NotificationManagerCompat.getEnabledListenerPackages(contexto).contains(contexto.packageName),
    avisos = AvisoDePago.puedeAvisar(contexto),
    bateria = (contexto.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(contexto.packageName),
    accesibilidad = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?.contains("${contexto.packageName}/${ServicioDeAccesibilidad::class.java.name}") == true,
    ventana = Settings.canDrawOverlays(contexto),
)

private fun abrirAjusteDelSistema(contexto: Context, accion: String, conPaquete: Boolean = false) {
    val intent = Intent(accion).apply {
        if (conPaquete) putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { contexto.startActivity(intent) }
}

private enum class Hoja { Presupuesto, Aprendidas, Propias, Alias, BorrarUbicaciones }

private sealed interface DialogoDeRespaldo {
    data object PedirContrasenaParaCrear : DialogoDeRespaldo
    data class PedirContrasenaParaAbrir(val datos: ByteArray) : DialogoDeRespaldo
    data class Esperando(val mensaje: String) : DialogoDeRespaldo
    data class ConfirmarRestauracion(val contenido: ContenidoDeRespaldo, val nuevos: Int) : DialogoDeRespaldo
}

@Composable
private fun AjustesContenido(ui: AjustesUi, vm: AjustesViewModel, alAvisar: (String) -> Unit) {
    val c = MiGastoTheme.colores
    val contexto = LocalContext.current
    val ambito = rememberCoroutineScope()
    var hoja by remember { mutableStateOf<Hoja?>(null) }
    var dialogo by remember { mutableStateOf<DialogoDeRespaldo?>(null) }
    var respaldoListo by remember { mutableStateOf<ByteArray?>(null) }
    var sistema by remember { mutableStateOf(leerSistema(contexto)) }
    // Al volver de los ajustes del sistema se vuelve a mirar qué quedó activado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { sistema = leerSistema(contexto) }
    val pedirPermisoDeAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { sistema = leerSistema(contexto) }

    val guardarArchivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = respaldoListo
        respaldoListo = null
        if (uri != null && bytes != null) {
            ambito.launch {
                val bien = withContext(Dispatchers.IO) {
                    runCatching { contexto.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("sin salida") }.isSuccess
                }
                if (bien) {
                    vm.respaldoGuardado()
                    alAvisar("Respaldo creado. Guárdalo en un lugar seguro (Drive, correo, tu computadora).")
                } else {
                    alAvisar("No se pudo crear el respaldo.")
                }
            }
        }
    }
    val abrirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ambito.launch {
                val datos = withContext(Dispatchers.IO) { runCatching { contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
                if (datos == null) alAvisar("No se pudo leer el archivo.") else dialogo = DialogoDeRespaldo.PedirContrasenaParaAbrir(datos)
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl).padding(bottom = 24.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = c.tinta, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))

        Etiqueta("Registro automático")
        Caja {
            FilaDeAjuste(
                Icons.Outlined.Sms, "Lectura de pagos", subtitulo = "Lee las notificaciones de Yape, Plin y Google Wallet",
                estado = sistema.lectura to (if (sistema.lectura) "Activa" else "Activar"),
                alTocar = { abrirAjusteDelSistema(contexto, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.Notifications, "Avisos de pagos", subtitulo = "Una notificación cuando detecta un pago con la pantalla bloqueada",
                estado = sistema.avisos to (if (sistema.avisos) "Activos" else "Activar"),
                alTocar = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !sistema.avisos) {
                        pedirPermisoDeAvisos.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        abrirAjusteDelSistema(contexto, Settings.ACTION_APP_NOTIFICATION_SETTINGS, conPaquete = true)
                    }
                },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.Lock, "Monto en pantalla bloqueada",
                subtitulo = if (ui.montoEnPantallaBloqueada) "Visible: el aviso muestra el monto y el nombre" else "Oculto: solo dice «Pago detectado»",
                interruptor = ui.montoEnPantallaBloqueada, alCambiar = { vm.alternarMontoEnPantallaBloqueada() },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.BatteryChargingFull, "Batería sin restricciones", subtitulo = "Evita que el teléfono duerma la lectura y se pierdan pagos",
                estado = sistema.bateria to (if (sistema.bateria) "Activa" else "Configurar"),
                alTocar = { abrirAjusteDelSistema(contexto, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.Sms, "Pagos que envías", subtitulo = "Lee la constancia de Yape al pagar o enviar",
                estado = sistema.accesibilidad to (if (sistema.accesibilidad) "Activa" else "Activar"),
                alTocar = { abrirAjusteDelSistema(contexto, Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.Notifications, "Ventana flotante", subtitulo = "Muestra cada pago para que lo confirmes",
                estado = sistema.ventana to (if (sistema.ventana) "Activa" else "Activar"),
                alTocar = {
                    runCatching {
                        contexto.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            Separador()
            FilaDeAjuste(
                Icons.Outlined.Lock, "Guardar ingresos automáticamente",
                subtitulo = if (ui.ingresosAutomaticos) "Los ingresos se guardan solos, como los gastos." else "Apagado: cada ingreso espera tu confirmación.",
                interruptor = ui.ingresosAutomaticos, alCambiar = { vm.alternarIngresosAutomaticos() },
            )
            Separador()
            listOf("yape" to "Yape", "plin" to "Plin", "googlePay" to "Google Wallet").forEachIndexed { i, (clave, nombre) ->
                if (i > 0) Separador()
                FilaDeProveedor(nombre, clave, ui.proveedores[clave] == true) { vm.alternarProveedor(clave) }
            }
        }

        Etiqueta("Dónde pagaste")
        Caja {
            FilaDeAjuste(Icons.Outlined.Place, "Borrar todas las ubicaciones", subtitulo = "Los movimientos se mantienen", alTocar = { hoja = Hoja.BorrarUbicaciones })
        }

        Etiqueta("Tus datos")
        Caja {
            FilaDeAjuste(Icons.Outlined.AccountBalanceWallet, "Presupuesto de gastos", textoFinal = textoDelPresupuestoEnAjustes(ui.presupuesto), alTocar = { hoja = Hoja.Presupuesto })
            Separador()
            FilaDeAjuste(Icons.Outlined.Label, "Aprendidas de tus cambios", subtitulo = "Categorías que la app ya recuerda por comercio", alTocar = { hoja = Hoja.Aprendidas })
            Separador()
            FilaDeAjuste(Icons.Outlined.Label, "Mis categorías", subtitulo = "Las que creaste, con tu ícono", alTocar = { hoja = Hoja.Propias })
            Separador()
            FilaDeAjuste(Icons.Outlined.Label, "Nombres guardados", subtitulo = "Cómo ves a cada persona o comercio", alTocar = { hoja = Hoja.Alias })
            Separador()
            FilaDeAjuste(Icons.Outlined.Description, "Exportar a CSV", subtitulo = "Copia tus movimientos, sin ubicaciones", alTocar = {
                ambito.launch {
                    val texto = vm.csv()
                    (contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("MiGasto", texto))
                    alAvisar("Movimientos copiados al portapapeles")
                }
            })
            Separador()
            FilaDeAjuste(Icons.Outlined.Lock, "Respaldo cifrado", subtitulo = ui.subtituloDelRespaldo, alTocar = { dialogo = DialogoDeRespaldo.PedirContrasenaParaCrear })
            Separador()
            FilaDeAjuste(Icons.Outlined.Description, "Restaurar un respaldo", subtitulo = "Desde un archivo .mgb, con tu contraseña", alTocar = { abrirArchivo.launch(arrayOf("*/*")) })
        }
        Text("Tus datos se guardan solo en este teléfono.", style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
    }

    when (hoja) {
        Hoja.Presupuesto -> DialogoDePresupuesto(ui.presupuesto, alCerrar = { hoja = null }) { monto ->
            vm.guardarPresupuesto(monto)
            alAvisar(if (monto > 0) "Presupuesto actualizado" else "Presupuesto quitado")
        }
        Hoja.Aprendidas -> HojaDeLista(
            "Aprendidas de tus cambios", "Cuando cambias la categoría de un pago, la app lo recuerda para ese comercio.",
            "Todavía no cambiaste ninguna categoría.", alCerrar = { hoja = null },
            pie = if (ui.aprendidas.isNotEmpty()) ("Olvidar todas" to vm::olvidarTodasLasAprendidas) else null,
        ) {
            ui.aprendidas.forEach { FilaDeLista(it.comercio, it.categoria, "Olvidar ${it.comercio}") { vm.olvidarAprendida(it.comercio) } }
        }
        Hoja.Propias -> HojaDeLista(
            "Mis categorías", "Las creas al registrar un movimiento, con \"Nueva\". Si borras una, sus movimientos pasan a verse como \"Otros\".",
            "Aún no creaste ninguna.", alCerrar = { hoja = null },
        ) {
            ui.propias.forEach { p ->
                FilaDeLista(p.nombre, if (p.tipo == TipoMovimiento.Ingreso) "Ingreso" else "Gasto", "Borrar ${p.nombre}", icono = iconoPropio(p.icono)) { vm.quitarPropia(p.id) }
            }
        }
        Hoja.Alias -> HojaDeLista(
            "Nombres guardados", "Para ponerle nombre a alguien, abre un movimiento suyo y toca \"De\" o \"Para\".",
            "Aún no has guardado ninguno.", alCerrar = { hoja = null },
        ) {
            ui.aliases.forEach { a -> FilaDeLista(a.clave, a.nombre, "Quitar el nombre de ${a.nombre}") { vm.quitarAlias(a.clave) } }
        }
        Hoja.BorrarUbicaciones -> AlertDialog(
            onDismissRequest = { hoja = null }, containerColor = c.superficie,
            title = { Text("Borrar todas las ubicaciones", color = c.tinta) },
            text = { Text("Se borra dónde hiciste cada pago. Tus gastos e ingresos se mantienen.", color = c.tinta) },
            dismissButton = { TextButton({ hoja = null }) { Text("Cancelar", color = c.marcaTinta) } },
            confirmButton = { TextButton({ hoja = null; vm.borrarUbicaciones { alAvisar("Ubicaciones borradas") } }) { Text("Borrar", color = c.presupuestoPasado) } },
        )
        null -> Unit
    }

    when (val d = dialogo) {
        DialogoDeRespaldo.PedirContrasenaParaCrear -> DialogoDeContrasena(creando = true, alCerrar = { dialogo = null }) { contrasena ->
            dialogo = DialogoDeRespaldo.Esperando("Cifrando tu respaldo…")
            ambito.launch {
                try {
                    respaldoListo = vm.crearRespaldo(contrasena)
                    dialogo = null
                    guardarArchivo.launch(nombreDelArchivoDeRespaldo(Clock.System.now()))
                } catch (e: ExcepcionDeRespaldo) {
                    dialogo = null; alAvisar(e.message)
                } catch (e: Exception) {
                    dialogo = null; alAvisar("No se pudo crear el respaldo.")
                }
            }
        }
        is DialogoDeRespaldo.PedirContrasenaParaAbrir -> DialogoDeContrasena(creando = false, alCerrar = { dialogo = null }) { contrasena ->
            dialogo = DialogoDeRespaldo.Esperando("Abriendo tu respaldo…")
            ambito.launch {
                try {
                    val contenido = vm.abrirRespaldo(d.datos, contrasena)
                    dialogo = DialogoDeRespaldo.ConfirmarRestauracion(contenido, vm.cuantosSonNuevos(contenido))
                } catch (e: ExcepcionDeRespaldo) {
                    dialogo = null; alAvisar(e.message)
                } catch (e: Exception) {
                    dialogo = null; alAvisar("No se pudo abrir el respaldo.")
                }
            }
        }
        is DialogoDeRespaldo.Esperando -> AlertDialog(
            onDismissRequest = {}, containerColor = c.superficie,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = c.marcaTinta, strokeWidth = 3.dp)
                    Spacer(Modifier.width(20.dp))
                    Text(d.mensaje, color = c.tinta)
                }
            },
            confirmButton = {},
        )
        is DialogoDeRespaldo.ConfirmarRestauracion -> AlertDialog(
            onDismissRequest = { dialogo = null }, containerColor = c.superficie,
            title = { Text("Restaurar respaldo", color = c.tinta) },
            text = { Text(textoDeRestauracion(d.contenido, d.nuevos), color = c.tinta) },
            dismissButton = { TextButton({ dialogo = null }) { Text("Cancelar", color = c.marcaTinta) } },
            confirmButton = {
                TextButton({
                    dialogo = null
                    ambito.launch { alAvisar(mensajeDeRestauracion(vm.restaurar(d.contenido))) }
                }) { Text("Restaurar", color = c.marcaTinta) }
            },
        )
        null -> Unit
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelMedium, color = MiGastoTheme.colores.apagado, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))
}

@Composable
private fun Caja(contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(MiGastoTheme.colores.superficie)) { contenido() }
}

@Composable
private fun Separador() = Box(Modifier.fillMaxWidth().height(1.dp).background(MiGastoTheme.colores.linea))

@Composable
private fun FilaDeAjuste(
    icono: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    textoFinal: String? = null,
    interruptor: Boolean? = null,
    alCambiar: (() -> Unit)? = null,
    alTocar: (() -> Unit)? = null,
    estado: Pair<Boolean, String>? = null,
) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().then(
            when {
                alTocar != null -> Modifier.clickable(onClick = alTocar)
                interruptor != null && alCambiar != null -> Modifier.clickable(onClick = alCambiar)
                else -> Modifier
            },
        ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = c.apagado, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = c.tinta)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = c.apagado)
        }
        textoFinal?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(end = 4.dp)) }
        estado?.let { (activo, texto) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                if (activo) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.presupuestoBien, modifier = Modifier.size(16.dp).padding(end = 0.dp))
                Text(
                    if (activo) " $texto" else texto,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = if (activo) c.presupuestoBien else c.marcaTinta,
                )
            }
        }
        if (interruptor != null) Interruptor(interruptor, titulo, alCambiar ?: {})
        if (alTocar != null && textoFinal == null && estado == null) Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.apagado, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilaDeProveedor(nombre: String, clave: String, activo: Boolean, alCambiar: () -> Unit) {
    val c = MiGastoTheme.colores
    val color = when (clave) { "yape" -> c.yape; "plin" -> c.plin; else -> c.googlePay }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alCambiar).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(14.dp))
        Text("Registrar pagos de $nombre", style = MaterialTheme.typography.titleSmall, color = c.tinta, modifier = Modifier.weight(1f))
        Interruptor(activo, "Registrar pagos de $nombre", alCambiar)
    }
}

@Composable
private fun Interruptor(activo: Boolean, descripcion: String, alCambiar: () -> Unit) {
    val c = MiGastoTheme.colores
    Switch(
        checked = activo, onCheckedChange = { alCambiar() },
        modifier = Modifier.semantics { contentDescription = descripcion },
        colors = SwitchDefaults.colors(
            checkedThumbColor = ColoresMiGasto.VerdeNeon, checkedTrackColor = c.tinta, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = c.apagado, uncheckedTrackColor = c.linea, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun DialogoDePresupuesto(actual: Double, alCerrar: () -> Unit, alGuardar: (Double) -> Unit) {
    val c = MiGastoTheme.colores
    var texto by remember { mutableStateOf(presupuestoParaEditar(actual)) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = c.superficie,
        title = { Text("Presupuesto de gastos", color = c.tinta) },
        text = {
            OutlinedTextField(
                texto, { texto = it }, prefix = { Text("S/ ") }, placeholder = { Text("Monto del mes") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        dismissButton = {
            Row {
                if (actual > 0) TextButton({ alGuardar(0.0); alCerrar() }) { Text("Quitar", color = c.marcaTinta) }
                TextButton(alCerrar) { Text("Cancelar", color = c.marcaTinta) }
            }
        },
        confirmButton = { TextButton({ parsearPresupuesto(texto)?.let { alGuardar(it); alCerrar() } }) { Text("Guardar", color = c.marcaTinta) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeLista(
    titulo: String,
    explicacion: String,
    vacio: String,
    alCerrar: () -> Unit,
    pie: Pair<String, () -> Unit>? = null,
    filas: @Composable () -> Unit,
) {
    val c = MiGastoTheme.colores
    ModalBottomSheet(onDismissRequest = alCerrar, containerColor = c.superficie) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 18.dp).navigationBarsPadding()) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = c.tinta)
            Text(explicacion, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            filas()
            pie?.let { (texto, accion) -> TextButton(accion) { Text(texto, color = c.marcaTinta) } }
        }
    }
}

@Composable
private fun FilaDeLista(titulo: String, subtitulo: String, descripcionDeBorrar: String, icono: ImageVector? = null, alBorrar: () -> Unit) {
    val c = MiGastoTheme.colores
    Row(Modifier.fillMaxWidth().height(Tamano.toqueMinimo + 8.dp), verticalAlignment = Alignment.CenterVertically) {
        icono?.let { Icon(it, contentDescription = null, tint = c.apagado, modifier = Modifier.padding(end = 12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = c.tinta)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = c.apagado)
        }
        Box(
            Modifier.size(Tamano.botonDeIcono).clip(CircleShape).clickable(onClick = alBorrar).semantics { contentDescription = descripcionDeBorrar },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.presupuestoPasado) }
    }
}

@Composable
private fun DialogoDeContrasena(creando: Boolean, alCerrar: () -> Unit, alAceptar: (String) -> Unit) {
    val c = MiGastoTheme.colores
    var primera by remember { mutableStateOf("") }
    var segunda by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val transformacion: VisualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = c.superficie,
        title = { Text("Contraseña del respaldo", color = c.tinta) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creando) {
                    Text(
                        "Elige una contraseña que recuerdes. MiGasto no la guarda: si la olvidas, nadie podrá abrir el respaldo.",
                        style = MaterialTheme.typography.bodySmall, color = c.apagado,
                    )
                }
                OutlinedTextField(
                    primera, { primera = it; error = null }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = transformacion,
                    trailingIcon = {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (visible) "Ocultar" else "Mostrar",
                            modifier = Modifier.clickable { visible = !visible },
                        )
                    },
                )
                if (creando) OutlinedTextField(segunda, { segunda = it; error = null }, label = { Text("Repite la contraseña") }, singleLine = true, visualTransformation = transformacion)
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.presupuestoPasado) }
            }
        },
        dismissButton = { TextButton(alCerrar) { Text("Cancelar", color = c.marcaTinta) } },
        confirmButton = {
            TextButton({
                val problema = validarContrasenaDeRespaldo(primera, segunda, creando)
                if (problema != null) error = problema else alAceptar(primera)
            }) { Text(if (creando) "Crear respaldo" else "Abrir", color = c.marcaTinta) }
        },
    )
}
