package com.migasto.android.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.migasto.android.R

/**
 * Outfit, empaquetada en la app (res/font/outfit.ttf, licencia OFL en docs/licencias). Es una fuente
 * variable: cada peso es el mismo archivo con otro eje `wght`. En Android 7 (API 24–25) no hay fuentes
 * variables y se ve con el peso normal.
 */
@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    listOf(400, 500, 600, 700).map { peso ->
        Font(
            resId = R.font.outfit,
            weight = FontWeight(peso),
            variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
        )
    },
)

/** Cifras de ancho fijo, para que los montos no bailen. */
const val CifrasTabulares = "tnum"

private fun estilo(tamano: Float, peso: Int, espaciado: Float = 0f, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) =
    TextStyle(
        fontFamily = Outfit,
        fontSize = tamano.sp,
        fontWeight = FontWeight(peso),
        letterSpacing = espaciado.sp,
        lineHeight = (tamano * 1.4f).sp,
        color = color,
    )

/** La escala de `_textTheme` de Flutter, con los mismos tamaños, pesos e interlineado. */
fun tipografiaMiGasto(apagado: androidx.compose.ui.graphics.Color) = Typography(
    displayLarge = estilo(42f, 700, -1.26f), // saldo del Resumen
    displayMedium = estilo(38f, 700, -1.14f), // monto en la ventana flotante
    headlineMedium = estilo(24f, 700, -0.48f), // título de pantalla
    headlineSmall = estilo(22f, 700, -0.33f),
    titleLarge = estilo(16.5f, 600),
    titleMedium = estilo(15f, 600),
    titleSmall = estilo(14.5f, 600),
    bodyLarge = estilo(14.5f, 400),
    bodyMedium = estilo(14f, 400),
    bodySmall = estilo(12.5f, 400, color = apagado),
    labelLarge = estilo(13.5f, 600),
    labelMedium = estilo(12f, 500, color = apagado),
    labelSmall = estilo(11f, 400, color = apagado),
)
