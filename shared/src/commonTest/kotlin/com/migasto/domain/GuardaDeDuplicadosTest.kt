package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Portado de android/app/src/test/.../DuplicateGuardTest.kt (los 9 tests nativos de Flutter).
class GuardaDeDuplicadosTest {
    private var ahora: Instant = fecha()
    private val guarda = GuardaDeDuplicados(2.minutes) { ahora }
    private val clave = "yape|ingreso|1.00"

    private fun dup(origen: OrigenDeLectura, k: String = clave) = guarda.esDuplicado(k, origen)

    @Test
    fun primerPagoNoEsDuplicado() = assertFalse(dup(OrigenDeLectura.Notificaciones))

    @Test
    fun dosYapesDelMismoMontoSeguidosValenLosDos() {
        assertFalse(dup(OrigenDeLectura.Notificaciones))
        ahora += 60.seconds
        assertFalse(dup(OrigenDeLectura.Notificaciones))
        ahora += 30.seconds
        assertFalse(dup(OrigenDeLectura.Notificaciones))
    }

    @Test
    fun laNotificacionYLaPantallaDeLaConstanciaSonElMismoPago() {
        assertFalse(dup(OrigenDeLectura.Notificaciones))
        ahora += 5.seconds
        assertTrue(dup(OrigenDeLectura.Pantalla))
    }

    @Test
    fun laPantallaYLaNotificacionTambien() {
        assertFalse(dup(OrigenDeLectura.Pantalla))
        ahora += 5.seconds
        assertTrue(dup(OrigenDeLectura.Notificaciones))
    }

    @Test
    fun laAccesibilidadYElServicioDeNotificacionesSonElMismoPago() {
        assertFalse(dup(OrigenDeLectura.Notificaciones))
        ahora += 1.seconds
        assertTrue(dup(OrigenDeLectura.Accesibilidad))
    }

    @Test
    fun dosAvisosDeAccesibilidadDelMismoMontoSiguenFrenados() {
        assertFalse(dup(OrigenDeLectura.Accesibilidad))
        ahora += 10.seconds
        assertTrue(dup(OrigenDeLectura.Accesibilidad))
    }

    @Test
    fun pasadaLaVentanaYaNoEsDuplicado() {
        assertFalse(dup(OrigenDeLectura.Pantalla))
        ahora += 2.minutes + 1.milliseconds
        assertFalse(dup(OrigenDeLectura.Notificaciones))
    }

    @Test
    fun otroMontoOtroTipoOOtraFuenteNoSeFrena() {
        assertFalse(dup(OrigenDeLectura.Notificaciones))
        assertFalse(dup(OrigenDeLectura.Pantalla, "yape|ingreso|2.00"))
        assertFalse(dup(OrigenDeLectura.Pantalla, "yape|gasto|1.00"))
        assertFalse(dup(OrigenDeLectura.Pantalla, "plin|ingreso|1.00"))
    }

    @Test
    fun unaConstanciaLeidaDosVecesSeFrena() {
        assertFalse(dup(OrigenDeLectura.Pantalla))
        ahora += 20.seconds
        assertTrue(dup(OrigenDeLectura.Pantalla))
    }
}
