package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Port de los textos de _SetupBanner (lib/presentation/dashboard/dashboard_screen.dart).
class AvisoDeConfiguracionTextosTest {
    @Test
    fun laLecturaApagadaNoSePuedePosponer() {
        val t = textosDelAvisoDeConfiguracion(AvisoDeConfiguracion.LecturaApagada)
        assertEquals("La lectura de pagos está apagada", t.titulo)
        assertEquals("Activar", t.accion)
        assertFalse(t.sePuedePosponer)
        assertTrue("Yape, Plin ni Wallet" in t.cuerpo)
    }

    @Test
    fun losAvisosApagados() {
        val t = textosDelAvisoDeConfiguracion(AvisoDeConfiguracion.AvisosApagados)
        assertEquals("Activa los avisos de MiGasto", t.titulo)
        assertEquals("Activar", t.accion)
        assertTrue(t.sePuedePosponer)
    }

    @Test
    fun laBateriaRestringida() {
        val t = textosDelAvisoDeConfiguracion(AvisoDeConfiguracion.BateriaRestringida)
        assertEquals("Evita que el teléfono cierre la lectura", t.titulo)
        assertEquals("Ajustar", t.accion)
        assertTrue(t.sePuedePosponer)
        assertEquals("${t.titulo}. ${t.cuerpo}", t.descripcion)
    }

    @Test
    fun cadaAvisoTieneSuPropiaPausa() {
        assertEquals(ClaveDePausa.Bateria, claveDePausa(AvisoDeConfiguracion.BateriaRestringida))
        assertEquals(ClaveDePausa.Avisos, claveDePausa(AvisoDeConfiguracion.AvisosApagados))
        assertEquals(null, claveDePausa(AvisoDeConfiguracion.LecturaApagada))
    }
}
