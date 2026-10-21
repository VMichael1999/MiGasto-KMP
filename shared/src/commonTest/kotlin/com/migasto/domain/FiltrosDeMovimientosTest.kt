package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Port de la hoja «Filtrar movimientos» de lib/presentation/expenses/expenses_screen.dart.
class FiltrosDeMovimientosTest {
    private val propias = listOf(
        CategoriaPropia("g1", "Barbería", "scissors", TipoMovimiento.Gasto),
        CategoriaPropia("i1", "Clases", "escuela", TipoMovimiento.Ingreso),
    )

    @Test
    fun lasCategoriasDeLaHojaDependenDelTipo() {
        assertEquals(Categoria.gastos, OpcionesDeFiltro.categorias(FiltroDeTipo.Todo))
        assertEquals(Categoria.gastos, OpcionesDeFiltro.categorias(FiltroDeTipo.Gastos))
        assertEquals(Categoria.gastos, OpcionesDeFiltro.categorias(FiltroDeTipo.PorConfirmar))
        assertEquals(Categoria.ingresos, OpcionesDeFiltro.categorias(FiltroDeTipo.Ingresos))
    }

    @Test
    fun lasCategoriasPropiasSonLasDelMismoTipo() {
        assertEquals(listOf("Barbería"), OpcionesDeFiltro.propias(FiltroDeTipo.Todo, propias).map { it.nombre })
        assertEquals(listOf("Barbería"), OpcionesDeFiltro.propias(FiltroDeTipo.Gastos, propias).map { it.nombre })
        assertEquals(listOf("Clases"), OpcionesDeFiltro.propias(FiltroDeTipo.Ingresos, propias).map { it.nombre })
    }

    @Test
    fun lasFuentesQueSePuedenFiltrar() {
        assertEquals(
            listOf("Yape", "Plin", "Google Wallet", "Tarjeta", "Efectivo", "Manual"),
            OpcionesDeFiltro.fuentes.map { it.etiqueta },
        )
    }

    @Test
    fun elRangoVa0a500DeDiezEnDiez() {
        assertEquals(0.0, OpcionesDeFiltro.MONTO_MINIMO)
        assertEquals(500.0, OpcionesDeFiltro.MONTO_MAXIMO)
        assertEquals(49, OpcionesDeFiltro.PASOS_INTERMEDIOS)
        assertEquals("S/ 0 – S/ 500", OpcionesDeFiltro.textoDelRango(0.0, 500.0))
        assertEquals("S/ 40 – S/ 120", OpcionesDeFiltro.textoDelRango(40.0, 120.0))
    }

    @Test
    fun aplicarLaHojaPoneCategoriaFuenteYRango() {
        val base = FiltrosDeMovimientos(tipo = FiltroDeTipo.Gastos, busqueda = "tambo")
        val f = base.conLaHoja("compras", PaymentSource.Yape, 10.0..200.0)
        assertEquals("compras", f.categoria)
        assertEquals(PaymentSource.Yape, f.fuente)
        assertEquals(10.0, f.montoMinimo)
        assertEquals(200.0, f.montoMaximo)
        // El tipo y la búsqueda no se tocan.
        assertEquals(FiltroDeTipo.Gastos, f.tipo)
        assertEquals("tambo", f.busqueda)
        assertEquals(3, f.cantidadDeFiltrosActivos)
    }

    @Test
    fun siNoSeEligioRangoNoHayRango() {
        val f = FiltrosDeMovimientos().conLaHoja(null, null, null)
        assertNull(f.montoMinimo)
        assertNull(f.montoMaximo)
        assertEquals(0, f.cantidadDeFiltrosActivos)
    }

    @Test
    fun limpiarLaHojaQuitaSoloLoDeLaHoja() {
        val f = FiltrosDeMovimientos(FiltroDeTipo.Ingresos, "ana", "venta", PaymentSource.Plin, 1.0, 2.0).sinLoDeLaHoja()
        assertNull(f.categoria)
        assertNull(f.fuente)
        assertNull(f.montoMinimo)
        assertNull(f.montoMaximo)
        assertEquals(FiltroDeTipo.Ingresos, f.tipo)
        assertEquals("ana", f.busqueda)
    }

    @Test
    fun elRangoActualDeLaHojaSeLeeDeLosFiltros() {
        assertNull(FiltrosDeMovimientos().rangoDeLaHoja)
        assertEquals(10.0..200.0, FiltrosDeMovimientos(montoMinimo = 10.0, montoMaximo = 200.0).rangoDeLaHoja)
        // Con un solo extremo puesto, el otro es el de la hoja.
        assertEquals(10.0..500.0, FiltrosDeMovimientos(montoMinimo = 10.0).rangoDeLaHoja)
        assertEquals(0.0..80.0, FiltrosDeMovimientos(montoMaximo = 80.0).rangoDeLaHoja)
    }
}
