package com.migasto.android.donde

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.migasto.android.MiGastoApp
import com.migasto.android.mapa.MapaDeZonas
import com.migasto.android.resumen.BotonDeIcono
import com.migasto.android.resumen.OCULTO
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.ubicacion.hayClaveDeMapas
import com.migasto.domain.PeriodoDeUbicaciones
import com.migasto.domain.TextosDeDondePagaste
import com.migasto.domain.ZonaDePagos

/** «Dónde pagaste»: los pagos con ubicación en un mapa y, debajo, agrupados por zona (port de WhereScreen). */
@Composable
fun DondePagastePantalla(alCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as MiGastoApp
    val vm: DondePagasteViewModel = viewModel(factory = viewModelFactory { initializer { DondePagasteViewModel(app.base, app.montosOcultos) } })
    val ui by vm.estado.collectAsStateWithLifecycle()
    val hayMapa = remember { hayClaveDeMapas(contexto) }
    BackHandler(onBack = alCerrar)
    val actual = ui
    Box(Modifier.fillMaxSize().background(MiGastoTheme.colores.fondo)) {
        if (actual != null) {
            DondePagasteContenido(
                ui = actual,
                alElegirPeriodo = vm::elegirPeriodo,
                alVolver = alCerrar,
                mapa = { zonas, modificador ->
                    if (hayMapa) MapaDeZonas(zonas, modificador) else SinMapa(modificador)
                },
            )
        }
    }
}

@Composable
fun SinMapa(modifier: Modifier = Modifier) {
    val c = MiGastoTheme.colores
    Box(modifier.background(c.superficie), contentAlignment = Alignment.Center) {
        Text(
            TextosDeDondePagaste.SIN_CLAVE, style = MaterialTheme.typography.bodySmall, color = c.apagado,
            textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp),
        )
    }
}

/** La pantalla sin ViewModel ni Google Maps, para poder probarla; el mapa se recibe de afuera. */
@Composable
fun DondePagasteContenido(
    ui: DondePagasteUi,
    alElegirPeriodo: (PeriodoDeUbicaciones) -> Unit,
    alVolver: () -> Unit,
    mapa: @Composable (List<ZonaDePagos>, Modifier) -> Unit,
) {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(5f).fillMaxWidth()) {
            mapa(ui.zonas, Modifier.fillMaxSize())
            Box(Modifier.statusBarsPadding().padding(14.dp).clip(RoundedCornerShape(13.dp)).background(c.superficie)) {
                BotonDeIcono(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", alVolver)
            }
        }
        Column(
            Modifier.weight(4f).fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.superficie)
                .padding(start = 18.dp, end = 18.dp, top = 14.dp).navigationBarsPadding(),
        ) {
            // El título y los tres botones no caben en una fila en pantallas angostas: cada uno va en la suya.
            Text(TextosDeDondePagaste.TITULO, style = MaterialTheme.typography.headlineSmall, color = c.tinta)
            Spacer(Modifier.size(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PeriodoDeUbicaciones.entries.forEachIndexed { i, periodo ->
                    SegmentedButton(
                        selected = ui.periodo == periodo,
                        onClick = { alElegirPeriodo(periodo) },
                        shape = SegmentedButtonDefaults.itemShape(i, PeriodoDeUbicaciones.entries.size),
                        icon = {},
                    ) { Text(periodo.etiqueta, style = MaterialTheme.typography.labelMedium) }
                }
            }
            Spacer(Modifier.size(8.dp))
            if (ui.zonas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(TextosDeDondePagaste.VACIO, style = MaterialTheme.typography.bodySmall, color = c.apagado, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(ui.zonas) { i, zona ->
                        if (i > 0) Box(Modifier.fillMaxWidth().size(1.dp).background(c.linea))
                        FilaDeZona(zona, ui)
                    }
                    item {
                        Text(TextosDeDondePagaste.PIE, style = MaterialTheme.typography.bodySmall, color = c.apagado, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDeZona(zona: ZonaDePagos, ui: DondePagasteUi) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp).clearAndSetSemantics { contentDescription = zona.descripcion(ui.alias, ui.oculto) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.tinta), contentAlignment = Alignment.Center) {
            Text("${zona.movimientos.size}", style = MaterialTheme.typography.titleSmall, color = c.superficie)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(zona.nombre, style = MaterialTheme.typography.titleSmall, color = c.tinta)
            Text(zona.subtitulo(ui.alias), style = MaterialTheme.typography.bodySmall, color = c.apagado)
        }
        Text(
            if (ui.oculto) OCULTO else com.migasto.domain.formatearSoles(zona.totalGastos),
            style = MaterialTheme.typography.titleSmall, color = c.tinta,
        )
    }
}
