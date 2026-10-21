package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Port de la lógica de lib/presentation/onboarding/onboarding_screen.dart (qué paso toca, qué dice el botón,
// los textos del aviso de privacidad) y de la marca `onboarding_complete`.
class BienvenidaTest {
    private val nada = PermisosDeBienvenida(lectura = false, ventana = false, avisos = false, bateria = false)

    @Test
    fun sinNadaActivadoPrimeroPideLaLectura() {
        assertEquals(PasoDeBienvenida.Lectura, siguientePasoDeBienvenida(nada))
        assertEquals("Aceptar y activar la lectura", textoDelBotonDeBienvenida(nada))
    }

    @Test
    fun conLaLecturaPideLaVentanaFlotante() {
        val p = nada.copy(lectura = true)
        assertEquals(PasoDeBienvenida.Ventana, siguientePasoDeBienvenida(p))
        assertEquals("Permitir la ventana flotante", textoDelBotonDeBienvenida(p))
    }

    @Test
    fun conLaVentanaPideLosAvisos() {
        val p = nada.copy(lectura = true, ventana = true)
        assertEquals(PasoDeBienvenida.Avisos, siguientePasoDeBienvenida(p))
        assertEquals("Permitir los avisos", textoDelBotonDeBienvenida(p))
    }

    @Test
    fun conLosAvisosPideQuitarLaRestriccionDeBateria() {
        val p = nada.copy(lectura = true, ventana = true, avisos = true)
        assertEquals(PasoDeBienvenida.Bateria, siguientePasoDeBienvenida(p))
        assertEquals("Quitar la restricción de batería", textoDelBotonDeBienvenida(p))
    }

    @Test
    fun conTodoActivadoElBotonDiceEmpezar() {
        val p = PermisosDeBienvenida(lectura = true, ventana = true, avisos = true, bateria = true)
        assertEquals(PasoDeBienvenida.Terminar, siguientePasoDeBienvenida(p))
        assertEquals("Empezar", textoDelBotonDeBienvenida(p))
    }

    @Test
    fun siguePidiendoLaLecturaAunqueOtrosPermisosYaEstenDados() {
        val p = nada.copy(ventana = true, avisos = true, bateria = true)
        assertEquals(PasoDeBienvenida.Lectura, siguientePasoDeBienvenida(p))
    }

    @Test
    fun elBotonDeAbajoDejaUsarSoloElRegistroManualHastaQueSeActivaLaLectura() {
        assertEquals("Usar solo registro manual", textoDelBotonSecundarioDeBienvenida(nada))
        assertEquals("Terminar después", textoDelBotonSecundarioDeBienvenida(nada.copy(lectura = true)))
    }

    @Test
    fun losCuatroPasosLlevanSuNumeroSuTituloYSuPista() {
        val pasos = pasosDeBienvenida(nada.copy(lectura = true))
        assertEquals(listOf(1, 2, 3, 4), pasos.map { it.numero })
        assertEquals(
            listOf(
                "Activa la lectura de pagos",
                "Permite la ventana flotante",
                "Recibe un aviso de cada pago",
                "Evita que el teléfono la duerma",
            ),
            pasos.map { it.titulo },
        )
        assertEquals(listOf(true, false, false, false), pasos.map { it.listo })
        assertEquals("En Acceso a notificaciones, activa MiGasto", pasos[0].pista)
        assertEquals("Para confirmar cada pago al instante. Es opcional.", pasos[1].pista)
        assertEquals("Una notificación cuando detecta un pago con la pantalla bloqueada.", pasos[2].pista)
        assertEquals("En Batería, elige «No restringido». Así no se pierden pagos.", pasos[3].pista)
    }

    @Test
    fun laDescripcionParaElLectorDePantallaDiceListoOLaPista() {
        val pasos = pasosDeBienvenida(nada.copy(lectura = true))
        assertEquals("Paso 1: Activa la lectura de pagos. Listo", pasos[0].descripcion)
        assertEquals("Paso 2: Permite la ventana flotante. Para confirmar cada pago al instante. Es opcional.", pasos[1].descripcion)
    }

    @Test
    fun elAvisoDePrivacidadDiceQueLeeYQueNo() {
        assertEquals("Registra tus pagos sin escribirlos.", TextosDeBienvenida.TITULO)
        assertEquals(
            "MiGasto anota los pagos de Yape, Plin y Google Wallet por ti y te deja confirmar cada uno.",
            TextosDeBienvenida.SUBTITULO,
        )
        assertEquals("Qué lee la app y qué no", TextosDeBienvenida.PRIVACIDAD_TITULO)
        assertEquals("Lee: ", TextosDeBienvenida.LEE)
        assertEquals("No lee: ", TextosDeBienvenida.NO_LEE)
        assertEquals("tus mensajes, fotos, contactos ni otras apps.", TextosDeBienvenida.NO_LEE_DETALLE)
        assertEquals("Dónde se guarda: ", TextosDeBienvenida.GUARDA)
        assertEquals("solo en tu teléfono. No se envía a ningún servidor.", TextosDeBienvenida.GUARDA_DETALLE)
        assertTrue(TextosDeBienvenida.LEE_DETALLE.startsWith("las notificaciones y la pantalla de Yape, Plin y Google Wallet"))
    }

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-bienvenida.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun laBienvenidaSeMuestraHastaQueSeTermina() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            assertFalse(base.ajustes.bienvenidaCompleta())
            base.ajustes.establecerBienvenidaCompleta()
            assertTrue(base.ajustes.bienvenidaCompleta())
        }
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            assertTrue(base.ajustes.bienvenidaCompleta())
            assertEquals("true", base.ajustes.leerCrudo("onboarding_complete"))
        }
    }
}
