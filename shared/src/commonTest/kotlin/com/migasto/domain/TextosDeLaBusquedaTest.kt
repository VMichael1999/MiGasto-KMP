package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals

// La búsqueda de la barra de iPhone usa la pantalla de Movimientos con su texto de búsqueda (filtrar por nombre, con chips
// y grupos por día); aquí solo se fijan sus textos, que son los mismos de Android.
class TextosDeLaBusquedaTest {
    @Test
    fun lasEtiquetasDeLaBusqueda() {
        assertEquals("Buscar", TextosDeLaBusqueda.TITULO)
        assertEquals("Buscar por nombre", TextosDeLaBusqueda.PLACEHOLDER)
    }
}
