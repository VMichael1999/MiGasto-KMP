package com.migasto.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.migasto.android.bienvenida.BienvenidaContenido
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.PermisosDeBienvenida
import com.migasto.domain.TextosDeBienvenida
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** La bienvenida (port del test de widget de Flutter: qué lee la app y entrar solo con registro manual). */
class BienvenidaPantallaTest {
    @get:Rule
    val regla = createComposeRule()

    private var principales = 0
    private var secundarios = 0

    private val nada = PermisosDeBienvenida(lectura = false, ventana = false, avisos = false, bateria = false)

    private fun mostrar(permisos: PermisosDeBienvenida = nada) {
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                BienvenidaContenido(permisos, { principales++ }, { secundarios++ })
            }
        }
    }

    @Test
    fun muestraQueLeeLaAppYQueNo() {
        mostrar()
        regla.onNodeWithText(TextosDeBienvenida.TITULO).assertIsDisplayed()
        regla.onNodeWithText(TextosDeBienvenida.PRIVACIDAD_TITULO).assertIsDisplayed()
        regla.onNodeWithText("tus mensajes, fotos, contactos ni otras apps.", substring = true).assertIsDisplayed()
        regla.onNodeWithContentDescription("MiGasto").assertIsDisplayed()
    }

    @Test
    fun sinPermisosSePuedeUsarSoloElRegistroManual() {
        mostrar()
        regla.onNodeWithText("Usar solo registro manual").performClick()
        assertEquals(1, secundarios)
        assertEquals(0, principales)
    }

    @Test
    fun elBotonPrincipalDiceElPasoQueToca() {
        mostrar()
        regla.onNodeWithText("Aceptar y activar la lectura").performClick()
        assertEquals(1, principales)
    }

    @Test
    fun conLaLecturaActivadaElPasoUnoDiceListoYElBotonCambia() {
        mostrar(nada.copy(lectura = true))
        regla.onNodeWithContentDescription("Paso 1: Activa la lectura de pagos. Listo").verEnPantalla()
        regla.onNodeWithText("Permitir la ventana flotante").assertIsDisplayed()
        regla.onNodeWithText("Terminar después").assertIsDisplayed()
    }

    @Test
    fun conTodoActivadoElBotonDiceEmpezar() {
        mostrar(PermisosDeBienvenida(lectura = true, ventana = true, avisos = true, bateria = true))
        regla.onNodeWithText("Empezar").performClick()
        assertEquals(1, principales)
    }
}
