package com.migasto.android.ubicacion

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.migasto.domain.MotivoDeFallo
import com.migasto.domain.UbicacionCapturada
import com.migasto.domain.armarDireccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed interface ResultadoDeCaptura {
    data class Ok(val ubicacion: UbicacionCapturada) : ResultadoDeCaptura

    data class Fallo(val motivo: MotivoDeFallo) : ResultadoDeCaptura
}

/** Lee el GPS una sola vez (port de DeviceLocationService de Flutter); no usa servicios de Google ni corre en segundo plano. */
class ServicioDeUbicacion(private val contexto: Context) {
    private val manager get() = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun serviciosEncendidos(): Boolean = LocationManagerCompat.isLocationEnabled(manager)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Llamar solo con el permiso dado. */
    @SuppressLint("MissingPermission")
    suspend fun capturar(): ResultadoDeCaptura {
        if (!serviciosEncendidos()) return ResultadoDeCaptura.Fallo(MotivoDeFallo.ServiciosApagados)
        val proveedor = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { manager.isProviderEnabled(it) }
            ?: return ResultadoDeCaptura.Fallo(MotivoDeFallo.NoDisponible)
        val punto = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Location?> { continuacion ->
                val senal = android.os.CancellationSignal()
                continuacion.invokeOnCancellation { senal.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, proveedor, senal, ContextCompat.getMainExecutor(contexto)) { continuacion.resume(it) }
            }
        } ?: return ResultadoDeCaptura.Fallo(MotivoDeFallo.NoDisponible)
        return ResultadoDeCaptura.Ok(UbicacionCapturada(punto.latitude, punto.longitude, punto.accuracy.toDouble(), direccion(punto.latitude, punto.longitude)))
    }

    /** «Av. Arequipa 3120, San Isidro»; se resuelve en el teléfono y, si no se puede, no se guarda dirección. */
    private suspend fun direccion(latitud: Double, longitud: Double): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(contexto, Locale.getDefault())
        val lugar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuacion ->
                geocoder.getFromLocation(
                    latitud, longitud, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) = continuacion.resume(addresses.firstOrNull())

                        override fun onError(errorMessage: String?) = continuacion.resume(null)
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitud, longitud, 1)?.firstOrNull()
            }
        }
        lugar?.let { armarDireccion(it.thoroughfare, it.subThoroughfare, it.subLocality, it.locality) }
    }.getOrNull()
}

/** `true` si la app se compiló con una clave de Google Maps (sin ella el mapa no dibuja nada). */
fun hayClaveDeMapas(contexto: Context): Boolean = runCatching {
    val info = contexto.packageManager.getApplicationInfo(contexto.packageName, PackageManager.GET_META_DATA)
    !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
}.getOrDefault(false)
