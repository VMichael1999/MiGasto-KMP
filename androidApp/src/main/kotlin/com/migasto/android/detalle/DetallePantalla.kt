package com.migasto.android.detalle

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.mapa.MapaChico
import com.migasto.android.nuevo.HojaDeCategoriaNueva
import com.migasto.android.resumen.BotonDeIcono
import com.migasto.android.resumen.OCULTO
import com.migasto.android.resumen.colorDeLaFuente
import com.migasto.android.resumen.iconoDeCategoria
import com.migasto.android.resumen.iconoPropio
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano
import com.migasto.android.ubicacion.abrirAjustesDeLaApp
import com.migasto.android.ubicacion.hayClaveDeMapas
import com.migasto.android.ubicacion.rememberAgregarUbicacion
import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.DetalleDelMovimiento
import com.migasto.domain.MotivoDeFallo
import com.migasto.domain.Movimiento
import com.migasto.domain.TextosDeUbicacion
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.etiqueta
import com.migasto.domain.formatearMonto
import com.migasto.domain.mensajeDeFallo
import com.migasto.domain.sugerenciaDeAlias

/**
 * @param alEliminado se llama con el aviso («Gasto de "X" eliminado») y el movimiento original, para «Deshacer».
 * @param alAvisar muestra un aviso corto (por ejemplo «Gasto guardado»).
 */
