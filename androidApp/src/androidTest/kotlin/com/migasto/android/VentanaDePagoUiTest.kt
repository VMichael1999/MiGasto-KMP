package com.migasto.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.migasto.android.pagos.VentanaDePagoUi
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.AccionDeVentana
import com.migasto.domain.Categoria
import com.migasto.domain.PagoLeido
import com.migasto.domain.PaymentSource
import com.migasto.domain.SesionDeVentana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** La ventana flotante «Pago detectado»: botones, cuenta regresiva, pausa y deslizar. */
class VentanaDePagoUiTest {
    @get:Rule
    val regla = createComposeRule()

    private val acciones = mutableListOf<AccionDeVentana>()

    private fun gasto() = PagoLeido(32.0, "Café Central", PaymentSource.Yape, esIngreso = false)
    private fun ingreso() = PagoLeido(15.0, "Juan Pérez", PaymentSource.Yape, esIngreso = true)

    private fun mostrar(pago: PagoLeido, guardarSolo: Boolean = !pago.esIngreso, categoria: String = "Alimentación"): SesionDeVentana {
        val sesion = SesionDeVentana(pago, "texto", Categoria.Alimentacion, guardarSolo)
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                VentanaDePagoUi(1L, sesion, pago.peer, categoria, "14:49") { acciones += it }
            }
        }
        regla.waitForIdle()
        return sesion
    }

    @Test
    fun muestraElPagoDeUnGasto() {
        mostrar(gasto())
        regla.onNodeWithText("Yape").assertIsDisplayed()
        regla.onNodeWithText("14:49").assertIsDisplayed()
        regla.onNodeWithText("32.00").assertIsDisplayed()
        regla.onNodeWithText("Alimentación  ›").assertIsDisplayed()
        regla.onNodeWithText("Descartar").assertIsDisplayed()
        regla.onNodeWithText("Editar").assertIsDisplayed()
        regla.onNodeWithText("Guardar").assertIsDisplayed()
    }

    @Test
    fun guardarGuardaConfirmado() {
        mostrar(gasto())
        regla.onNodeWithText("Guardar").performClick()
        assertEquals(listOf(AccionDeVentana.GuardarConfirmado), acciones)
    }

    @Test
    fun descartarNoGuardaNada() {
        mostrar(gasto())
        regla.onNodeWithText("Descartar").performClick()
        assertEquals(listOf(AccionDeVentana.Descartar), acciones)
    }

    @Test
    fun editarYLaCategoriaDejanElPagoPendienteYAbrenLaApp() {
        mostrar(gasto())
        regla.onNodeWithText("Editar").performClick()
        assertEquals(listOf(AccionDeVentana.DejarPendienteYAbrirLaApp), acciones)
    }

    @Test
    fun alAcabarseLaCuentaRegresivaSeGuardaSolo() {
        mostrar(gasto())
        regla.mainClock.advanceTimeBy(3_000)
        assertTrue(acciones.isEmpty())
        regla.mainClock.advanceTimeBy(1_500)
        assertEquals(listOf(AccionDeVentana.GuardarConfirmado), acciones)
    }

    @Test
    fun tocarLaVentanaPausaLaCuentaYNoSeGuarda() {
        mostrar(gasto())
        regla.mainClock.advanceTimeBy(1_000)
        regla.onNodeWithText("32.00").performTouchInput { down(center) }
        regla.mainClock.advanceTimeBy(300)
        regla.onNodeWithText("En pausa").assertIsDisplayed()
        regla.mainClock.advanceTimeBy(10_000)
        assertTrue("con la cuenta en pausa no se guarda solo", acciones.isEmpty())
    }

    @Test
    fun deslizarHaciaArribaDescartaUnGasto() {
        mostrar(gasto())
        regla.onNodeWithText("32.00").performTouchInput { swipeUp(startY = centerY, endY = centerY - 600f) }
        regla.waitForIdle()
        assertEquals(listOf(AccionDeVentana.Descartar), acciones)
    }

    @Test
    fun unIngresoNoTieneCuentaRegresivaYEsperaLaRespuesta() {
        mostrar(ingreso(), categoria = "Transferencia recibida")
        regla.onNodeWithText("Ignorar").assertIsDisplayed()
        regla.onNodeWithText("Guardar ingreso").assertIsDisplayed()
        regla.onNodeWithText("Descartar").assertDoesNotExist()
        regla.mainClock.advanceTimeBy(20_000)
        assertTrue("un ingreso no se guarda solo", acciones.isEmpty())
    }

    @Test
    fun ignorarUnIngresoLoDejaPendiente() {
        mostrar(ingreso(), categoria = "Transferencia recibida")
        regla.onNodeWithText("Ignorar").performClick()
        assertEquals(listOf(AccionDeVentana.DejarPendiente), acciones)
    }

    @Test
    fun unIngresoSinRespuestaQuedaPendienteAlMinuto() {
        mostrar(ingreso(), categoria = "Transferencia recibida")
        regla.mainClock.advanceTimeBy(61_000)
        assertEquals(listOf(AccionDeVentana.DejarPendiente), acciones)
    }

    @Test
    fun conGuardarIngresosAutomaticamenteUnIngresoSigueLaCuenta() {
        mostrar(ingreso(), guardarSolo = true, categoria = "Transferencia recibida")
        regla.mainClock.advanceTimeBy(4_500)
        assertEquals(listOf(AccionDeVentana.GuardarConfirmado), acciones)
    }

    @Test
    fun elBotonPrincipalAnunciaQueSeGuardaSolo() {
        mostrar(gasto())
        regla.onNodeWithContentDescription("Guardar. Se guarda solo en unos segundos").assertIsDisplayed()
    }
}
