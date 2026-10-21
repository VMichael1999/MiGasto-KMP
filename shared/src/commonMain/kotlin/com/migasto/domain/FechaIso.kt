package com.migasto.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// La app de Flutter guarda las fechas como `DateTime.toIso8601String()`: hora local sin zona
// (`2026-09-27T12:41:00.000`) o UTC con `Z`. Estas funciones hacen lo mismo para que los
// respaldos y los CSV se entiendan en las dos apps.

private fun Int.dos() = toString().padStart(2, '0')

/** Hora local de [zona] sin indicar la zona, con milisegundos: `2026-09-27T12:41:00.000`. */
fun fechaIsoLocal(fecha: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String {
    val l = fecha.toLocalDateTime(zona)
    val ms = (l.nanosecond / 1_000_000).toString().padStart(3, '0')
    return "${l.year.toString().padStart(4, '0')}-${(l.month.ordinal + 1).dos()}-${l.day.dos()}" +
        "T${l.hour.dos()}:${l.minute.dos()}:${l.second.dos()}.$ms"
}

/** UTC con `Z` y milisegundos: `2026-10-01T12:00:00.000Z`. */
fun fechaIsoUtc(fecha: Instant): String = fechaIsoLocal(fecha, TimeZone.UTC) + "Z"

private val conZona = Regex("(Z|[+-]\\d{2}(:?\\d{2})?)$", RegexOption.IGNORE_CASE)

/** Lee una fecha ISO: con `Z` o desfase es un instante exacto; sin zona, hora local de [zona]. */
fun leerFechaIso(texto: String, zona: TimeZone = TimeZone.currentSystemDefault()): Instant {
    val t = texto.trim()
    val hora = t.substringAfter('T', "")
    return if (conZona.containsMatchIn(hora)) Instant.parse(t) else LocalDateTime.parse(t).toInstant(zona)
}
