package com.migasto.android.nuevo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.migasto.android.resumen.colorDeLaFuente
import com.migasto.android.resumen.iconoDeCategoria
import com.migasto.android.resumen.iconoPropio
import com.migasto.android.tema.CifrasTabulares
import com.migasto.android.tema.ColoresMiGasto
import com.migasto.android.tema.Espacio
import com.migasto.android.tema.MiGastoTheme
import com.migasto.android.tema.Radio
import com.migasto.android.tema.Tamano
import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.FormularioNuevoMovimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.clavesDeIconosPropios
import com.migasto.domain.etiqueta
import com.migasto.domain.limitarNombreDeCategoria
import com.migasto.domain.formatearHora

@Composable
fun NuevoMovimientoPantalla(sesion: Int, alCerrar: () -> Unit, alGuardar: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as MiGastoApp
    val vm: NuevoMovimientoViewModel = viewModel(
        key = "nuevo-$sesion",
        factory = viewModelFactory { initializer { NuevoMovimientoViewModel(app.base, app.registro) } },
    )
    val f by vm.formulario.collectAsStateWithLifecycle()
    val propias by vm.propias.collectAsStateWithLifecycle()
    val guardando by vm.guardando.collectAsStateWithLifecycle()
    NuevoMovimientoContenido(
        f, propias, guardando, alCerrar, vm::cambiarTipo, vm::escribirConcepto, vm::elegirFuente, vm::elegirCategoria,
        vm::elegirPropia, vm::crearCategoria, vm::presionar,
    ) { vm.guardar { mensaje -> alGuardar(mensaje) } }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NuevoMovimientoContenido(
    f: FormularioNuevoMovimiento,
    propias: List<CategoriaPropia>,
    guardando: Boolean,
    alCerrar: () -> Unit,
    alCambiarTipo: (TipoMovimiento) -> Unit,
    alEscribirConcepto: (String) -> Unit,
    alElegirFuente: (PaymentSource) -> Unit,
    alElegirCategoria: (Categoria) -> Unit,
    alElegirPropia: (CategoriaPropia) -> Unit,
    alCrearCategoria: (String, String) -> Unit,
    alPresionar: (Char) -> Unit,
    alGuardar: () -> Unit,
) {
    val c = MiGastoTheme.colores
    var eligiendoFuente by remember { mutableStateOf(false) }
    var creandoCategoria by remember { mutableStateOf(false) }
    val acento = if (f.esIngreso) c.ingreso else c.tinta
    val propiasDelTipo = propias.filter { it.tipo == f.tipo }

    Column(Modifier.fillMaxSize().background(c.fondo).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cabecera(alCerrar)
            SelectorDeTipo(f.tipo, alCambiarTipo)
            Monto(f, acento)
            CampoDelConcepto(f, alEscribirConcepto)
            CampoDeLaFuente(f) { eligiendoFuente = true }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Categoria.paraTipo(f.tipo).forEach { cat ->
                    ChipDeCategoria(cat.etiqueta, iconoDeCategoria(cat), f.categoriaPropia == null && f.categoria == cat, f.esIngreso) { alElegirCategoria(cat) }
                }
                propiasDelTipo.forEach { p ->
                    ChipDeCategoria(p.nombre, iconoPropio(p.icono), f.categoriaPropia == p.id, f.esIngreso) { alElegirPropia(p) }
                }
                ChipDeCategoria("Nueva", androidx.compose.material.icons.Icons.Outlined.Add, false, f.esIngreso) { creandoCategoria = true }
            }
        }
        Column(Modifier.padding(horizontal = Espacio.xl).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Teclado(alPresionar)
            BotonGuardar(f, guardando, alGuardar)
        }
    }

    if (eligiendoFuente) {
        ModalBottomSheet(onDismissRequest = { eligiendoFuente = false }, containerColor = c.superficie) {
            Column(Modifier.padding(bottom = 16.dp).navigationBarsPadding()) {
                Text(f.prefijoDeLaFuente, style = MaterialTheme.typography.titleLarge, color = c.tinta, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                FormularioNuevoMovimiento.fuentesPosibles.forEach { fuente ->
                    Row(
                        Modifier.fillMaxWidth().clickable { alElegirFuente(fuente); eligiendoFuente = false }
                            .height(Tamano.toqueMinimo).padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(colorDeLaFuente(fuente), CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            FormularioNuevoMovimiento.etiquetaDeLaFuente(fuente),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (fuente == f.fuente) FontWeight.SemiBold else FontWeight.Normal),
                            color = c.tinta,
                        )
                    }
                }
            }
        }
    }
    if (creandoCategoria) {
        HojaDeCategoriaNueva(alCerrar = { creandoCategoria = false }) { nombre, icono -> alCrearCategoria(nombre, icono); creandoCategoria = false }
    }
}

