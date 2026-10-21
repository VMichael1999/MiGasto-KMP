package com.migasto.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.migasto.android.resumen.ResumenContenido
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.AvisoDeConfiguracion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** El Resumen con los datos del diseño: lo que se ve, el modo de montos ocultos y los toques. */
class ResumenPantallaTest {
    @get:Rule
    val regla = createComposeRule()

    private var alternados = 0
    private var verTodos = 0
    private var buscar = 0
    private val abiertos = mutableListOf<String>()

    private var actuados = 0
    private var pospuestos = 0

    private fun mostrar(oculto: Boolean = false, presupuesto: Boolean = true, aviso: AvisoDeConfiguracion? = null) {
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                ResumenContenido(
                    Muestras.resumen(oculto, presupuesto), { alternados++ }, { verTodos++ }, { buscar++ }, { abiertos += it },
                    aviso = aviso, alActuarSobreAviso = { actuados++ }, alPosponerAviso = { pospuestos++ },
                )
            }
        }
    }

    @Test
    fun muestraLosMontosDelDiseno() {
        mostrar()
        regla.onNodeWithText("Octubre").assertIsDisplayed()
        regla.onNodeWithText("Te quedan de este mes").assertIsDisplayed()
        regla.onNodeWithText("2,655.30").assertIsDisplayed()
        regla.onNodeWithText("S/ 2,800.00").assertIsDisplayed()
        regla.onNodeWithText("S/ 144.70").assertIsDisplayed()
        regla.onNodeWithText("+ S/ 230.00").assertIsDisplayed()
        regla.onNodeWithText("Gastos: vas bien").assertIsDisplayed()
        regla.onNodeWithText("10 % de S/ 1,500").assertIsDisplayed()
    }

    @Test
    fun muestraLosMovimientosDeHoyConSuFuenteYHora() {
        mostrar()
        regla.onNodeWithText("Carlos").assertIsDisplayed()
        regla.onNodeWithText("Plin · 11:40").assertIsDisplayed()
        regla.onNodeWithText("Barbería El Patrón").assertIsDisplayed()
        regla.onNodeWithText("Google Wallet · 10:20").assertIsDisplayed()
        regla.onNodeWithText("S/ 89.20").assertIsDisplayed()
    }

    @Test
    fun conMontosOcultosNoSeVeNingunMonto() {
        mostrar(oculto = true)
        regla.onNodeWithText("2,655.30").assertDoesNotExist()
        regla.onNodeWithText("S/ 2,800.00").assertDoesNotExist()
        regla.onNodeWithText("S/ 89.20").assertDoesNotExist()
        regla.onNodeWithText("10 % de S/ 1,500").assertDoesNotExist()
        regla.onNodeWithContentDescription("Mostrar montos").assertIsDisplayed()
    }

    @Test
    fun sinPresupuestoNoHayTarjetaDePresupuesto() {
        mostrar(presupuesto = false)
        regla.onNodeWithText("Gastos: vas bien").assertDoesNotExist()
    }

    @Test
    fun losBotonesAvisan() {
        mostrar()
        regla.onNodeWithContentDescription("Ocultar montos").performClick()
        regla.onNodeWithContentDescription("Buscar movimientos").performClick()
        regla.onNodeWithText("Ver todos").performClick()
        assertEquals(1, alternados)
        assertEquals(1, buscar)
        assertEquals(1, verTodos)
    }

    @Test
    fun tocarUnaFilaAbreSuDetalle() {
        mostrar()
        regla.onNodeWithText("Minimarket La Esquina").performClick()
        assertEquals(listOf("minimarket"), abiertos)
    }

    @Test
    fun sinAvisoNoHayTarjetaDeConfiguracion() {
        mostrar()
        regla.onNodeWithText("Ahora no").assertDoesNotExist()
    }

    @Test
    fun elAvisoDeBateriaSePuedePosponerOArreglar() {
        mostrar(aviso = AvisoDeConfiguracion.BateriaRestringida)
        regla.onNodeWithText("Evita que el teléfono cierre la lectura").assertIsDisplayed()
        regla.onNodeWithText("Ahora no").performClick()
        regla.onNodeWithText("Ajustar").performClick()
        assertEquals(1, pospuestos)
        assertEquals(1, actuados)
    }

    @Test
    fun laLecturaApagadaSoloSePuedeActivar() {
        mostrar(aviso = AvisoDeConfiguracion.LecturaApagada)
        regla.onNodeWithText("La lectura de pagos está apagada").assertIsDisplayed()
        regla.onNodeWithText("Ahora no").assertDoesNotExist()
        regla.onNodeWithText("Activar").performClick()
        assertEquals(1, actuados)
    }
}
