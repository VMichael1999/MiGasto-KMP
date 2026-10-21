package com.migasto.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.migasto.android.movimientos.MovimientosPantalla
import com.migasto.android.ajustes.AjustesPantalla
import com.migasto.android.detalle.DetallePantalla
import com.migasto.android.nuevo.NuevoMovimientoPantalla
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.migasto.android.resumen.ResumenPantalla
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano

private enum class Pestana(val titulo: String, val icono: ImageVector) {
    Resumen("Resumen", Icons.Outlined.Home),
    Movimientos("Movimientos", Icons.Outlined.FormatListBulleted),
    Reportes("Reportes", Icons.Outlined.BarChart),
    Ajustes("Ajustes", Icons.Outlined.Settings),
}

/** Barra inferior de Material 3 con «agregar» al centro, como en el diseño de Android. */
@Composable
fun MiGastoNavegacion() {
    var actual by rememberSaveable { mutableStateOf(Pestana.Resumen.name) }
    var abrirBusqueda by rememberSaveable { mutableStateOf(false) }
    var sesionNuevo by rememberSaveable { mutableStateOf(0) }
    var nuevoAbierto by rememberSaveable { mutableStateOf(false) }
    var detalleId by rememberSaveable { mutableStateOf<String?>(null) }
    val pestana = Pestana.valueOf(actual)
    val c = MiGastoTheme.colores
    val avisos = remember { SnackbarHostState() }
    val ambito = rememberCoroutineScope()

    BackHandler(enabled = nuevoAbierto) { nuevoAbierto = false }
    BackHandler(enabled = detalleId != null && !nuevoAbierto) { detalleId = null }
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.migasto.android.MiGastoApp

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = c.fondo,
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            BarraInferior(pestana, alAgregar = { sesionNuevo++; nuevoAbierto = true }) { actual = it.name; abrirBusqueda = false }
        },
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when (pestana) {
                Pestana.Resumen -> ResumenPantalla(
                    alVerTodos = { abrirBusqueda = false; actual = Pestana.Movimientos.name },
                    alBuscar = { abrirBusqueda = true; actual = Pestana.Movimientos.name },
                    alAbrir = { detalleId = it },
                )
                Pestana.Movimientos -> MovimientosPantalla(abrirBusqueda) { detalleId = it }
                Pestana.Ajustes -> AjustesPantalla(alAvisar = { mensaje -> ambito.launch { avisos.showSnackbar(mensaje) } })
                else -> EnConstruccion(pestana.titulo)
            }
        }
    }
    detalleId?.let { id ->
        DetallePantalla(
            id = id,
            alCerrar = { detalleId = null },
            alAvisar = { mensaje -> ambito.launch { avisos.showSnackbar(mensaje) } },
            alEliminado = { mensaje, original ->
                detalleId = null
                ambito.launch {
                    val resultado = avisos.showSnackbar(mensaje, actionLabel = "Deshacer", duration = SnackbarDuration.Long)
                    if (resultado == SnackbarResult.ActionPerformed) {
                        withContext(Dispatchers.IO) { app.registro.await().restaurar(original) }
                    }
                }
            },
        )
    }
    if (nuevoAbierto) {
        NuevoMovimientoPantalla(
            sesion = sesionNuevo,
            alCerrar = { nuevoAbierto = false },
            alGuardar = { mensaje ->
                nuevoAbierto = false
                ambito.launch { avisos.showSnackbar(mensaje) }
            },
        )
    }
    }
}

@Composable
private fun EnConstruccion(titulo: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$titulo: todavía no está hecha", style = MaterialTheme.typography.bodyLarge, color = MiGastoTheme.colores.apagado)
    }
}

@Composable
private fun BarraInferior(actual: Pestana, alAgregar: () -> Unit, alElegir: (Pestana) -> Unit) {
    val c = MiGastoTheme.colores
    Column(Modifier.fillMaxWidth().background(c.superficie).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().size(width = 0.dp, height = 1.dp).background(c.linea))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Elemento(Pestana.Resumen, actual, alElegir)
            Elemento(Pestana.Movimientos, actual, alElegir)
            BotonAgregar(alAgregar)
            Elemento(Pestana.Reportes, actual, alElegir)
            Elemento(Pestana.Ajustes, actual, alElegir)
        }
    }
}

@Composable
private fun RowScope.Elemento(pestana: Pestana, actual: Pestana, alElegir: (Pestana) -> Unit) {
    val c = MiGastoTheme.colores
    val elegido = pestana == actual
    Column(
        Modifier
            .weight(1f)
            .clickable(role = Role.Tab) { alElegir(pestana) }
            .semantics { contentDescription = pestana.titulo }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(pestana.icono, contentDescription = null, tint = if (elegido) c.marcaTinta else c.apagado, modifier = Modifier.size(24.dp))
        Text(
            pestana.titulo,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (elegido) c.tinta else c.apagado,
                fontWeight = if (elegido) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun BotonAgregar(alTocar: () -> Unit) {
    Box(
        Modifier
            .size(Tamano.agregar)
            .background(ColoresMiGasto.VerdeNeon, RoundedCornerShape(Radio.agregar))
            .clickable(role = Role.Button, onClick = alTocar)
            .semantics { contentDescription = "Agregar movimiento" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = ColoresMiGasto.TintaSobreVerde, modifier = Modifier.size(28.dp))
    }
}