@Composable
private fun Cabecera(alCerrar: () -> Unit) {
    val c = MiGastoTheme.colores
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(Tamano.botonDeIcono).clip(RoundedCornerShape(Radio.campo)).background(c.superficie).clickable(onClick = alCerrar)
                .semantics { contentDescription = "Cerrar" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Close, contentDescription = null, tint = c.tinta) }
        Text("Nuevo movimiento", style = MaterialTheme.typography.titleLarge, color = c.tinta, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        Spacer(Modifier.width(Tamano.botonDeIcono))
    }
}

@Composable
private fun SelectorDeTipo(tipo: TipoMovimiento, alCambiar: (TipoMovimiento) -> Unit) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radio.campo)).background(c.linea.copy(alpha = 0.6f)).padding(3.dp),
    ) {
        listOf(TipoMovimiento.Gasto to "Gasto", TipoMovimiento.Ingreso to "Ingreso").forEach { (t, texto) ->
            val elegido = tipo == t
            Box(
                Modifier.weight(1f).height(Tamano.toqueMinimo - 8.dp).clip(RoundedCornerShape(Radio.pequeno))
                    .background(if (elegido) c.elevado else Color.Transparent)
                    .clickable { alCambiar(t) }.semantics { contentDescription = texto },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    texto, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (!elegido) c.apagado else if (t == TipoMovimiento.Ingreso) c.ingreso else c.tinta,
                )
            }
        }
    }
}

@Composable
private fun Monto(f: FormularioNuevoMovimiento, acento: Color) {
    val c = MiGastoTheme.colores
    val ahora = remember { kotlin.time.Clock.System.now() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics { contentDescription = "Monto: ${f.entrada.texto} soles" }) {
            Text(f.prefijoDelMonto, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold), color = acento, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                f.entrada.texto,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, letterSpacing = (-1.5).sp, fontFeatureSettings = CifrasTabulares),
                color = acento,
            )
        }
        Text("Hoy ${formatearHora(ahora)}", style = MaterialTheme.typography.bodySmall, color = c.apagado)
    }
}

@Composable
private fun CampoDelConcepto(f: FormularioNuevoMovimiento, alEscribir: (String) -> Unit) {
    val c = MiGastoTheme.colores
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Radio.campo)).background(c.superficie).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Label, contentDescription = null, tint = c.apagado, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (f.concepto.isEmpty()) Text(f.pistaDelConcepto, style = MaterialTheme.typography.bodyLarge, color = c.apagado)
            BasicTextField(
                value = f.concepto, onValueChange = alEscribir, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tinta),
                cursorBrush = SolidColor(c.marcaTinta),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CampoDeLaFuente(f: FormularioNuevoMovimiento, alTocar: () -> Unit) {
    val c = MiGastoTheme.colores
    val etiqueta = FormularioNuevoMovimiento.etiquetaDeLaFuente(f.fuente)
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Radio.campo)).background(c.superficie).clickable(onClick = alTocar)
            .padding(horizontal = 14.dp).semantics { contentDescription = "${f.prefijoDeLaFuente} $etiqueta. Cambiar" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(colorDeLaFuente(f.fuente), CircleShape))
        Text("${f.prefijoDeLaFuente} $etiqueta", style = MaterialTheme.typography.bodyLarge, color = c.tinta, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text("Cambiar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = c.apagado)
        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.apagado, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChipDeCategoria(texto: String, icono: ImageVector, elegido: Boolean, esIngreso: Boolean, alTocar: () -> Unit) {
    val c = MiGastoTheme.colores
    val tinta = when { elegido && esIngreso -> c.ingreso; elegido -> c.marcaTinta; else -> c.tinta }
    val fondo = when { elegido && esIngreso -> c.ingresoSuave; elegido -> c.marcaSuave; else -> c.superficie }
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(Radio.tarjeta)).background(fondo)
            .border(1.dp, if (elegido) tinta else c.linea, RoundedCornerShape(Radio.tarjeta))
            .clickable(onClick = alTocar).padding(horizontal = 12.dp).semantics { contentDescription = texto },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = tinta, maxLines = 1)
    }
}

