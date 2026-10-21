package com.migasto.android.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Espaciados, radios y tamaños de `app_tokens.dart`. */
object Espacio {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 18.dp // padding horizontal de pantalla
    val xxl = 22.dp
}

object Radio {
    val chip = 999.dp
    val pequeno = 9.dp
    val icono = 11.dp // ícono de categoría en listas
    val campo = 13.dp // campos, botones de ícono, segmentos
    val tarjeta = 14.dp // tarjetas pequeñas, chips de estado
    val boton = 15.dp
    val caja = 16.dp // cajas de ajustes
    val hoja = 22.dp
    val agregar = 18.dp // botón agregar de la barra inferior
}

object Tamano {
    val botonDeIcono = 44.dp
    val toqueMinimo = 48.dp
    val iconoDeCategoria = 36.dp
    val agregar = 52.dp
    val pista = 8.dp // barra de presupuesto
}

val LocalColoresMiGasto = staticCompositionLocalOf { ColoresMiGasto.Oscuro }

object MiGastoTheme {
    val colores: ColoresMiGasto
        @Composable get() = LocalColoresMiGasto.current
}

private fun esquema(c: ColoresMiGasto, oscuro: Boolean): ColorScheme {
    val base = if (oscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ColoresMiGasto.VerdeNeon,
        onPrimary = ColoresMiGasto.TintaSobreVerde,
        secondary = c.ingreso,
        background = c.fondo,
        onBackground = c.tinta,
        surface = c.superficie,
        onSurface = c.tinta,
        onSurfaceVariant = c.apagado,
        outline = c.linea,
        outlineVariant = c.linea,
        error = c.error,
        surfaceContainerHighest = c.elevado,
    )
}

@Composable
fun MiGastoTheme(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    val colores = if (oscuro) ColoresMiGasto.Oscuro else ColoresMiGasto.Claro
    CompositionLocalProvider(LocalColoresMiGasto provides colores) {
        MaterialTheme(
            colorScheme = esquema(colores, oscuro),
            typography = tipografiaMiGasto(colores.apagado),
            content = contenido,
        )
    }
}
