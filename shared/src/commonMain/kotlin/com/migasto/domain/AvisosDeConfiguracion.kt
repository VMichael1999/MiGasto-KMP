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
