package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Portado de test/categoria_propia_test.dart. «Cómo se guarda la categoría» y «Movimiento» ya
// están en MovimientoTest; la base y el respaldo se prueban con sus capas.
class CategoriaPropiaTest {
    private val barberia = CategoriaPropia("c1", "Barbería", "spa", TipoMovimiento.Gasto)

    private fun movimiento(propia: String? = null, categoria: Categoria = Categoria.Otros) = Movimiento(
        id = "m1",
        amount = 12.0,
        merchant = "Peluquería",
        category = categoria,
        categoriaPropia = propia,
        source = PaymentSource.Efectivo,
        date = fecha(dia = 1, hora = 10, minuto = 0, mes = 10),
        estado = EstadoMovimiento.Confirmado,
    )

    // Qué se ve

    @Test
    fun unaPropiaQueExisteMuestraSuNombreYSiSeBorroLaDeSiempre() {
        assertEquals("Barbería", etiquetaDeCategoria(Categoria.Otros, "c1", listOf(barberia)))
        assertEquals("Otros", etiquetaDeCategoria(Categoria.Otros, "c1", emptyList()))
        assertEquals("Compras", etiquetaDeCategoria(Categoria.Compras, null, listOf(barberia)))
    }

    @Test
    fun laClaveDeAgruparEsLaPropiaSoloSiExiste() {
        assertEquals("c1", claveDeCategoria(movimiento(propia = "c1"), listOf(barberia)))
        assertEquals("otros", claveDeCategoria(movimiento(propia = "c1"), emptyList()))
        assertEquals("Barbería", etiquetaDeClave("c1", listOf(barberia)))
        assertEquals("Compras", etiquetaDeClave("compras", listOf(barberia)))
        assertEquals("Otros", etiquetaDeClave("noExiste", emptyList()))
    }

    @Test
    fun todasLasCategoriasTienenNombreVisible() {
        assertEquals("Alimentación", Categoria.Alimentacion.etiqueta)
        assertEquals("Transferencia recibida", Categoria.TransferenciaRecibida.etiqueta)
        assertEquals("Otros", Categoria.OtrosIngresos.etiqueta)
        assertTrue(Categoria.entries.all { it.etiqueta.isNotBlank() })
    }

    @Test
    fun laCategoriaDeSiempreDeUnaPropiaDependeDelTipo() {
        assertEquals(Categoria.Otros, barberia.base)
        assertEquals(Categoria.OtrosIngresos, barberia.copy(tipo = TipoMovimiento.Ingreso).base)
    }

    // Categorías propias guardadas

    @Test
    fun crearLaMismaMismoNombreYTipoDevuelveLaExistente() {
        var contador = 0
        val nuevoId = { "id${++contador}" }
        val a = CategoriasPropias().agregar("Barbería", "spa", TipoMovimiento.Gasto, nuevoId)
        val b = a.categorias.agregar("  barbería ", "cafe", TipoMovimiento.Gasto, nuevoId)
        assertEquals(a.categoria.id, b.categoria.id)
        assertEquals(1, b.categorias.lista.size)
        // El mismo nombre en otro tipo sí es otra categoría.
        val c = b.categorias.agregar("Barbería", "spa", TipoMovimiento.Ingreso, nuevoId)
        assertEquals(2, c.categorias.lista.size)
    }

    @Test
    fun elNombreSeGuardaSinEspaciosDeLosBordes() {
        assertEquals("Gym", CategoriasPropias().agregar("  Gym ", "x", TipoMovimiento.Gasto).categoria.nombre)
    }

    @Test
    fun quitarBorraSoloEsa() {
        val dos = CategoriasPropias(listOf(barberia, barberia.copy(id = "c2", nombre = "Gym")))
        assertEquals(listOf("c2"), dos.quitar("c1").lista.map { it.id })
    }

    @Test
    fun mezclarLasDeUnRespaldoNoPisaLasTuyas() {
        val mia = CategoriaPropia("m", "Gym", "gimnasio", TipoMovimiento.Gasto)
        val mezcladas = CategoriasPropias(listOf(mia))
            .mezclar(listOf(mia.copy(nombre = "Otro nombre", icono = "spa"), barberia))
        assertEquals(2, mezcladas.lista.size)
        assertEquals("Gym", mezcladas.lista.first { it.id == "m" }.nombre)
    }

    @Test
    fun mezclarSinNovedadesDevuelveLaMismaLista() {
        val actuales = CategoriasPropias(listOf(barberia))
        assertEquals(actuales.lista, actuales.mezclar(listOf(barberia)).lista)
    }

    // Datos guardados

    @Test
    fun datosDañadosSeIgnoran() {
        assertEquals(emptyList(), categoriasPropiasDesdeJson(JsonPrimitive("basura")))
        assertEquals(emptyList(), categoriasPropiasDesdeJson(null))
        val malos = JsonArray(
            listOf(
                JsonPrimitive(1),
                buildJsonObject { put("id", "x") },
                buildJsonObject { put("id", "a"); put("nombre", " ") },
            ),
        )
        assertEquals(emptyList(), categoriasPropiasDesdeJson(malos))
    }

    @Test
    fun idaYVueltaPorJson() {
        val leida = CategoriaPropia.desdeJson(barberia.aJson())
        assertEquals(barberia, leida)
        val ingreso = barberia.copy(tipo = TipoMovimiento.Ingreso)
        assertEquals(ingreso, CategoriaPropia.desdeJson(ingreso.aJson()))
    }

    @Test
    fun alLeerSeLimpiaElNombreYUnIconoFaltanteQuedaVacio() {
        val c = assertNotNull(
            CategoriaPropia.desdeJson(buildJsonObject { put("id", "a"); put("nombre", "  Gym "); put("tipo", "raro") }),
        )
        assertEquals("Gym", c.nombre)
        assertEquals("", c.icono)
        assertEquals(TipoMovimiento.Gasto, c.tipo)
        assertNull(CategoriaPropia.desdeJson(buildJsonObject { put("id", ""); put("nombre", "x") }))
    }
}