@Composable
private fun Teclado(alPresionar: (Char) -> Unit) {
    val c = MiGastoTheme.colores
    val filas = listOf("123", "456", "789", ".0<")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { tecla ->
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(Radio.campo)).background(c.superficie)
                            .clickable { alPresionar(tecla) }
                            .semantics { contentDescription = if (tecla == '<') "Borrar" else if (tecla == '.') "Punto" else tecla.toString() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (tecla == '<') {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = c.tinta, modifier = Modifier.size(22.dp))
                        } else {
                            Text(tecla.toString(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = c.tinta)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonGuardar(f: FormularioNuevoMovimiento, guardando: Boolean, alGuardar: () -> Unit) {
    val c = MiGastoTheme.colores
    val activo = f.puedeGuardar && !guardando
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton))
            .background(if (activo) ColoresMiGasto.VerdeNeon else c.linea)
            .clickable(enabled = activo, onClick = alGuardar)
            .semantics { contentDescription = f.textoDelBoton },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            f.textoDelBoton, style = MaterialTheme.typography.titleLarge,
            color = if (activo) ColoresMiGasto.TintaSobreVerde else c.apagado,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeCategoriaNueva(alCerrar: () -> Unit, alCrear: (String, String) -> Unit) {
    val c = MiGastoTheme.colores
    var nombre by remember { mutableStateOf("") }
    var icono by remember { mutableStateOf(clavesDeIconosPropios.first()) }
    ModalBottomSheet(onDismissRequest = alCerrar, containerColor = c.superficie) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nueva categoría", style = MaterialTheme.typography.titleLarge, color = c.tinta)
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Radio.campo)).background(c.fondo).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (nombre.isEmpty()) Text("Nombre", style = MaterialTheme.typography.bodyLarge, color = c.apagado)
                    BasicTextField(
                        value = nombre, onValueChange = { nombre = limitarNombreDeCategoria(it) }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tinta), cursorBrush = SolidColor(c.marcaTinta),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(6), modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(clavesDeIconosPropios) { clave ->
                    val elegido = clave == icono
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(Radio.campo))
                            .background(if (elegido) c.marcaSuave else c.fondo)
                            .border(1.dp, if (elegido) c.marcaTinta else Color.Transparent, RoundedCornerShape(Radio.campo))
                            .clickable { icono = clave }.semantics { contentDescription = "Ícono $clave" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(iconoPropio(clave), contentDescription = null, tint = if (elegido) c.marcaTinta else c.apagado, modifier = Modifier.size(22.dp)) }
                }
            }
            val puede = nombre.isNotBlank()
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(Radio.boton))
                    .background(if (puede) ColoresMiGasto.VerdeNeon else c.linea)
                    .clickable(enabled = puede) { alCrear(nombre, icono) },
                contentAlignment = Alignment.Center,
            ) { Text("Crear categoría", style = MaterialTheme.typography.titleLarge, color = if (puede) ColoresMiGasto.TintaSobreVerde else c.apagado) }
        }
    }
}