@Composable
fun DetallePantalla(id: String, alCerrar: () -> Unit, alAvisar: (String) -> Unit, alEliminado: (String, Movimiento) -> Unit) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: DetalleViewModel = viewModel(
        key = "detalle-$id",
        factory = viewModelFactory { initializer { DetalleViewModel(id, app.base, app.registro, app.montosOcultos) } },
    )
    val ui by vm.estado.collectAsStateWithLifecycle()
    val oculto by vm.montosOcultos.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MiGastoTheme.colores.fondo).statusBarsPadding().navigationBarsPadding()) {
        when (val u = ui) {
            DetalleUi.Cargando -> Unit
            DetalleUi.NoExiste -> {
                Row(Modifier.padding(Espacio.xl)) { BotonDeIcono(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", alCerrar) }
                Text("Este movimiento ya no existe.", style = MaterialTheme.typography.bodyLarge, color = MiGastoTheme.colores.apagado, modifier = Modifier.padding(horizontal = Espacio.xl))
            }
            is DetalleUi.Listo -> DetalleContenido(u, oculto, vm, alCerrar, alAvisar, alEliminado)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleContenido(
    u: DetalleUi.Listo,
    oculto: Boolean,
    vm: DetalleViewModel,
    alCerrar: () -> Unit,
    alAvisar: (String) -> Unit,
    alEliminado: (String, Movimiento) -> Unit,
) {
    val c = MiGastoTheme.colores
    val d = u.detalle
    var editando by remember { mutableStateOf(false) }
    var cambiandoNombre by remember { mutableStateOf(false) }
    var eligiendoCategoria by remember { mutableStateOf(false) }
    var confirmandoEliminar by remember { mutableStateOf(false) }
    var verTexto by remember { mutableStateOf(false) }
    var ubicacionBloqueada by remember { mutableStateOf(false) }
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val hayMapa = remember { hayClaveDeMapas(contexto) }
    val agregarUbicacion = rememberAgregarUbicacion(
        alCapturar = vm::ponerUbicacion,
        alFallar = { motivo -> if (motivo.ofreceAbrirAjustes) ubicacionBloqueada = true else alAvisar(mensajeDeFallo(motivo)) },
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl).padding(top = 6.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BotonDeIcono(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", alCerrar)
            Text(d.tipoTexto, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { editando = true }) { Text("Editar", style = MaterialTheme.typography.labelLarge, color = c.marcaTinta) }
        }
        Spacer(Modifier.height(14.dp))

        // La boleta
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(c.superficie).padding(16.dp)) {
            Text(d.nombre, style = MaterialTheme.typography.bodySmall, color = c.apagado)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                if (oculto) {
                    Text(OCULTO, style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp), color = if (d.esIngreso) c.ingreso else c.tinta)
                } else {
                    Text(
                        if (d.esIngreso) "+ S/" else "S/", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = c.apagado, modifier = Modifier.padding(end = 6.dp, bottom = 7.dp),
                    )
                    Text(
                        formatearMonto(d.monto),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp, fontFeatureSettings = CifrasTabulares),
                        color = if (d.esIngreso) c.ingreso else c.tinta,
                    )
                }
            }
            if (d.porConfirmar) {
                Text(
                    "Por confirmar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = c.porConfirmar,
                    modifier = Modifier.padding(top = 8.dp).clip(CircleShape).background(c.porConfirmarSuave).padding(horizontal = 9.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(1.dp).drawBehind {
                    drawLine(c.linea, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                },
            )
            Fila("Fecha") { Texto(d.fecha) }
            Fila(d.etiquetaDelNombre, alTocar = { cambiandoNombre = true }) { Texto(d.nombre); Chevron() }
            Fila(d.etiquetaDeLaFuente) {
                Box(Modifier.size(8.dp).background(colorDeLaFuente(d.fuente), CircleShape))
                Spacer(Modifier.width(6.dp))
                Texto(d.fuenteTexto)
            }
            Fila("Categoría", alTocar = { eligiendoCategoria = true }) {
                Icon(d.iconoPropio?.let { iconoPropio(it) } ?: iconoDeCategoria(d.categoria), contentDescription = null, tint = c.tinta, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Texto(d.categoriaEtiqueta)
                Chevron()
            }
            Fila("Registrado") { Texto(d.registrado) }
            d.nota?.let { Fila("Nota") { Texto(it) } }
        }

        if (d.porConfirmar) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton))
                    .background(if (d.esIngreso) c.ingreso else ColoresMiGasto.VerdeNeon)
                    .clickable { vm.confirmar { alAvisar(d.avisoDeGuardado) } },
                contentAlignment = Alignment.Center,
            ) { Text(d.textoDeConfirmar, style = MaterialTheme.typography.titleLarge, color = if (d.esIngreso) c.fondo else ColoresMiGasto.TintaSobreVerde) }
        }

        Spacer(Modifier.height(16.dp))
        if (d.tieneUbicacion) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.tarjeta)).background(c.superficie)) {
                if (hayMapa && d.latitud != null && d.longitud != null) {
                    MapaChico(d.latitud!!, d.longitud!!, Modifier.fillMaxWidth().height(120.dp))
                }
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = c.apagado)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(d.direccion ?: "Ubicación guardada", style = MaterialTheme.typography.titleSmall, color = c.tinta)
                        d.textoDeLaUbicacion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.apagado) }
                    }
                }
            }
            TextButton(onClick = vm::quitarUbicacion) {
                Text("Quitar la ubicación de este ${d.tipoTexto.lowercase()}", style = MaterialTheme.typography.labelLarge, color = c.marcaTinta)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.tarjeta)).background(c.superficie)
                    .clickable(enabled = !agregarUbicacion.capturando, onClick = agregarUbicacion.iniciar)
                    .semantics { contentDescription = "${TextosDeUbicacion.AGREGAR}. ${TextosDeUbicacion.AGREGAR_DETALLE}" }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = c.apagado)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(TextosDeUbicacion.AGREGAR, style = MaterialTheme.typography.titleSmall, color = c.tinta)
                    Text(TextosDeUbicacion.AGREGAR_DETALLE, style = MaterialTheme.typography.bodySmall, color = c.apagado)
                }
                if (agregarUbicacion.capturando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.apagado)
                } else {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.apagado)
                }
            }
        }

        d.textoOriginal?.let { texto ->
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clickable { verTexto = !verTexto }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Texto original", style = MaterialTheme.typography.titleSmall, color = c.tinta, modifier = Modifier.weight(1f))
                Icon(if (verTexto) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = c.apagado)
            }
            if (verTexto) Text(texto, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(bottom = 12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().height(Tamano.toqueMinimo).clickable { confirmandoEliminar = true }.semantics { contentDescription = d.textoDeEliminar },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = c.presupuestoPasado, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(d.textoDeEliminar, style = MaterialTheme.typography.titleSmall, color = c.presupuestoPasado)
        }
    }

    if (ubicacionBloqueada) {
        AlertDialog(
            onDismissRequest = { ubicacionBloqueada = false },
            containerColor = c.superficie,
            text = { Text(mensajeDeFallo(MotivoDeFallo.DenegadaParaSiempre), color = c.tinta) },
            dismissButton = { TextButton({ ubicacionBloqueada = false }) { Text("Cancelar", color = c.marcaTinta) } },
            confirmButton = {
                TextButton({
                    ubicacionBloqueada = false
                    abrirAjustesDeLaApp(contexto)
                }) { Text(TextosDeUbicacion.ABRIR_AJUSTES, color = c.marcaTinta) }
            },
        )
    }
    if (confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = { confirmandoEliminar = false },
            containerColor = c.superficie,
            title = { Text(d.textoDeEliminar, color = c.tinta) },
            text = { Text(d.preguntaDeEliminar, color = c.tinta) },
            dismissButton = { TextButton({ confirmandoEliminar = false }) { Text("Cancelar", color = c.marcaTinta) } },
            confirmButton = {
                TextButton({
                    confirmandoEliminar = false
                    vm.eliminar { original -> alEliminado(d.avisoDeEliminado, original) }
                }) { Text("Eliminar", color = c.presupuestoPasado) }
            },
        )
    }
    if (cambiandoNombre) {
        var texto by remember { mutableStateOf(if (d.nombre != d.nombreOriginal) d.nombre else sugerenciaDeAlias(d.nombreOriginal)) }
        val tieneAlias = d.nombre != d.nombreOriginal
        AlertDialog(
            onDismissRequest = { cambiandoNombre = false },
            containerColor = c.superficie,
            title = { Text("Cambiar nombre", color = c.tinta) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Así verás a \"${d.nombreOriginal}\" en toda la app. Es solo cómo se ve: el pago no cambia.", style = MaterialTheme.typography.bodySmall, color = c.apagado)
                    OutlinedTextField(texto, { texto = it }, label = { Text("Nombre") }, singleLine = true)
                }
            },
            dismissButton = {
                Row {
                    if (tieneAlias) TextButton({ vm.establecerAlias(d.nombreOriginal, ""); cambiandoNombre = false }) { Text("Quitar nombre", color = c.marcaTinta) }
                    TextButton({ cambiandoNombre = false }) { Text("Cancelar", color = c.marcaTinta) }
                }
            },
            confirmButton = { TextButton({ vm.establecerAlias(d.nombreOriginal, texto); cambiandoNombre = false }) { Text("Guardar", color = c.marcaTinta) } },
        )
    }
    if (editando) HojaDeEdicion(d, vm) { editando = false }
    if (eligiendoCategoria) {
        HojaDeCategoria(d, u.tipo, u.propias, vm) { eligiendoCategoria = false }
    }
}

