package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Port del esquema migasto:// de la app de Flutter (lo usan el widget y el control del Centro de control).
class EnlacesDeLaAppTest {
    @Test
    fun elEnlaceDeNuevoMovimientoEsElDeFlutter() {
        assertEquals("migasto://new", EnlacesDeLaApp.NUEVO_MOVIMIENTO)
    }

    @Test
    fun reconoceElEnlaceDeNuevoMovimiento() {
        assertTrue(EnlacesDeLaApp.esNuevoMovimiento("migasto://new"))
        assertTrue(EnlacesDeLaApp.esNuevoMovimiento("MiGasto://NEW"))
        assertTrue(EnlacesDeLaApp.esNuevoMovimiento("migasto://new/"))
        assertTrue(EnlacesDeLaApp.esNuevoMovimiento("migasto://new?origen=widget"))
    }

    @Test
    fun ignoraLoQueNoEsEseEnlace() {
        assertFalse(EnlacesDeLaApp.esNuevoMovimiento("migasto://settings"))
        assertFalse(EnlacesDeLaApp.esNuevoMovimiento("https://new"))
        assertFalse(EnlacesDeLaApp.esNuevoMovimiento("migasto://newer"))
        assertFalse(EnlacesDeLaApp.esNuevoMovimiento(""))
    }
}
