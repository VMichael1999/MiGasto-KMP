package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: en Android esto vivía en ReportesViewModel (qué período se mira y observar los movimientos). Ahora lo
// comparten Android e iPhone. Los datos son los del diseño: octubre deja S/ 2,655.30, S/ 780.70 más que septiembre.
class SeguimientoDeReportesTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-seguimiento-reportes.db"
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 12, minuto = 0, mes = 10)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun mov(id: String, monto: Double, tipo: TipoMovimiento, mes: Int, dia: Int = 5) = Movimiento(
        id = id, amount = monto, merchant = id, category = Categoria.Compras, source = PaymentSource.Efectivo,
        date = fecha(dia = dia, mes = mes), tipo = tipo, estado = EstadoMovimiento.Confirmado,
    )

    private fun probar(bloque: suspend (SeguimientoDeReportes) -> Unit) = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            listOf(
                mov("sueldo-oct", 2800.0, TipoMovimiento.Ingreso, 10),
                mov("gastos-oct", 144.7, TipoMovimiento.Gasto, 10),
                mov("sueldo-set", 2500.0, TipoMovimiento.Ingreso, 9),
                mov("gastos-set", 625.4, TipoMovimiento.Gasto, 9),
            ).forEach(base.movimientos::guardar)
            bloque(SeguimientoDeReportes(base, { ahora }, utc))
        }
    }

    @Test
    fun empiezaEnElMesEnCursoConSusMontos() = probar { s ->
        val p = s.estado.first()
        assertEquals(ModoDeReporte.Mes, p.modo)
        assertEquals("Octubre 2026", p.reporte.etiquetaDelPeriodo)
        assertEquals(2655.3, p.reporte.saldo, 1e-9)
        assertFalse(p.puedeAvanzar)
        assertTrue(p.puedeRetroceder)
    }

    @Test
    fun laComparacionDiceCuantoMasQueElMesAnterior() = probar { s ->
        val p = s.estado.first()
        assertEquals("S/ 780.70 más que en septiembre", p.textoDeLaComparacion)
        assertEquals("S/ •••• más que en septiembre", p.textoDeLaComparacionOculta)
    }

    @Test
    fun siNoHayPeriodoAnteriorNoHayComparacion() = probar { s ->
        s.desplazar(-1)
        s.desplazar(-1) // agosto: no hay nada, y julio tampoco
        val p = s.estado.first()
        assertNull(p.textoDeLaComparacion)
        assertNull(p.textoDeLaComparacionOculta)
    }

    @Test
    fun desplazarVaAlMesAnteriorYSePuedeVolver() = probar { s ->
        s.desplazar(-1)
        val septiembre = s.estado.first()
        assertEquals("Septiembre 2026", septiembre.reporte.etiquetaDelPeriodo)
        assertTrue(septiembre.puedeAvanzar)
        s.desplazar(1)
        assertEquals("Octubre 2026", s.estado.first().reporte.etiquetaDelPeriodo)
    }

    @Test
    fun elModoAnioSumaTodoElAnio() = probar { s ->
        s.elegirModo(ModoDeReporte.Anio)
        val p = s.estado.first()
        assertEquals(ModoDeReporte.Anio, p.modo)
        assertEquals("2026", p.reporte.etiquetaDelPeriodo)
        assertEquals(5300.0, p.reporte.ingresos, 1e-9)
    }

    @Test
    fun elRangoSeEligeConFechasEnTexto() = probar { s ->
        s.elegirRango("2026-09-01", "2026-09-30")
        val p = s.estado.first()
        assertEquals(ModoDeReporte.Rango, p.modo)
        assertEquals("2026-09-01", p.desde)
        assertEquals("2026-09-30", p.hasta)
        assertEquals(2500.0, p.reporte.ingresos, 1e-9)
        assertFalse(p.puedeRetroceder)
        assertFalse(p.puedeAvanzar)
    }

    @Test
    fun unaFechaQueNoEsFechaNoCambiaNada() = probar { s ->
        s.elegirRango("mañana", "2026-09-30")
        s.elegirRango("2026-09-01", "")
        assertEquals(ModoDeReporte.Mes, s.estado.first().modo)
    }

    @Test
    fun lasOpcionesDeModoTraenSuTexto() = probar { s ->
        val p = s.estado.first()
        assertEquals(listOf("Mes", "Año", "Rango"), p.opcionesDeModo.map { it.texto })
        assertEquals(listOf(ModoDeReporte.Mes, ModoDeReporte.Anio, ModoDeReporte.Rango), p.opcionesDeModo.map { it.modo })
        assertNotNull(p.seleccion)
    }

    @Test
    fun losTextosFijos() {
        assertEquals("Reportes", TextosDeReportes.TITULO)
        assertEquals("Cambiar", TextosDeReportes.CAMBIAR)
        assertEquals("Período anterior", TextosDeReportes.PERIODO_ANTERIOR)
        assertEquals("Período siguiente", TextosDeReportes.PERIODO_SIGUIENTE)
        assertEquals("Ingresos", TextosDeReportes.INGRESOS)
        assertEquals("Gastos", TextosDeReportes.GASTOS)
        assertEquals("Aceptar", TextosDeReportes.ACEPTAR)
        assertEquals("Cancelar", TextosDeReportes.CANCELAR)
    }
}
