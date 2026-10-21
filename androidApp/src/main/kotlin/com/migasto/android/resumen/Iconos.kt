package com.migasto.android.resumen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
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

private val iconosPropios: Map<String, ImageVector> by lazy {
    mapOf(
        "restaurante" to Icons.Outlined.Restaurant, "cafe" to Icons.Outlined.LocalCafe, "pizza" to Icons.Outlined.LocalPizza,
        "hamburguesa" to Icons.Outlined.LunchDining, "pastel" to Icons.Outlined.Cake, "bar" to Icons.Outlined.LocalBar,
        "carrito" to Icons.Outlined.ShoppingCart, "bolsa" to Icons.Outlined.ShoppingBag, "tienda" to Icons.Outlined.Storefront,
        "ropa" to Icons.Outlined.Checkroom, "bus" to Icons.Outlined.DirectionsBus, "auto" to Icons.Outlined.DirectionsCar,
        "gasolina" to Icons.Outlined.LocalGasStation, "moto" to Icons.Outlined.TwoWheeler, "avion" to Icons.Outlined.Flight,
        "hotel" to Icons.Outlined.Hotel, "casa" to Icons.Outlined.Home, "llave" to Icons.Outlined.Key, "luz" to Icons.Outlined.Bolt,
        "agua" to Icons.Outlined.WaterDrop, "wifi" to Icons.Outlined.Wifi, "celular" to Icons.Outlined.PhoneAndroid,
        "tele" to Icons.Outlined.Tv, "cine" to Icons.Outlined.Movie, "musica" to Icons.Outlined.MusicNote,
        "juegos" to Icons.Outlined.SportsEsports, "futbol" to Icons.Outlined.SportsSoccer, "gimnasio" to Icons.Outlined.FitnessCenter,
        "spa" to Icons.Outlined.Spa, "salud" to Icons.Outlined.MedicalServices, "medicina" to Icons.Outlined.Medication,
        "escuela" to Icons.Outlined.School, "libro" to Icons.Outlined.MenuBook, "bebe" to Icons.Outlined.ChildCare,
        "mascota" to Icons.Outlined.Pets, "regalo" to Icons.Outlined.Redeem, "fiesta" to Icons.Outlined.Celebration,
        "ahorro" to Icons.Outlined.Savings, "banco" to Icons.Outlined.AccountBalance, "tarjeta" to Icons.Outlined.CreditCard,
        "pagos" to Icons.Outlined.Payments, "trabajo" to Icons.Outlined.Work, "herramientas" to Icons.Outlined.Handyman,
        "arreglo" to Icons.Outlined.Build, "computadora" to Icons.Outlined.Computer, "camara" to Icons.Outlined.CameraAlt,
        "arte" to Icons.Outlined.Brush, "lavanderia" to Icons.Outlined.LocalLaundryService, "donacion" to Icons.Outlined.VolunteerActivism,
        "corazon" to Icons.Outlined.FavoriteBorder, "estrella" to Icons.Outlined.StarBorder, "planta" to Icons.Outlined.Eco,
        "parque" to Icons.Outlined.Park, "playa" to Icons.Outlined.BeachAccess, "maleta" to Icons.Outlined.Luggage,
        "recibo" to Icons.Outlined.ReceiptLong, "inversion" to Icons.Outlined.TrendingUp, "flores" to Icons.Outlined.LocalFlorist,
        "scissors" to Icons.Outlined.ContentCut,
    )
}

/** Ícono que eligió el usuario para una categoría propia; uno genérico si no se conoce la clave. */
fun iconoPropio(clave: String): ImageVector = iconosPropios[clave] ?: Icons.Outlined.Category
