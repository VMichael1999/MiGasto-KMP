package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Port de BigAmount.countUpFromZero (lib/shared/widgets/big_amount.dart): al volver a mostrar los montos, el número sube
// desde 0 hasta su valor en 900 ms con la curva easeOutCubic.
class CuentaAnimadaTest {
    @Test
    fun duraLoMismoQueEnFlutter() {
        assertEquals(900, CuentaAnimada.DURACION_EN_MILISEGUNDOS)
    }

    @Test
    fun laCurvaEmpiezaEnCeroTerminaEnUnoYSubeRapidoAlPrincipio() {
        assertEquals(0.0, CuentaAnimada.progreso(0.0))
        assertEquals(1.0, CuentaAnimada.progreso(1.0))
        // easeOutCubic: 1 - (1 - t)^3
        assertEquals(0.875, CuentaAnimada.progreso(0.5), 1e-9)
        assertTrue(CuentaAnimada.progreso(0.25) > 0.25)
    }

    @Test
    fun fueraDeRangoSeAcota() {
        assertEquals(0.0, CuentaAnimada.progreso(-1.0))
        assertEquals(1.0, CuentaAnimada.progreso(3.0))
    }

    @Test
    fun elTextoSubeDesdeCeroHastaElMonto() {
        assertEquals("0.00", CuentaAnimada.texto(2655.30, 0.0))
        assertEquals("2,655.30", CuentaAnimada.texto(2655.30, 1.0))
        // a la mitad del tiempo ya va en el 87.5 %
        assertEquals("2,323.39", CuentaAnimada.texto(2655.30, 0.5))
    }

    @Test
    fun conSolesLlevaElPrefijo() {
        assertEquals("S/ 0.00", CuentaAnimada.textoConSoles(2800.0, 0.0))
        assertEquals("S/ 2,800.00", CuentaAnimada.textoConSoles(2800.0, 1.0))
    }

    @Test
    fun unSaldoNegativoTambienCuenta() {
        assertEquals("-1,000.00", CuentaAnimada.texto(-1000.0, 1.0))
    }
}
