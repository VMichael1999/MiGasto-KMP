package com.migasto.domain

import kotlin.math.abs
import kotlin.math.roundToLong

// Port de la parte pura de lib/data/services/location_service.dart. Pedir el permiso y leer el GPS es de cada
// sistema; qué se guarda, qué se dice cuando falla y cómo se arma la dirección sale de aquí.

/** Lo que se toma al tocar «Agregar ubicación». */
data class UbicacionCapturada(val latitud: Double, val longitud: Double, val precision: Double, val lugar: String?)

enum class MotivoDeFallo(val ofreceAbrirAjustes: Boolean = false) {
    ServiciosApagados,
    Denegada,
    DenegadaParaSiempre(ofreceAbrirAjustes = true),
    NoDisponible,
}

fun mensajeDeFallo(motivo: MotivoDeFallo): String = when (motivo) {
    MotivoDeFallo.ServiciosApagados -> "Activa la ubicación de tu teléfono para agregarla."
    MotivoDeFallo.Denegada -> "Sin el permiso de ubicación no podemos guardar dónde pagaste."
    MotivoDeFallo.DenegadaParaSiempre -> "El permiso de ubicación está bloqueado. Actívalo en los ajustes del teléfono."
    MotivoDeFallo.NoDisponible -> "No pudimos obtener tu ubicación. Prueba de nuevo al aire libre."
}

object TextosDeUbicacion {
    const val AGREGAR = "Agregar ubicación"
    const val AGREGAR_DETALLE = "Usa tu ubicación actual, solo ahora."
    const val ABRIR_AJUSTES = "Abrir ajustes"
}

/** «Av. Arequipa 3120, San Isidro»: calle y número, y el barrio (o, si no hay, la ciudad). `null` si no hay nada. */
fun armarDireccion(calle: String?, numero: String?, barrio: String?, ciudad: String?): String? {
    fun String?.limpio() = this?.trim().orEmpty()
    val via = listOf(calle.limpio(), numero.limpio()).filter { it.isNotEmpty() }.joinToString(" ")
    val zona = barrio.limpio().ifEmpty { ciudad.limpio() }
    return listOf(via, zona).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null }
}

/** La dirección, o las coordenadas si no se pudo resolver. */
fun direccionParaMostrar(m: Movimiento): String =
    m.lugar ?: "${cincoDecimales(m.latitud ?: 0.0)}, ${cincoDecimales(m.longitud ?: 0.0)}"

fun textoDeLaPrecision(m: Movimiento): String =
    "Ubicación del teléfono" + (m.precision?.let { " · precisión ±${it.roundToLong()} m" } ?: "")

private fun cincoDecimales(valor: Double): String {
    val escalado = (abs(valor) * 100_000).roundToLong()
    val entero = escalado / 100_000
    val decimales = (escalado % 100_000).toString().padStart(5, '0')
    return "${if (valor < 0 && escalado != 0L) "-" else ""}$entero.$decimales"
}
