package com.migasto.android.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.resumen.BotonDeIcono
import com.migasto.android.resumen.FilaDelMovimiento
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.OpcionesDeFiltro
import com.migasto.domain.PaymentSource
import com.migasto.domain.etiqueta
import com.migasto.domain.rangoDeLaHoja

@Composable
fun MovimientosPantalla(abrirBusqueda: Boolean = false, alAbrir: (String) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: MovimientosViewModel = viewModel(
        factory = viewModelFactory { initializer { MovimientosViewModel(app.base, app.montosOcultos) } },
    )
    val ui by vm.estado.collectAsStateWithLifecycle()
    ui?.let { MovimientosContenido(it, abrirBusqueda, vm::elegirTipo, vm::buscar, vm::limpiar, alAbrir, vm::aplicarFiltros, vm::limpiarFiltrosDeLaHoja) }
}

@Composable
fun MovimientosContenido(
    ui: MovimientosUi,
    abrirBusqueda: Boolean,
    alElegirTipo: (FiltroDeTipo) -> Unit,
    alBuscar: (String) -> Unit,
    alLimpiar: () -> Unit,
    alAbrir: (String) -> Unit,
    alAplicarFiltros: (String?, PaymentSource?, ClosedFloatingPointRange<Double>?) -> Unit = { _, _, _ -> },
    alLimpiarLaHoja: () -> Unit = {},
) {
    val c = MiGastoTheme.colores
    var hojaAbierta by remember { mutableStateOf(false) }
    var buscando by rememberSaveable(abrirBusqueda) { mutableStateOf(abrirBusqueda || ui.busqueda.isNotEmpty()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Espacio.xl, end = 10.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Movimientos", style = MaterialTheme.typography.headlineMedium, color = c.tinta, modifier = Modifier.weight(1f))
            BotonDeIcono(Icons.Outlined.Search, if (buscando) "Cerrar la búsqueda" else "Buscar movimientos") {
                buscando = !buscando
                if (!buscando) alBuscar("")
            }
            Box {
                BotonDeIcono(Icons.Outlined.FilterList, "Filtrar movimientos") { hojaAbierta = true }
                val activos = ui.filtros.cantidadDeFiltrosActivos
                if (activos > 0) {
                    Text(
                        activos.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = ColoresMiGasto.TintaSobreVerde,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(16.dp)
                            .background(ColoresMiGasto.VerdeNeon, CircleShape).wrapContentSize(Alignment.Center)
                            .semantics { contentDescription = "$activos filtros puestos" },
                    )
                }
            }
        }
        if (hojaAbierta) {
            HojaDeFiltros(ui, alCerrar = { hojaAbierta = false }, alAplicarFiltros, alLimpiarLaHoja)
        }
        if (buscando) CampoDeBusqueda(ui.busqueda, alBuscar)
        Chips(ui, alElegirTipo)
        Spacer(Modifier.height(6.dp))
        if (ui.grupos.isEmpty()) {
            Vacio(ui, alLimpiar)
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Espacio.xl, vertical = 8.dp)) {
                items(ui.grupos, key = { it.etiqueta }) { grupo ->
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                grupo.etiqueta, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = c.tinta, modifier = Modifier.weight(1f),
                            )
                            grupo.totalDeGastos?.let {
                                Text(if (ui.oculto) "•••• en gastos" else it, style = MaterialTheme.typography.bodySmall, color = c.apagado)
                            }
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.caja)).background(c.superficie)) {
                            grupo.filas.forEachIndexed { i, fila ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.linea))
                                FilaDelMovimiento(fila, ui.oculto) { alAbrir(fila.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoDeBusqueda(texto: String, alCambiar: (String) -> Unit) {
    val c = MiGastoTheme.colores
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Espacio.xl).padding(bottom = 10.dp).height(46.dp)
            .clip(RoundedCornerShape(Radio.campo)).background(c.superficie).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = c.apagado, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (texto.isEmpty()) Text("Buscar por nombre", style = MaterialTheme.typography.bodyLarge, color = c.apagado)
            BasicTextField(
                value = texto, onValueChange = alCambiar, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tinta),
                cursorBrush = SolidColor(c.marcaTinta),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(),
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
            )
        }
        if (texto.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close, contentDescription = "Borrar el texto", tint = c.apagado,
                modifier = Modifier.size(20.dp).clickable { alCambiar("") },
            )
        }
    }
}

