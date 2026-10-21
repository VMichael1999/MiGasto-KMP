package com.migasto.domain

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// Port de lib/domain/setup_issues.dart.

/** Lo que impide que MiGasto lea los pagos solo. */
enum class AvisoDeConfiguracion {
    /** El acceso a notificaciones está apagado: no se ve ningún pago. */
    LecturaApagada,

    /** Las notificaciones de MiGasto están apagadas: con la pantalla bloqueada no se entera de ningún pago. */
    AvisosApagados,

    /** La batería de la app está restringida: el teléfono puede dormir la lectura. */
    BateriaRestringida,
}

/** Cuánto tiempo se esconde un aviso después de tocar «Ahora no». */
val pausaDelAviso = 7.days

/** Problemas a avisar, el más importante primero. Solo aplica a Android. */
fun calcularAvisosDeConfiguracion(
    esAndroid: Boolean,
    lecturaActivada: Boolean,
    bateriaSinRestriccion: Boolean,
    ahora: Instant,
    avisosActivados: Boolean = true,
    avisosPospuestosEn: Instant? = null,
    bateriaPospuestaEn: Instant? = null,
): List<AvisoDeConfiguracion> {
    if (!esAndroid) return emptyList()
    fun pospuesto(en: Instant?) = en != null && ahora - en < pausaDelAviso

    return buildList {
        if (!lecturaActivada) add(AvisoDeConfiguracion.LecturaApagada)
        // Sin permiso de leer, avisar de las notificaciones propias no tiene sentido todavía.
        if (lecturaActivada && !avisosActivados && !pospuesto(avisosPospuestosEn)) add(AvisoDeConfiguracion.AvisosApagados)
        if (!bateriaSinRestriccion && !pospuesto(bateriaPospuestaEn)) add(AvisoDeConfiguracion.BateriaRestringida)
    }
}

/** El texto de un aviso de configuración, como se ve en el Resumen. */
data class TextosDelAvisoDeConfiguracion(
    val titulo: String,
    val cuerpo: String,
    /** Lo que dice el botón que lleva a arreglarlo. */
    val accion: String,
    /** «Ahora no» esconde el aviso 7 días; la lectura apagada no se puede esconder. */
    val sePuedePosponer: Boolean,
) {
    val descripcion: String get() = "$titulo. $cuerpo"
}

fun textosDelAvisoDeConfiguracion(aviso: AvisoDeConfiguracion): TextosDelAvisoDeConfiguracion = when (aviso) {
    AvisoDeConfiguracion.LecturaApagada -> TextosDelAvisoDeConfiguracion(
        "La lectura de pagos está apagada",
        "MiGasto no está viendo tus pagos de Yape, Plin ni Wallet. Actívala en Acceso a notificaciones.",
        "Activar",
        sePuedePosponer = false,
    )
    AvisoDeConfiguracion.AvisosApagados -> TextosDelAvisoDeConfiguracion(
        "Activa los avisos de MiGasto",
        "Con la pantalla apagada o bloqueada, solo así sabrás qué pago se detectó.",
        "Activar",
        sePuedePosponer = true,
    )
    AvisoDeConfiguracion.BateriaRestringida -> TextosDelAvisoDeConfiguracion(
        "Evita que el teléfono cierre la lectura",
        "Entra a Batería y elige «No restringido» («Sin restricciones» en otras marcas). " +
            "Si no, el teléfono puede dormir la app y se pierden pagos.",
        "Ajustar",
        sePuedePosponer = true,
    )
}

/** Cada aviso que se puede posponer tiene su propia pausa de 7 días. */
enum class ClaveDePausa { Bateria, Avisos }

fun claveDePausa(aviso: AvisoDeConfiguracion): ClaveDePausa? = when (aviso) {
    AvisoDeConfiguracion.BateriaRestringida -> ClaveDePausa.Bateria
    AvisoDeConfiguracion.AvisosApagados -> ClaveDePausa.Avisos
    AvisoDeConfiguracion.LecturaApagada -> null
}
