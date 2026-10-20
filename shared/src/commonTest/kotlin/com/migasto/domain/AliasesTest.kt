package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals

// Portado de test/alias_test.dart, grupo «alias». Los tests «alias en la app»
// dependen de los providers y del respaldo: se portan con esas partes.
class AliasesTest {
    @Test
    fun laClaveIgnoraMayusculasYEspaciosDeMas() {
        assertEquals("michael anthony valdiviezo", claveDeAlias("  MICHAEL   ANTHONY  Valdiviezo "))
        assertEquals("ñandú pérez", claveDeAlias("Ñandú Pérez"))
    }

    @Test
    fun seMuestraElAliasSiExisteYElOriginalSiNo() {
        val alias = mapOf("michael anthony valdiviezo maza" to "Michael")
        assertEquals("Michael", nombreVisible("MICHAEL ANTHONY VALDIVIEZO MAZA", alias))
        assertEquals("Michael", nombreVisible("Michael  Anthony Valdiviezo Maza", alias))
        assertEquals("Tambo Surco", nombreVisible("Tambo Surco", alias))
        assertEquals("X", nombreVisible("X", mapOf("x" to "  ")))
    }

    @Test
    fun elAliasSeMuestraSinLosEspaciosDeLosBordes() {
        assertEquals("Mari", nombreVisible("Maria Lopez", mapOf("maria lopez" to "  Mari ")))
    }

    @Test
    fun tabuladoresYSaltosDeLineaTambienSeJuntanEnUnEspacio() {
        assertEquals("a b c", claveDeAlias("A\tB\nC"))
    }
}
