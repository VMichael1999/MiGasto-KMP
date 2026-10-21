package com.migasto.android.reportes

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.resumen.OCULTO
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano
import com.migasto.domain.FilaDeReporte
import com.migasto.domain.ModoDeReporte
import com.migasto.domain.OpcionDeModo
import com.migasto.domain.Reporte
import com.migasto.domain.TextosDeReportes
import com.migasto.domain.formatearMonto
import com.migasto.domain.formatearSoles
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ReportesPantalla() {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: ReportesViewModel = viewModel(factory = viewModelFactory { initializer { ReportesViewModel(app.base, app.montosOcultos) } })
    val ui by vm.estado.collectAsStateWithLifecycle()
    ui?.let { ReportesContenido(it, vm) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportesContenido(ui: ReporteUi, vm: ReportesViewModel) {
    val c = MiGastoTheme.colores
    val r = ui.reporte
    var eligiendoRango by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl).padding(top = 6.dp, bottom = 18.dp)) {
        Text(TextosDeReportes.TITULO, style = MaterialTheme.typography.headlineMedium, color = c.tinta)
        Spacer(Modifier.height(14.dp))
        SelectorDeModo(ui.opcionesDeModo, ui.seleccion.modo, vm::elegirModo)
        Spacer(Modifier.height(10.dp))
        BarraDelPeriodo(ui, r, vm::desplazar) { eligiendoRango = true }
        Spacer(Modifier.height(14.dp))
        Saldo(r, ui.oculto)
        Spacer(Modifier.height(14.dp))
        Leyenda()
        Spacer(Modifier.height(8.dp))
        Grafico(r)
        Spacer(Modifier.height(14.dp))
        Tabla(r.tituloDeIngresos, r.textoSinIngresos, r.ingresosPorCategoria, ui.oculto)
        Spacer(Modifier.height(14.dp))
        Tabla(r.tituloDeGastos, r.textoSinGastos, r.gastosPorCategoria, ui.oculto)
    }

    if (eligiendoRango) {
        val estado = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { eligiendoRango = false },
            confirmButton = {
                TextButton(onClick = {
                    val desde = estado.selectedStartDateMillis
                    val hasta = estado.selectedEndDateMillis ?: desde
                    if (desde != null && hasta != null) {
                        vm.elegirRango(fechaDe(desde), fechaDe(hasta))
                        eligiendoRango = false
                    }
                }) { Text(TextosDeReportes.ACEPTAR, color = c.marcaTinta) }
            },
            dismissButton = { TextButton(onClick = { eligiendoRango = false }) { Text(TextosDeReportes.CANCELAR, color = c.marcaTinta) } },
        ) { DateRangePicker(estado, modifier = Modifier.height(500.dp)) }
    }
}

private fun fechaDe(milisegundos: Long) =
    kotlin.time.Instant.fromEpochMilliseconds(milisegundos).toLocalDateTime(TimeZone.UTC).date

@Composable
private fun SelectorDeModo(opciones: List<OpcionDeModo>, modo: ModoDeReporte, alElegir: (ModoDeReporte) -> Unit) {
    val c = MiGastoTheme.colores
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.campo)).background(c.superficie).padding(3.dp)) {
        opciones.map { it.modo to it.texto }.forEach { (m, texto) ->
            val elegido = m == modo
            Box(
                Modifier.weight(1f).height(Tamano.toqueMinimo - 8.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (elegido) c.linea else Color.Transparent).clickable { alElegir(m) }
                    .semantics { contentDescription = texto },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                    color = if (elegido) c.tinta else c.apagado,
                )
            }
        }
    }
}

@Composable
private fun BarraDelPeriodo(ui: ReporteUi, r: Reporte, alDesplazar: (Int) -> Unit, alElegirRango: () -> Unit) {
    val c = MiGastoTheme.colores
    if (ui.seleccion.modo == ModoDeReporte.Rango) {
        Row(
            Modifier.fillMaxWidth().height(Tamano.toqueMinimo).clip(RoundedCornerShape(Radio.campo)).background(c.superficie)
                .clickable(onClick = alElegirRango).padding(horizontal = 14.dp)
                .semantics { contentDescription = "Elegir rango de fechas. ${r.etiquetaDelPeriodo}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(r.etiquetaDelPeriodo, style = MaterialTheme.typography.bodyLarge, color = c.tinta, modifier = Modifier.weight(1f))
            Text(TextosDeReportes.CAMBIAR, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.apagado)
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.apagado, modifier = Modifier.size(18.dp))
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Flecha(Icons.Outlined.KeyboardArrowLeft, TextosDeReportes.PERIODO_ANTERIOR, true) { alDesplazar(-1) }
            Text(r.etiquetaDelPeriodo, style = MaterialTheme.typography.titleMedium, color = c.tinta)
            Flecha(Icons.Outlined.KeyboardArrowRight, TextosDeReportes.PERIODO_SIGUIENTE, ui.puedeAvanzar) { alDesplazar(1) }
        }
    }
}

