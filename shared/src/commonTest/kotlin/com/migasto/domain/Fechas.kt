package com.migasto.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Fecha de ejemplo para los tests, equivalente a `DateTime(2026, 9, 27, 12, 41)` de Dart. */
fun fecha(
    dia: Int = 27,
    hora: Int = 12,
    minuto: Int = 41,
    mes: Int = 9,
    anio: Int = 2026,
): Instant = LocalDateTime(anio, mes, dia, hora, minuto).toInstant(TimeZone.UTC)
