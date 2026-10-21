package com.migasto.android.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.migasto.domain.ZonaDePagos
import com.migasto.domain.limitesDelMapa

private val sinControles = MapUiSettings(
    compassEnabled = false, indoorLevelPickerEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false,
    rotationGesturesEnabled = false, scrollGesturesEnabled = false, scrollGesturesEnabledDuringRotateOrZoom = false,
    tiltGesturesEnabled = false, zoomControlsEnabled = false, zoomGesturesEnabled = false,
)

/** Mapa pequeño del detalle (en modo ligero, sin gestos) con un marcador en el lugar del pago. */
@Composable
fun MapaChico(latitud: Double, longitud: Double, modifier: Modifier = Modifier) {
    val punto = LatLng(latitud, longitud)
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(punto, 16f) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        uiSettings = sinControles,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
    ) { Marker(state = rememberMarkerState(position = punto)) }
}

/** El mapa de «Dónde pagaste»: un marcador por zona, encuadrado para que se vean todos. */
@Composable
fun MapaDeZonas(zonas: List<ZonaDePagos>, modifier: Modifier = Modifier) {
    val primera = zonas.firstOrNull()
    val camara = rememberCameraPositionState {
        // Sin pagos, Lima (como en Flutter).
        position = CameraPosition.fromLatLngZoom(
            primera?.let { LatLng(it.latitudDelCentro, it.longitudDelCentro) } ?: LatLng(-12.0464, -77.0428),
            13f,
        )
    }
    var cargado by remember { mutableStateOf(false) }
    LaunchedEffect(zonas, cargado) {
        if (!cargado) return@LaunchedEffect
        val limites = limitesDelMapa(zonas) ?: return@LaunchedEffect
        val actualizacion = if (limites.puntos == 1 || (limites.sur == limites.norte && limites.oeste == limites.este)) {
            CameraUpdateFactory.newLatLngZoom(LatLng(limites.sur, limites.oeste), 15f)
        } else {
            CameraUpdateFactory.newLatLngBounds(LatLngBounds(LatLng(limites.sur, limites.oeste), LatLng(limites.norte, limites.este)), 96)
        }
        camara.animate(actualizacion)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
        onMapLoaded = { cargado = true },
    ) {
        zonas.forEach { zona ->
            val centro = LatLng(zona.latitudDelCentro, zona.longitudDelCentro)
            Marker(state = rememberMarkerState(key = zona.nombre, position = centro), title = zona.nombre, snippet = zona.resumen)
        }
    }
}
