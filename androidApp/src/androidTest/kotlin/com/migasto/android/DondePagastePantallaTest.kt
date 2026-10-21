package com.migasto.android

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.migasto.android.donde.DondePagasteContenido
import com.migasto.android.donde.DondePagasteUi
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.Categoria
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.PeriodoDeUbicaciones
import com.migasto.domain.TextosDeDondePagaste
import com.migasto.domain.ZonaDePagos
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

/** «Dónde pagaste»: las zonas, el período, los montos ocultos y el aviso cuando no hay pagos (el mapa se reemplaza). */
class DondePagastePantallaTest {
    @get:Rule
    val regla = createComposeRule()

    private val periodos = mutableListOf<PeriodoDeUbicaciones>()
    private var volvio = 0

    private fun mov(id: String, comercio: String, monto: Double) = Movimiento(
        id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = PaymentSource.Tarjeta,
        date = Clock.System.now(), latitud = -12.09, longitud = -77.03, lugar = "Av. Arequipa 3120, San Isidro",
    )

    private val zonas = listOf(ZonaDePagos("San Isidro", listOf(mov("a", "Wong", 80.0), mov("b", "Tambo", 25.5))))

    private fun mostrar(zonas: List<ZonaDePagos> = this.zonas, oculto: Boolean = false, periodo: PeriodoDeUbicaciones = PeriodoDeUbicaciones.Semana) {
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                DondePagasteContenido(
                    ui = DondePagasteUi(periodo, zonas, emptyMap(), oculto),
                    alElegirPeriodo = { periodos += it },
                    alVolver = { volvio++ },
                    mapa = { _, modificador -> Text("mapa de prueba", modificador) },
                )
            }
        }
    }

    @Test
    fun muestraElTituloLasZonasYElPie() {
        mostrar()
        regla.onNodeWithText(TextosDeDondePagaste.TITULO).assertIsDisplayed()
        regla.onNodeWithText("mapa de prueba").assertIsDisplayed()
        regla.onNodeWithContentDescription("San Isidro, 2 pagos, S/ 105.50. Wong y 1 más").assertIsDisplayed()
        regla.onNodeWithText(TextosDeDondePagaste.PIE).assertIsDisplayed()
    }

    @Test
    fun elPeriodoSeCambiaConLosTresBotones() {
        mostrar()
        regla.onNodeWithText("Mes").performClick()
        regla.onNodeWithText("Todo").performClick()
        regla.onNodeWithText("Semana").assertIsDisplayed()
        assertEquals(listOf(PeriodoDeUbicaciones.Mes, PeriodoDeUbicaciones.Todo), periodos)
    }

    @Test
    fun sinPagosConUbicacionExplicaComoAgregarla() {
        mostrar(zonas = emptyList())
        regla.onNodeWithText(TextosDeDondePagaste.VACIO).assertIsDisplayed()
    }

    @Test
    fun conLosMontosOcultosNoSeVeElTotal() {
        mostrar(oculto = true)
        regla.onNodeWithContentDescription("San Isidro, 2 pagos. Wong y 1 más").assertIsDisplayed()
        regla.onNodeWithContentDescription("S/ 105.50", substring = true).assertDoesNotExist()
    }

    @Test
    fun elBotonVolverAvisa() {
        mostrar()
        regla.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, volvio)
    }
}
