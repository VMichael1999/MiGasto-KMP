package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrearRegistroTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-crear-registro.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun elRegistroCreadoGuardaEnLaBaseYClasificaConLasReglasIncluidas() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val registro = crearRegistroDeMovimientos(base)
            registro.agregarManual(TipoMovimiento.Gasto, 25.0, "Tambo", Categoria.Compras)
            assertEquals(listOf("Tambo"), base.movimientos.todos().map { it.merchant })
        }
    }
}
