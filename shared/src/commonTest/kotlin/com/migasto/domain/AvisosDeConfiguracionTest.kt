package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

// Portado de test/setup_issues_test.dart.
class AvisosDeConfiguracionTest {
    private val ahora = fecha(dia = 1, hora = 12, minuto = 0, mes = 10)

    private fun avisos(
        esAndroid: Boolean = true,
        notificaciones: Boolean = true,
        bateria: Boolean = true,
        avisos: Boolean = true,
        pospuestoBateria: kotlin.time.Instant? = null,
        pospuestoAvisos: kotlin.time.Instant? = null,
    ) = calcularAvisosDeConfiguracion(
        esAndroid = esAndroid,
        lecturaActivada = notificaciones,
        bateriaSinRestriccion = bateria,
        avisosActivados = avisos,
        avisosPospuestosEn = pospuestoAvisos,
        bateriaPospuestaEn = pospuestoBateria,
        ahora = ahora,
    )

    @Test
    fun todoEnOrdenNoHayAvisos() = assertEquals(emptyList(), avisos())

    @Test
    fun enIphoneNuncaHayAvisos() =
        assertEquals(emptyList(), avisos(esAndroid = false, notificaciones = false, bateria = false))

    @Test
    fun lecturaApagadaEsLoPrimero() {
        assertEquals(
            listOf(AvisoDeConfiguracion.LecturaApagada, AvisoDeConfiguracion.BateriaRestringida),
            avisos(notificaciones = false, bateria = false),
        )
        assertEquals(listOf(AvisoDeConfiguracion.LecturaApagada), avisos(notificaciones = false))
    }

    @Test
    fun bateriaRestringidaAvisaSoloSiLaLecturaEstaEncendida() =
        assertEquals(listOf(AvisoDeConfiguracion.BateriaRestringida), avisos(bateria = false))

    @Test
    fun ahoraNoEsconde7DiasElAvisoDeBateriaYLuegoVuelve() {
        assertEquals(emptyList(), avisos(bateria = false, pospuestoBateria = ahora - 3.days))
        assertEquals(listOf(AvisoDeConfiguracion.BateriaRestringida), avisos(bateria = false, pospuestoBateria = ahora - 8.days))
    }

    @Test
    fun ahoraNoNoEscondeQueLaLecturaEsteApagada() {
        assertEquals(
            listOf(AvisoDeConfiguracion.LecturaApagada),
            avisos(notificaciones = false, bateria = false, pospuestoBateria = ahora - 1.days),
        )
    }

    @Test
    fun avisosDeNotificacionesApagadosSalenSiLaLecturaEstaEncendida() {
        assertEquals(listOf(AvisoDeConfiguracion.AvisosApagados), avisos(avisos = false))
        assertEquals(
            listOf(AvisoDeConfiguracion.AvisosApagados, AvisoDeConfiguracion.BateriaRestringida),
            avisos(avisos = false, bateria = false),
        )
    }

    @Test
    fun sinLaLecturaEncendidaPrimeroVaEseAvisoYNoElDeLasNotificacionesPropias() =
        assertEquals(listOf(AvisoDeConfiguracion.LecturaApagada), avisos(notificaciones = false, avisos = false))

    @Test
    fun ahoraNoEnLosAvisosLosEscondeSieteDiasAparteDelDeBateria() {
        assertEquals(emptyList(), avisos(avisos = false, pospuestoAvisos = ahora - 2.days))
        assertEquals(listOf(AvisoDeConfiguracion.AvisosApagados), avisos(avisos = false, pospuestoAvisos = ahora - 9.days))
        assertEquals(
            listOf(AvisoDeConfiguracion.BateriaRestringida),
            avisos(avisos = false, bateria = false, pospuestoAvisos = ahora - 2.days),
        )
    }

    @Test
    fun justoALos7DiasElAvisoVuelve() {
        assertEquals(listOf(AvisoDeConfiguracion.BateriaRestringida), avisos(bateria = false, pospuestoBateria = ahora - 7.days))
        assertEquals(emptyList(), avisos(bateria = false, pospuestoBateria = ahora - 7.days + 1.days))
    }
}
