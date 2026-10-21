package com.migasto.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.migasto.android.bloqueo.Autenticador
import com.migasto.android.bloqueo.PuertaDeBloqueo
import com.migasto.android.tema.MiGastoTheme
import com.migasto.domain.TextosDelBloqueo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** La puerta de bloqueo (port de los tests de LockGate de Flutter), con un autenticador falso. */
class PuertaDeBloqueoTest {
    @get:Rule
    val regla = createAndroidComposeRule<ComponentActivity>()

    private class Falso(var resultado: Boolean) : Autenticador {
        var pedidos = 0
        val motivos = mutableListOf<String>()
        override fun disponible() = true
        override suspend fun autenticar(motivo: String): Boolean {
            pedidos++
            motivos += motivo
            return resultado
        }
    }

    private fun mostrar(activado: Boolean?, autenticador: Autenticador) {
        regla.setContent {
            MiGastoTheme(oscuro = false) {
                PuertaDeBloqueo(activado, autenticador) { Text("Contenido secreto") }
            }
        }
    }

    @Test
    fun sinBloqueoMuestraElContenidoYNoPideNada() {
        val falso = Falso(true)
        mostrar(false, falso)
        regla.onNodeWithText("Contenido secreto").assertIsDisplayed()
        assertEquals(0, falso.pedidos)
    }

    @Test
    fun mientrasNoSabeSiEstaActivadoNoMuestraNada() {
        mostrar(null, Falso(true))
        regla.onNodeWithText("Contenido secreto").assertDoesNotExist()
        regla.onNodeWithText(TextosDelBloqueo.TITULO).assertDoesNotExist()
    }

    @Test
    fun alAbrirPideDesbloquearYConLaHuellaCorrectaMuestraElContenido() {
        val falso = Falso(true)
        mostrar(true, falso)
        regla.waitForIdle()
        assertEquals(listOf(TextosDelBloqueo.MOTIVO_AL_ABRIR), falso.motivos)
        regla.onNodeWithText(TextosDelBloqueo.TITULO).assertDoesNotExist()
        regla.onNodeWithText("Contenido secreto").assertIsDisplayed()
    }

    @Test
    fun siFallaSeQuedaBloqueadaYElBotonVuelveAPedir() {
        val falso = Falso(false)
        mostrar(true, falso)
        regla.waitForIdle()
        regla.onNodeWithText(TextosDelBloqueo.TITULO).assertIsDisplayed()
        regla.onNodeWithText(TextosDelBloqueo.EXPLICACION).assertIsDisplayed()
        assertEquals(1, falso.pedidos)

        falso.resultado = true
        regla.onNodeWithContentDescription(TextosDelBloqueo.BOTON).performClick()
        regla.waitForIdle()
        assertEquals(2, falso.pedidos)
        regla.onNodeWithText(TextosDelBloqueo.TITULO).assertDoesNotExist()
    }

    @Test
    fun volverDeSegundoPlanoAntesDeLaGraciaNoPideOtraVez() {
        val falso = Falso(true)
        mostrar(true, falso)
        regla.waitForIdle()
        val escenario = regla.activityRule.scenario
        escenario.moveToState(Lifecycle.State.CREATED)
        escenario.moveToState(Lifecycle.State.RESUMED)
        regla.waitForIdle()
        assertEquals(1, falso.pedidos)
        regla.onNodeWithText("Contenido secreto").assertIsDisplayed()
    }
}
