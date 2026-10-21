package com.migasto.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.migasto.android.movimientos.MovimientosContenido
import com.migasto.android.movimientos.MovimientosUi
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.FiltrosDeMovimientos
import com.migasto.domain.PaymentSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MovimientosPantallaTest {
    @get:Rule
    val regla = createComposeRule()

    private val tipos = mutableListOf<FiltroDeTipo>()
    private val busquedas = mutableListOf<String>()
    private var limpiados = 0
    private val abiertos = mutableListOf<String>()

    private val aplicados = mutableListOf<Triple<String?, PaymentSource?, ClosedFloatingPointRange<Double>?>>()
    private var hojaLimpiada = 0

    private fun mostrar(ui: MovimientosUi = Muestras.movimientos(), abrirBusqueda: Boolean = false) {
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                MovimientosContenido(
                    ui, abrirBusqueda, { tipos += it }, { busquedas += it }, { limpiados++ }, { abiertos += it },
                    alAplicarFiltros = { c, f, r -> aplicados += Triple(c, f, r) }, alLimpiarLaHoja = { hojaLimpiada++ },
                )
            }
        }
    }

    @Test
    fun muestraLosDiasAgrupadosConElTotalDeGastos() {
        mostrar()
        regla.onNodeWithText("Hoy, jueves 1").assertIsDisplayed()
        regla.onNodeWithText("S/ 114.20 en gastos").assertIsDisplayed()
        regla.onNodeWithText("Ayer, miércoles 30").assertIsDisplayed()
        regla.onNodeWithText("Barbería El Patrón").assertIsDisplayed()
    }

    @Test
    fun conMontosOcultosElTotalDelDiaTambienSeOculta() {
        mostrar(Muestras.movimientos(oculto = true))
        regla.onNodeWithText("S/ 114.20 en gastos").assertDoesNotExist()
        regla.onNodeWithText("•••• en gastos").assertIsDisplayed()
    }

    @Test
    fun loschipsDeTipoAvisan() {
        mostrar()
        regla.onNodeWithText("Gastos").performClick()
        regla.onNodeWithText("Ingresos").performClick()
        regla.onNodeWithText("Por confirmar · 2").performClick()
        assertEquals(listOf(FiltroDeTipo.Gastos, FiltroDeTipo.Ingresos, FiltroDeTipo.PorConfirmar), tipos)
    }

    @Test
    fun laLupaAbreLaBusquedaYLoEscritoSeEnvia() {
        mostrar()
        regla.onNodeWithContentDescription("Buscar movimientos").performClick()
        regla.onNode(hasSetTextAction()).performTextInput("carl")
        assertEquals("carl", busquedas.last())
    }

    @Test
    fun puedeAbrirseConLaBusquedaAbierta() {
        mostrar(abrirBusqueda = true)
        regla.onNodeWithText("Buscar por nombre").assertIsDisplayed()
    }

    @Test
    fun tocarUnaFilaLaAbre() {
        mostrar()
        regla.onNodeWithText("Ana").performClick()
        assertEquals(listOf("ana"), abiertos)
    }

    @Test
    fun sinResultadosConFiltroOfreceQuitarlos() {
        mostrar(Muestras.movimientos(busqueda = "zzz", grupos = emptyList(), hayFiltro = true))
        regla.onNodeWithText("No hay movimientos con esos filtros.").assertIsDisplayed()
        regla.onNodeWithText("Quitar filtros").performClick()
        assertEquals(1, limpiados)
    }

    @Test
    fun sinMovimientosNiFiltrosDiceQueTodaviaNoHay() {
        mostrar(Muestras.movimientos(grupos = emptyList()))
        regla.onNodeWithText("Todavía no hay movimientos.").assertIsDisplayed()
        regla.onNodeWithText("Quitar filtros").assertDoesNotExist()
    }

    // La hoja «Filtrar movimientos»

    @Test
    fun elBotonDeFiltrosAbreLaHojaConSusOpciones() {
        mostrar()
        regla.onNodeWithContentDescription("Filtrar movimientos").performClick()
        regla.onNodeWithText("Filtrar movimientos").assertIsDisplayed()
        regla.onNodeWithText("Compras").assertIsDisplayed()
        regla.onNodeWithText("Google Wallet").assertIsDisplayed()
        regla.onNodeWithText("Rango de monto").assertIsDisplayed()
        regla.onNodeWithText("S/ 0 – S/ 500").assertIsDisplayed()
    }

    @Test
    fun aplicarEnviaLaCategoriaLaFuenteYSinRangoNoHayRango() {
        mostrar()
        regla.onNodeWithContentDescription("Filtrar movimientos").performClick()
        regla.onNodeWithText("Compras").performClick()
        regla.onNodeWithText("Yape").performClick()
        regla.onNodeWithText("Aplicar").performClick()
        assertEquals(listOf(Triple("compras", PaymentSource.Yape, null)), aplicados)
    }

    @Test
    fun limpiarLosFiltrosAvisa() {
        mostrar(Muestras.movimientos(filtros = FiltrosDeMovimientos(categoria = "compras")))
        regla.onNodeWithContentDescription("Filtrar movimientos").performClick()
        regla.onNodeWithText("Limpiar filtros").performClick()
        assertEquals(1, hojaLimpiada)
    }

    @Test
    fun laHojaSeAbreConLoQueYaEstabaElegido() {
        mostrar(Muestras.movimientos(filtros = FiltrosDeMovimientos(montoMinimo = 40.0, montoMaximo = 120.0)))
        regla.onNodeWithContentDescription("Filtrar movimientos").performClick()
        regla.onNodeWithText("S/ 40 – S/ 120").assertIsDisplayed()
    }

    @Test
    fun conFiltrosPuestosElBotonMuestraCuantos() {
        mostrar(Muestras.movimientos(filtros = FiltrosDeMovimientos(categoria = "compras", fuente = PaymentSource.Yape)))
        regla.onNodeWithContentDescription("2 filtros puestos").assertIsDisplayed()
    }

    @Test
    fun sinFiltrosNoHayInsignia() {
        mostrar()
        regla.onNodeWithContentDescription("0 filtros puestos").assertDoesNotExist()
    }

    @Test
    fun enIngresosLaHojaOfreceLasCategoriasDeIngreso() {
        mostrar(Muestras.movimientos(tipo = FiltroDeTipo.Ingresos))
        regla.onNodeWithContentDescription("Filtrar movimientos").performClick()
        regla.onNodeWithText("Sueldo").assertIsDisplayed()
        regla.onNodeWithText("Alimentación").assertDoesNotExist()
    }
}
