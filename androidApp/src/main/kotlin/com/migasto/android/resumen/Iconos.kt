package com.migasto.android.resumen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.migasto.domain.Categoria

/** Ícono de una categoría de siempre. */
fun iconoDeCategoria(categoria: Categoria): ImageVector = when (categoria) {
    Categoria.Alimentacion -> Icons.Outlined.Restaurant
    Categoria.Transporte -> Icons.Outlined.DirectionsCar
    Categoria.Compras -> Icons.Outlined.ShoppingBag
    Categoria.Servicios -> Icons.Outlined.CreditCard
    Categoria.Entretenimiento -> Icons.Outlined.Movie
    Categoria.Otros, Categoria.OtrosIngresos -> Icons.Outlined.Category
    Categoria.Sueldo -> Icons.Outlined.Work
    Categoria.TransferenciaRecibida -> Icons.Outlined.SwapHoriz
    Categoria.Venta -> Icons.Outlined.Sell
}

/** Ícono que eligió el usuario para una categoría propia; uno genérico si no se conoce la clave. */
fun iconoPropio(clave: String): ImageVector = when (clave) {
    "scissors" -> Icons.Outlined.ContentCut
    "spa" -> Icons.Outlined.Spa
    "gimnasio" -> Icons.Outlined.FitnessCenter
    "cafe" -> Icons.Outlined.LocalCafe
    else -> Icons.Outlined.Category
}