@Composable
private fun Fila(etiqueta: String, alTocar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().then(if (alTocar != null) Modifier.clickable(onClick = alTocar) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp), color = c.apagado)
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { contenido() }
    }
}

@Composable
private fun Texto(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold), color = MiGastoTheme.colores.tinta, textAlign = TextAlign.End)
}

@Composable
private fun Chevron(icono: ImageVector = Icons.Outlined.KeyboardArrowRight) {
    Icon(icono, contentDescription = null, tint = MiGastoTheme.colores.apagado, modifier = Modifier.size(18.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeEdicion(d: DetalleDelMovimiento, vm: DetalleViewModel, alCerrar: () -> Unit) {
    val c = MiGastoTheme.colores
    var monto by remember { mutableStateOf(d.montoParaEditar) }
    var comercio by remember { mutableStateOf(d.nombreOriginal) }
    var nota by remember { mutableStateOf(d.nota ?: "") }
    var error by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = alCerrar, containerColor = c.superficie) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Editar ${d.tipoTexto.lowercase()}", style = MaterialTheme.typography.titleLarge, color = c.tinta)
            OutlinedTextField(monto, { monto = it; error = false }, label = { Text("Monto") }, prefix = { Text("S/ ") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error)
            OutlinedTextField(comercio, { comercio = it; error = false }, label = { Text(if (d.esIngreso) "De quién" else "Comercio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nota, { nota = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (error) Text("Pon un monto mayor que cero y un nombre.", style = MaterialTheme.typography.bodySmall, color = c.presupuestoPasado)
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton)).background(ColoresMiGasto.VerdeNeon)
                    .clickable { vm.editar(monto, comercio, nota) { bien -> if (bien) alCerrar() else error = true } },
                contentAlignment = Alignment.Center,
            ) { Text("Guardar cambios", style = MaterialTheme.typography.titleLarge, color = ColoresMiGasto.TintaSobreVerde) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeCategoria(d: DetalleDelMovimiento, tipo: TipoMovimiento, propias: List<CategoriaPropia>, vm: DetalleViewModel, alCerrar: () -> Unit) {
    val c = MiGastoTheme.colores
    var recordar by remember { mutableStateOf(!d.esIngreso) }
    var creando by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = alCerrar, containerColor = c.superficie) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp).navigationBarsPadding()) {
            Text("Categoría", style = MaterialTheme.typography.titleLarge, color = c.tinta, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            if (!d.esIngreso) {
                Row(Modifier.fillMaxWidth().clickable { recordar = !recordar }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(recordar, { recordar = it })
                    Text("Aplicar a todos los pagos a ${d.nombre}", style = MaterialTheme.typography.bodyMedium, color = c.tinta)
                }
            }
            Categoria.paraTipo(tipo).forEach { cat ->
                OpcionDeCategoria(iconoDeCategoria(cat), cat.etiqueta, d.categoriaPropiaId == null && d.categoria == cat) {
                    vm.cambiarCategoria(cat, recordar); alCerrar()
                }
            }
            propias.filter { it.tipo == tipo }.forEach { p ->
                OpcionDeCategoria(iconoPropio(p.icono), p.nombre, d.categoriaPropiaId == p.id) {
                    vm.cambiarCategoria(p.base, recordar, p.id); alCerrar()
                }
            }
            OpcionDeCategoria(Icons.Outlined.Add, "Nueva categoría", false) { creando = true }
        }
    }
    if (creando) {
        HojaDeCategoriaNueva(alCerrar = { creando = false }) { nombre, icono ->
            vm.crearCategoriaYElegirla(nombre, icono, tipo, recordar)
            creando = false
            alCerrar()
        }
    }
}

@Composable
private fun OpcionDeCategoria(icono: ImageVector, texto: String, elegida: Boolean, alTocar: () -> Unit) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().height(Tamano.toqueMinimo).clickable(onClick = alTocar).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = if (elegida) c.marcaTinta else c.apagado)
        Spacer(Modifier.width(14.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (elegida) FontWeight.SemiBold else FontWeight.Normal), color = if (elegida) c.marcaTinta else c.tinta)
    }
}