@Composable
private fun Flecha(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, activa: Boolean, alTocar: () -> Unit) {
    Box(
        Modifier.size(Tamano.toqueMinimo).alpha(if (activa) 1f else 0.35f).clip(RoundedCornerShape(Radio.campo))
            .clickable(enabled = activa, onClick = alTocar).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) { Icon(icono, contentDescription = null, tint = MiGastoTheme.colores.tinta) }
}

@Composable
private fun Saldo(r: Reporte, oculto: Boolean) {
    val c = MiGastoTheme.colores
    Column {
        Text(r.textoDelSaldo, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = c.apagado)
        Spacer(Modifier.height(4.dp))
        val negativo = r.saldoNegativo && !oculto
        Row(verticalAlignment = Alignment.Bottom) {
            if (oculto) {
                Text(OCULTO, style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp), color = c.tinta)
            } else {
                Text(
                    if (negativo) "− S/" else "S/", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (negativo) c.presupuestoPasado else c.apagado, modifier = Modifier.padding(end = 6.dp, bottom = 6.dp),
                )
                Text(
                    formatearMonto(kotlin.math.abs(r.saldo)),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp, fontFeatureSettings = CifrasTabulares),
                    color = if (negativo) c.presupuestoPasado else c.tinta,
                )
            }
        }
        r.comparacion?.let {
            Spacer(Modifier.height(4.dp))
            Text(it.texto(if (oculto) "S/ $OCULTO" else formatearSoles(kotlin.math.abs(it.diferencia))), style = MaterialTheme.typography.bodySmall, color = c.apagado)
        }
    }
}

@Composable
private fun Leyenda() {
    val c = MiGastoTheme.colores
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(c.ingreso to TextosDeReportes.INGRESOS, c.barraDeGastos to TextosDeReportes.GASTOS).forEach { (color, texto) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(6.dp))
                Text(texto, style = MaterialTheme.typography.bodySmall, color = c.apagado)
            }
        }
    }
}

@Composable
private fun Grafico(r: Reporte) {
    // Con otro número de barras (Mes, Año, Rango) se arma un gráfico nuevo: así el eje nunca queda
    // con etiquetas de un modo y datos de otro.
    androidx.compose.runtime.key(r.cubos.size) { GraficoDeBarras(r) }
}

@Composable
private fun GraficoDeBarras(r: Reporte) {
    val c = MiGastoTheme.colores
    val productor = remember { CartesianChartModelProducer() }
    LaunchedEffect(r.cubos) {
        productor.runTransaction {
            columnModel {
                series(r.cubos.map { it.ingresos })
                series(r.cubos.map { it.gastos })
            }
        }
    }
    val ancho = if (r.cubos.size > 6) 7.dp else 14.dp
    val ingresos = rememberLineComponent(fill = Fill(c.ingreso), thickness = ancho)
    val gastos = rememberLineComponent(fill = Fill(c.barraDeGastos), thickness = ancho)
    val etiquetas = r.cubos.map { it.etiqueta }
    Box(Modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = r.descripcionDelGrafico }) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(ingresos, gastos),
                    rangeProvider = remember(r.topDelEje) { CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = r.topDelEje) },
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = remember { CartesianValueFormatter.decimal(decimalCount = 0, thousandsSeparator = ",") },
                    itemPlacer = remember(r.topDelEje) { VerticalAxis.ItemPlacer.step(step = { r.topDelEje / 4 }) },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    guideline = null,
                    valueFormatter = remember(etiquetas) { CartesianValueFormatter { _, valor, _ -> etiquetas.getOrElse(valor.toInt()) { valor.toInt().toString() } } },
                ),
            ),
            modelProducer = productor,
            // Todas las barras caben en pantalla: sin desplazamiento ni zoom.
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun Tabla(titulo: String, vacio: String, filas: List<FilaDeReporte>, oculto: Boolean) {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = c.tinta)
        Spacer(Modifier.height(4.dp))
        if (filas.isEmpty()) Text(vacio, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(vertical = 8.dp))
        filas.forEachIndexed { i, fila ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.linea))
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fila.etiqueta, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp), color = c.tinta, modifier = Modifier.weight(1f))
                Text(
                    if (oculto) "S/ $OCULTO" else formatearSoles(fila.monto),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = CifrasTabulares),
                    color = c.tinta,
                )
            }
        }
    }
}
