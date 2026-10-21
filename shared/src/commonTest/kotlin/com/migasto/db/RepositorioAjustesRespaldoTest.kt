package com.migasto.db

import com.migasto.domain.fecha
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositorioAjustesRespaldoTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-ajustes-respaldo.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    @Test
    fun elUltimoRespaldoEmpiezaVacioYSeGuarda() {
        abrir().use { base ->
            assertNull(base.ajustes.ultimoRespaldo())
            val ahora = fecha(dia = 21, hora = 10, minuto = 14, mes = 10)
            base.ajustes.guardarUltimoRespaldo(ahora)
            assertEquals(ahora, base.ajustes.ultimoRespaldo())
        }
    }

    @Test
    fun unValorDanadoDelUltimoRespaldoSeIgnora() {
        abrir().use { base ->
            base.ajustes.guardarCrudo("migasto_last_backup", "ayer")
            assertNull(base.ajustes.ultimoRespaldo())
        }
    }

    @Test
    fun seGuardaConElFormatoDeFlutter() {
        abrir().use { base ->
            base.ajustes.guardarUltimoRespaldo(fecha(dia = 21, hora = 10, minuto = 14, mes = 10))
            assertEquals("2026-10-21T10:14:00.000Z", base.ajustes.leerCrudo("migasto_last_backup"))
        }
    }
}
