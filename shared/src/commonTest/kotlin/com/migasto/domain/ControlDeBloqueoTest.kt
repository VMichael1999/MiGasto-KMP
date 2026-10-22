package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Port de la lógica de lib/presentation/lock/lock_gate.dart y del interruptor de Ajustes
// (_toggleLock), con los tests del grupo «Bloqueo» de test/lock_and_csv_test.dart.
class ControlDeBloqueoTest {
    private var ahora: Instant = fecha()

    private fun control(activado: Boolean) = ControlDeBloqueo(activado) { ahora }

    // Los tres tests del grupo «Bloqueo» de Flutter

    @Test
    fun sinBloqueoActivadoNoSePideNada() {
        val c = control(activado = false)
        assertFalse(c.alAbrir())
        assertFalse(c.visible(activado = false))
    }

    @Test
    fun conBloqueoActivadoSePideElDesbloqueoYSeEntra() {
        val c = control(activado = true)
        assertTrue(c.visible(activado = true)) // desde el primer cuadro se ve la pantalla de bloqueo
        assertTrue(c.alAbrir())
        assertTrue(c.alEmpezarAutenticacion())
        c.alTerminarAutenticacion(correcta = true)
        assertFalse(c.visible(activado = true))
    }

    @Test
    fun siFallaLaAutenticacionSigueBloqueadoYSePuedeReintentar() {
        val c = control(activado = true)
        assertTrue(c.alEmpezarAutenticacion())
        c.alTerminarAutenticacion(correcta = false)
        assertTrue(c.visible(activado = true))
        // «Desbloquear»: se vuelve a pedir.
        assertTrue(c.alEmpezarAutenticacion())
        c.alTerminarAutenticacion(correcta = false)
        assertTrue(c.visible(activado = true))
        assertTrue(c.alEmpezarAutenticacion())
        c.alTerminarAutenticacion(correcta = true)
        assertFalse(c.visible(activado = true))
    }

    // Casos nuevos

    @Test
    fun mientrasSeAutenticaNoSeAbreOtraAutenticacion() {
        val c = control(activado = true)
        assertTrue(c.alEmpezarAutenticacion())
        assertFalse(c.alEmpezarAutenticacion())
        c.alTerminarAutenticacion(correcta = false)
        assertTrue(c.alEmpezarAutenticacion())
    }

    @Test
    fun alVolverDespuesDe30SegundosSeVuelveABloquear() {
        val c = control(activado = true)
        c.alEmpezarAutenticacion(); c.alTerminarAutenticacion(correcta = true)
        c.alPasarAlFondo()
        ahora += 30.seconds
        assertTrue(c.alVolver(activado = true))
        assertTrue(c.visible(activado = true))
    }

    @Test
    fun alVolverAntesDe30SegundosNoSePideNada() {
        val c = control(activado = true)
        c.alEmpezarAutenticacion(); c.alTerminarAutenticacion(correcta = true)
        c.alPasarAlFondo()
        ahora += 29.seconds
        assertFalse(c.alVolver(activado = true))
        assertFalse(c.visible(activado = true))
    }

    @Test
    fun siElBloqueoEstaApagadoVolverNoBloquea() {
        val c = control(activado = false)
        c.alPasarAlFondo()
        ahora += 5.minutes
        assertFalse(c.alVolver(activado = false))
        assertFalse(c.visible(activado = false))
    }

    @Test
    fun elAvisoDelSistemaPausaLaAppPeroSeIgnoraMientrasSeAutentica() {
        val c = control(activado = true)
        assertTrue(c.alEmpezarAutenticacion())
        c.alPasarAlFondo() // el aviso del sistema también pausa la app
        ahora += 60.seconds
        assertFalse(c.alVolver(activado = true))
        c.alTerminarAutenticacion(correcta = true)
        assertFalse(c.visible(activado = true))
    }

    @Test
    fun volverSinHaberPasadoAlFondoNoBloquea() {
        val c = control(activado = true)
        c.alEmpezarAutenticacion(); c.alTerminarAutenticacion(correcta = true)
        assertFalse(c.alVolver(activado = true))
    }

    @Test
    fun siSeApagaElBloqueoLaPantallaDesaparece() {
        val c = control(activado = true)
        assertTrue(c.visible(activado = true))
        assertFalse(c.visible(activado = false))
    }

    @Test
    fun siSeEnciendeConLaAppAbiertaNoPideNadaHastaSalir() {
        val c = control(activado = false)
        assertFalse(c.visible(activado = true)) // recién activado: ya se autenticó para activarlo
        c.alPasarAlFondo()
        ahora += 31.seconds
        assertTrue(c.alVolver(activado = true))
    }

    @Test
    fun losTextosDelBloqueo() {
        assertEquals("MiGasto está bloqueado", TextosDelBloqueo.TITULO)
        assertEquals("Usa tu huella, rostro o el PIN del teléfono para entrar.", TextosDelBloqueo.EXPLICACION)
        assertEquals("Desbloquear", TextosDelBloqueo.BOTON)
        assertEquals("Desbloquea MiGasto para ver tus movimientos", TextosDelBloqueo.MOTIVO_AL_ABRIR)
        assertEquals("Confirma para activar el bloqueo de MiGasto", TextosDelBloqueo.MOTIVO_AL_ACTIVAR)
        assertTrue(TextosDelBloqueo.SIN_BLOQUEO_EN_EL_TELEFONO.startsWith("Configura un PIN, huella o rostro"))
    }

    // Activar y apagar desde Ajustes

    @Test
    fun alActivarSinBloqueoEnElTelefonoSeExplica() {
        assertEquals(DecisionAlActivar.Explicar, decidirAlActivar(telefonoSoportaBloqueo = false))
        assertEquals(DecisionAlActivar.PedirConfirmacion, decidirAlActivar(telefonoSoportaBloqueo = true))
    }

    @Test
    fun soloSeActivaSiLaConfirmacionSalioBien() {
        assertTrue(activarSiConfirmo(correcta = true))
        assertFalse(activarSiConfirmo(correcta = false))
    }

    // Se guarda en los ajustes

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-bloqueo.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun elBloqueoEstaApagadoPorDefectoYSeGuarda() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            assertFalse(base.ajustes.bloqueoActivado())
            base.ajustes.establecerBloqueoActivado(true)
            assertTrue(base.ajustes.bloqueoActivado())
        }
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            assertTrue(base.ajustes.bloqueoActivado())
            assertEquals("true", base.ajustes.leerCrudo("migasto_passcode_enabled"))
        }
    }

    @Test
    fun elConstructorParaSwiftUsaElRelojDelSistema() {
        val c = ControlDeBloqueo(activadoAlAbrir = true)
        assertTrue(c.alAbrir())
        assertFalse(ControlDeBloqueo(activadoAlAbrir = false).alAbrir())
    }
}
