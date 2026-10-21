package com.migasto.db

import com.migasto.domain.CategoriaPropia
import com.migasto.domain.TipoMovimiento
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Portado de «Categorías propias guardadas» de test/categoria_propia_test.dart, sobre la base.
class RepositorioCategoriasPropiasTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-categorias.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    @Test
    fun crearLaMismaDevuelveLaExistente() {
        abrir().use { base ->
            val repo = base.categoriasPropias
            val a = repo.agregar("Barbería", "spa", TipoMovimiento.Gasto)
            val b = repo.agregar("  barbería ", "cafe", TipoMovimiento.Gasto)
            assertEquals(a.id, b.id)
            assertEquals(1, repo.todas().size)
            // El mismo nombre en otro tipo sí es otra categoría.
            repo.agregar("Barbería", "spa", TipoMovimiento.Ingreso)
            assertEquals(2, repo.todas().size)
        }
    }

    @Test
    fun seGuardanSeLeenAlAbrirYSePuedenBorrar() {
        val creada = abrir().use { it.categoriasPropias.agregar("Gym", "gimnasio", TipoMovimiento.Gasto) }
        abrir().use { base ->
            assertEquals("Gym", base.categoriasPropias.todas().single().nombre)
            base.categoriasPropias.quitar(creada.id)
            assertEquals(emptyList(), base.categoriasPropias.todas())
        }
    }

    @Test
    fun mezclarLasDeUnRespaldoNoPisaLasTuyas() {
        abrir().use { base ->
            val repo = base.categoriasPropias
            val mia = repo.agregar("Gym", "gimnasio", TipoMovimiento.Gasto)
            val barberia = CategoriaPropia("c1", "Barbería", "spa", TipoMovimiento.Gasto)
            repo.mezclar(listOf(CategoriaPropia(mia.id, "Otro nombre", "spa", TipoMovimiento.Gasto), barberia))
            val lista = repo.todas()
            assertEquals(2, lista.size)
            assertEquals("Gym", lista.first { it.id == mia.id }.nombre)
            assertEquals(barberia, lista.first { it.id == "c1" })
        }
    }

    @Test
    fun conservanElOrdenDeCreacion() {
        abrir().use { base ->
            listOf("Uno", "Dos", "Tres").forEach { base.categoriasPropias.agregar(it, "x", TipoMovimiento.Gasto) }
            assertEquals(listOf("Uno", "Dos", "Tres"), base.categoriasPropias.todas().map { it.nombre })
        }
    }
}
