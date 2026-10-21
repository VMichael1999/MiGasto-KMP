package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

// Las fechas de la app de Flutter se guardan como `DateTime.toIso8601String()`: hora local sin
// zona (`2026-09-27T12:41:00.000`) o UTC con `Z`. Hay que leer y escribir las dos.
class FechaIsoTest {
    private val utc = TimeZone.UTC
    private val lima = TimeZone.of("America/Lima")

    @Test
    fun escribeHoraLocalSinZonaConMilisegundos() {
        assertEquals("2026-09-27T12:41:00.000", fechaIsoLocal(fecha(), utc))
        assertEquals("2026-09-27T07:41:00.000", fechaIsoLocal(fecha(), lima))
        assertEquals("2026-01-02T03:04:05.006", fechaIsoLocal(Instant.parse("2026-01-02T03:04:05.006Z"), utc))
    }

    @Test
    fun escribeUtcConZ() {
        assertEquals("2026-10-01T12:00:00.000Z", fechaIsoUtc(fecha(dia = 1, hora = 12, minuto = 0, mes = 10)))
    }

    @Test
    fun leeConZYLoTomaComoUtc() {
        assertEquals(fecha(dia = 1, hora = 9, minuto = 30, mes = 10), leerFechaIso("2026-10-01T09:30:00.000Z", lima))
    }

    @Test
    fun leeSinZonaComoHoraLocal() {
        assertEquals(fecha(), leerFechaIso("2026-09-27T07:41:00.000", lima))
        assertEquals(fecha(), leerFechaIso("2026-09-27T12:41:00.000", utc))
        assertEquals(fecha(), leerFechaIso("2026-09-27T12:41:00", utc))
    }

    @Test
    fun leeConDesfase() {
        assertEquals(fecha(), leerFechaIso("2026-09-27T07:41:00.000-05:00", utc))
    }

    @Test
    fun escribirYLeerDaLoMismo() {
        val i = Instant.parse("2026-03-04T05:06:07.089Z")
        assertEquals(i, leerFechaIso(fechaIsoLocal(i, lima), lima))
        assertEquals(i, leerFechaIso(fechaIsoUtc(i), lima))
    }
}
