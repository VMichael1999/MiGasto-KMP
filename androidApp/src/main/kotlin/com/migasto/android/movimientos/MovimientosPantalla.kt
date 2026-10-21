package com.migasto.android.movimientos

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.resumen.BotonDeIcono
import com.migasto.android.resumen.FilaDelMovimiento
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.domain.FiltroDeTipo

@Composable
fun MovimientosPantalla(abrirBusqueda: Boolean = false, alAbrir: (String) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: MovimientosViewModel = viewModel(
        factory = viewModelFactory { initializer { MovimientosViewModel(app.base, app.montosOcultos) } },
    )
    val ui by vm.estado.collectAsStateWithLifecycle()
    ui?.let { MovimientosContenido(it, abrirBusqueda, vm::elegirTipo, vm::buscar, vm::limpiar, alAbrir) }
}

@Composable
fun MovimientosContenido(
    ui: MovimientosUi,
    abrirBusqueda: Boolean,
    alElegirTipo: (FiltroDeTipo) -> Unit,
    alBuscar: (String) -> Unit,
    alLimpiar: () -> Unit,
    alAbrir: (String) -> Unit,
) {
    val c = MiGastoTheme.colores
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
