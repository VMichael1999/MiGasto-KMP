package com.migasto.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.migasto.android.nuevo.NuevoMovimientoContenido
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.FormularioNuevoMovimiento
import com.migasto.domain.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** El flujo de «Nuevo movimiento» con el teclado propio, igual que lo usa el usuario. */
class NuevoMovimientoPantallaTest {
    @get:Rule
    val regla = createComposeRule()

    private var guardados = 0
    private var cerrados = 0
    private val creadas = mutableListOf<Pair<String, String>>()

    private fun mostrar(propias: List<CategoriaPropia> = emptyList()) {
        regla.setContent {
            var f by remember { mutableStateOf(FormularioNuevoMovimiento()) }
            MiGastoTheme(oscuro = false) {
                NuevoMovimientoContenido(
                    f = f, propias = propias, guardando = false,
                    alCerrar = { cerrados++ },
                    alCambiarTipo = { f = f.conTipo(it) },
                    alEscribirConcepto = { f = f.conConcepto(it) },
                    alElegirFuente = { f = f.conFuente(it) },
                    alElegirCategoria = { f = f.conCategoria(it) },
                    alElegirPropia = { f = f.conPropia(it) },
                    alCrearCategoria = { n, i -> creadas += n to i },
                    alPresionar = { f = f.conEntrada(f.entrada.presionar(it)) },
                    alGuardar = { guardados++ },
                )
            }
        }
    }

    private fun teclear(vararg teclas: String) = teclas.forEach { regla.onNodeWithContentDescription(it).performClick() }

    @Test
    fun alAbrirNoSePuedeGuardar() {
        mostrar()
        regla.onNodeWithText("0.00").assertIsDisplayed()
        regla.onNodeWithContentDescription("Guardar gasto").assertIsNotEnabled()
    }

    @Test
    fun elTecladoEscribeElMontoYElBotonLoMuestra() {
        mostrar()
        teclear("2", "5")
        regla.onNodeWithText("25.00").assertIsDisplayed()
        regla.onNodeWithContentDescription("Guardar gasto · S/ 25.00").assertIsNotEnabled() // falta el detalle
    }

    @Test
    fun conMontoYDetalleSePuedeGuardar() {
        mostrar()
        teclear("2", "5", "Punto", "5")
        regla.onNode(hasSetTextAction()).performTextInput("Corte de cabello")
        regla.onNodeWithContentDescription("Guardar gasto · S/ 25.50").assertIsEnabled().performClick()
        assertEquals(1, guardados)
    }

    @Test
    fun borrarQuitaElUltimoDigito() {
        mostrar()
        teclear("1", "2", "3", "Borrar", "Borrar")
        regla.onNodeWithText("1.00").assertIsDisplayed()
    }

    @Test
    fun cambiarAIngresoCambiaElBotonYLasCategorias() {
        mostrar()
        regla.onNodeWithText("Alimentación").verEnPantalla()
        regla.onNodeWithContentDescription("Ingreso").performClick()
        regla.onNodeWithText("Sueldo").assertExists()
        regla.onNodeWithText("Alimentación").assertDoesNotExist()
        teclear("1", "8", "0")
        regla.onNodeWithText("De quién o por qué").assertExists()
        regla.onNodeWithContentDescription("Guardar ingreso · S/ 180.00").assertIsNotEnabled()
    }

    @Test
    fun lasCategoriasPropiasDelTipoAparecen() {
        mostrar(listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto), CategoriaPropia("c2", "Clases", "escuela", TipoMovimiento.Ingreso)))
        regla.onNodeWithText("Barbería").verEnPantalla()
        regla.onNodeWithText("Clases").assertDoesNotExist()
        regla.onNodeWithContentDescription("Ingreso").performClick()
        regla.onNodeWithText("Clases").assertExists()
    }

    @Test
    fun laFuenteSeCambiaDesdeSuHoja() {
        mostrar()
        regla.onNodeWithText("Pagado con Efectivo").verEnPantalla()
        regla.onNodeWithText("Cambiar").performClick()
        regla.onNodeWithText("Yape").performClick()
        regla.onNodeWithText("Pagado con Yape").verEnPantalla()
    }

    @Test
    fun cerrarAvisa() {
        mostrar()
        regla.onNodeWithContentDescription("Cerrar").performClick()
        assertEquals(1, cerrados)
    }
}