@Composable
private fun Chips(ui: MovimientosUi, alElegir: (FiltroDeTipo) -> Unit) {
    val opciones = listOf(
        FiltroDeTipo.Todo to "Todo",
        FiltroDeTipo.Gastos to "Gastos",
        FiltroDeTipo.Ingresos to "Ingresos",
        FiltroDeTipo.PorConfirmar to "Por confirmar · ${ui.cantidadPorConfirmar}",
    )
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Espacio.xl),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(opciones) { (tipo, texto) -> Chip(texto, ui.tipo == tipo, tipo == FiltroDeTipo.PorConfirmar) { alElegir(tipo) } }
    }
}

@Composable
private fun Chip(texto: String, elegido: Boolean, esPorConfirmar: Boolean, alTocar: () -> Unit) {
    val c = MiGastoTheme.colores
    val (fondo, borde, tinta) = when {
        elegido -> Triple(c.tinta, c.tinta, c.fondo)
        esPorConfirmar -> Triple(c.porConfirmarSuave, c.porConfirmar, c.porConfirmar)
        else -> Triple(c.superficie, c.linea, c.tinta)
    }
    Box(
        Modifier.height(34.dp).clip(CircleShape).background(fondo).border(1.dp, borde, CircleShape)
            .clickable(onClick = alTocar).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = tinta, maxLines = 1)
    }
}

@Composable
private fun Vacio(ui: MovimientosUi, alLimpiar: () -> Unit) {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxSize().padding(Espacio.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            if (ui.hayFiltro) "No hay movimientos con esos filtros." else "Todavía no hay movimientos.",
            style = MaterialTheme.typography.bodyLarge, color = c.apagado,
        )
        if (ui.hayFiltro) {
            Text(
                "Quitar filtros", style = MaterialTheme.typography.labelLarge, color = c.marcaTinta,
                modifier = Modifier.clickable(onClick = alLimpiar).padding(12.dp).semantics { contentDescription = "Quitar filtros" },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaDeFiltros(
    ui: MovimientosUi,
    alCerrar: () -> Unit,
    alAplicar: (String?, PaymentSource?, ClosedFloatingPointRange<Double>?) -> Unit,
    alLimpiar: () -> Unit,
) {
    val c = MiGastoTheme.colores
    var categoria by remember { mutableStateOf(ui.filtros.categoria) }
    var fuente by remember { mutableStateOf(ui.filtros.fuente) }
    var rango by remember { mutableStateOf(ui.filtros.rangoDeLaHoja) }
    val rangoVisible = rango ?: (OpcionesDeFiltro.MONTO_MINIMO..OpcionesDeFiltro.MONTO_MAXIMO)

    // Abre completa: a media altura, en pantallas bajas, dejaba los últimos filtros fuera de la vista.
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.superficie,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 18.dp).navigationBarsPadding(),
        ) {
            Text("Filtrar movimientos", style = MaterialTheme.typography.titleLarge, color = c.tinta)
            Seccion("Categoría")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Todas", categoria == null, false) { categoria = null }
                OpcionesDeFiltro.categorias(ui.tipo).forEach { cat -> Chip(cat.etiqueta, categoria == cat.clave, false) { categoria = cat.clave } }
                OpcionesDeFiltro.propias(ui.tipo, ui.propias).forEach { p -> Chip(p.nombre, categoria == p.id, false) { categoria = p.id } }
            }
            Seccion("Fuente")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Todas", fuente == null, false) { fuente = null }
                OpcionesDeFiltro.fuentes.forEach { f -> Chip(f.etiqueta, fuente == f, false) { fuente = f } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Seccion("Rango de monto") }
                Text(
                    OpcionesDeFiltro.textoDelRango(rangoVisible.start, rangoVisible.endInclusive),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = CifrasTabulares), color = c.apagado,
                )
            }
            RangeSlider(
                value = rangoVisible.start.toFloat()..rangoVisible.endInclusive.toFloat(),
                onValueChange = { rango = it.start.toDouble()..it.endInclusive.toDouble() },
                valueRange = OpcionesDeFiltro.MONTO_MINIMO.toFloat()..OpcionesDeFiltro.MONTO_MAXIMO.toFloat(),
                steps = OpcionesDeFiltro.PASOS_INTERMEDIOS,
                colors = SliderDefaults.colors(
                    thumbColor = ColoresMiGasto.VerdeNeon, activeTrackColor = ColoresMiGasto.VerdeNeon, inactiveTrackColor = c.linea,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton)).background(ColoresMiGasto.VerdeNeon)
                    .clickable { alAplicar(categoria, fuente, rango); alCerrar() },
                contentAlignment = Alignment.Center,
            ) { Text("Aplicar", style = MaterialTheme.typography.titleLarge, color = ColoresMiGasto.TintaSobreVerde) }
            TextButton(onClick = { alLimpiar(); alCerrar() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Limpiar filtros", color = c.marcaTinta)
            }
        }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        texto, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MiGastoTheme.colores.apagado,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}
