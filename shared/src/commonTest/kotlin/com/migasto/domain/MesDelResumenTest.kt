package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Nuevo: en Flutter el inicio solo mostraba el mes en curso. El diseño (mg-01) trae «Octubre ⌄»; aquí se
// define qué hace: elegir uno de los meses con movimientos y ver su resumen.
class MesDelResumenTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 15, hora = 12, minuto = 0, mes = 10)
    private val octubre = MesElegido(2026, 10)
    private val septiembre = MesElegido(2026, 9)

    private fun mov(id: String, monto: Double, tipo: TipoMovimiento, mes: Int, anio: Int = 2026, estado: EstadoMovimiento = EstadoMovimiento.Confirmado) =
        Movimiento(
            id = id, amount = monto, merchant = id, category = Categoria.Otros, source = PaymentSource.Manual,
            date = fecha(dia = 10, mes = mes, anio = anio), tipo = tipo, estado = estado,
        )

    private val movs = listOf(
        mov("sueldo-oct", 2800.0, TipoMovimiento.Ingreso, mes = 10),
        mov("tambo", 144.7, TipoMovimiento.Gasto, mes = 10),
        mov("sueldo-set", 2500.0, TipoMovimiento.Ingreso, mes = 9),
        mov("renta", 800.0, TipoMovimiento.Gasto, mes = 9),
        mov("venta", 230.0, TipoMovimiento.Ingreso, mes = 8, estado = EstadoMovimiento.Pendiente),
    )

    @Test
    fun sinMesElegidoSigueSiendoElMesEnCurso() {
        assertEquals(calcularResumenDelMes(movs, 1500.0, ahora, utc, octubre), calcularResumenDelMes(movs, 1500.0, ahora, utc))
    }

    @Test
    fun unMesAnteriorSumaSoloSusMovimientos() {
        val r = calcularResumenDelMes(movs, 1500.0, ahora, utc, septiembre)
        assertEquals(2500.0, r.ingresos)
        assertEquals(800.0, r.gastos)
        assertEquals(1700.0, r.saldo)
    }

    @Test
    fun enUnMesTerminadoElMesYaPasoPorCompleto() {
        assertEquals(1.0, calcularResumenDelMes(movs, 1500.0, ahora, utc, septiembre).fraccionDelMes)
        assertEquals(0.5, calcularResumenDelMes(movs, 1500.0, ahora, utc, octubre).fraccionDelMes, 0.05)
    }

    @Test
    fun loPorConfirmarEsDeTodosLosMesesAunqueSeMireOtro() {
        val r = calcularResumenDelMes(movs, 1500.0, ahora, utc, septiembre)
        assertEquals(230.0, r.porConfirmar)
        assertEquals(1, r.cantidadPorConfirmar)
    }

    @Test
    fun losMesesParaElegirVanDelMasNuevoAlMasViejoYIncluyenElActual() {
        assertEquals(listOf(octubre, septiembre, MesElegido(2026, 8)), mesesParaElegir(movs, ahora, utc))
    }

    @Test
    fun siNoHayMovimientosSoloOfreceElMesActual() {
        assertEquals(listOf(octubre), mesesParaElegir(emptyList(), ahora, utc))
    }

    @Test
    fun unMesSinMovimientosEnMedioNoSeOfrece() {
        val salteado = listOf(mov("a", 10.0, TipoMovimiento.Gasto, mes = 10), mov("b", 10.0, TipoMovimiento.Gasto, mes = 7))
        assertEquals(listOf(octubre, MesElegido(2026, 7)), mesesParaElegir(salteado, ahora, utc))
    }

    @Test
    fun cruzaElAnioYSoloOfreceLosUltimosDoce() {
        val muchos = (1..15).map { mov("m$it", 1.0, TipoMovimiento.Gasto, mes = ((it - 1) % 12) + 1, anio = 2025 + (it - 1) / 12) }
        val ofrecidos = mesesParaElegir(muchos + movs, ahora, utc)
        assertEquals(12, ofrecidos.size)
        assertEquals(octubre, ofrecidos.first())
        assertEquals(MesElegido(2025, 7), ofrecidos.last())
        assertTrue(ofrecidos.zipWithNext().all { (a, b) -> a.anio * 12 + a.mes > b.anio * 12 + b.mes })
    }

    @Test
    fun elNombreLlevaElAnioSoloSiNoEsElDeEsteAnio() {
        assertEquals("Octubre", nombreDelMesElegido(octubre, ahora, utc))
        assertEquals("Septiembre", nombreDelMesElegido(septiembre, ahora, utc))
        assertEquals("Diciembre 2025", nombreDelMesElegido(MesElegido(2025, 12), ahora, utc))
    }

    @Test
    fun losTextosDelSaldoYDeLaListaCambianEnUnMesPasado() {
        assertEquals("Te quedan de este mes", textoDelSaldoDelResumen(octubre, ahora, utc))
        assertEquals("Te quedaron en septiembre", textoDelSaldoDelResumen(septiembre, ahora, utc))
        assertEquals("Te quedaron en diciembre de 2025", textoDelSaldoDelResumen(MesElegido(2025, 12), ahora, utc))
        assertEquals("Hoy", tituloDeLaListaDelResumen(octubre, ahora, utc))
        assertEquals("Últimos de septiembre", tituloDeLaListaDelResumen(septiembre, ahora, utc))
        assertEquals("Todavía no hay movimientos hoy.", textoSinMovimientosEnElResumen(octubre, ahora, utc))
        assertEquals("No hay movimientos en septiembre.", textoSinMovimientosEnElResumen(septiembre, ahora, utc))
    }
}
