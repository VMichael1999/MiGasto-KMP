package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: lo que las pantallas (Android y iPhone) necesitan del formulario sin repetirlo: las teclas como texto,
// las categorías ya armadas, el guardado en una llamada y los textos fijos.
class NuevoMovimientoParaLasPantallasTest {
    private val barberia = CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto)
    private val clases = CategoriaPropia("c2", "Clases", "libro", TipoMovimiento.Ingreso)
    private val propias = listOf(barberia, clases)

    private fun teclear(texto: String) = texto.fold(formularioVacio()) { f, t -> f.presionando(t.toString()) }

    @Test
    fun elFormularioVacioEsUnGastoEnEfectivoSinMonto() {
        val f = formularioVacio()
        assertEquals(TipoMovimiento.Gasto, f.tipo)
        assertEquals(PaymentSource.Efectivo, f.fuente)
        assertEquals("0.00", f.entrada.texto)
        assertEquals(false, f.puedeGuardar)
    }

    @Test
    fun lasTeclasComoTextoArmanElMontoIgualQueLasDeKotlin() {
        assertEquals("25.00", teclear("25").entrada.texto)
        assertEquals("12.5", teclear("12.5").entrada.texto)
        assertEquals("1.00", teclear("12<").entrada.texto)
        assertEquals("0.00", teclear("1<").entrada.texto)
    }

    @Test
    fun unaTeclaQueNoEsUnaSolaLetraNoHaceNada() {
        assertEquals("0.00", formularioVacio().presionando("").entrada.texto)
        assertEquals("0.00", formularioVacio().presionando("12").entrada.texto)
        assertEquals("0.00", formularioVacio().presionando("x").entrada.texto)
    }

    @Test
    fun losChipsDeUnGastoSonLasCategoriasDeGastoMasLasPropiasDeGasto() {
        val chips = formularioVacio().chipsDeCategoria(propias)
        assertEquals(Categoria.gastos.map { it.etiqueta } + "Barbería", chips.map { it.etiqueta })
        assertEquals(Categoria.Otros, chips.single { it.elegido }.categoria)
        assertNull(chips.last().propia?.takeIf { it.id != "c1" })
    }

    @Test
    fun conIngresoLosChipsCambianALosDeIngreso() {
        val chips = formularioVacio().conTipo(TipoMovimiento.Ingreso).chipsDeCategoria(propias)
        assertEquals(Categoria.ingresos.map { it.etiqueta } + "Clases", chips.map { it.etiqueta })
        assertEquals(Categoria.OtrosIngresos, chips.single { it.elegido }.categoria)
    }

    @Test
    fun elegirUnChipDeSiempreOUnaPropiaMarcaSoloEse() {
        val f = formularioVacio()
        val compras = f.chipsDeCategoria(propias).first { it.categoria == Categoria.Compras && it.propia == null }
        val conCompras = f.conChip(compras)
        assertEquals(Categoria.Compras, conCompras.categoria)
        assertEquals(listOf("Compras"), conCompras.chipsDeCategoria(propias).filter { it.elegido }.map { it.etiqueta })

        val propia = conCompras.chipsDeCategoria(propias).last()
        val conPropia = conCompras.conChip(propia)
        assertEquals("c1", conPropia.categoriaPropia)
        assertEquals(listOf("Barbería"), conPropia.chipsDeCategoria(propias).filter { it.elegido }.map { it.etiqueta })
    }

    @Test
    fun lasTeclasEstanEnElOrdenDelTeclado() {
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<"), TextosDeNuevoMovimiento.TECLAS)
    }

    @Test
    fun losTextosFijos() {
        assertEquals("Nuevo movimiento", TextosDeNuevoMovimiento.TITULO)
        assertEquals("Cambiar", TextosDeNuevoMovimiento.CAMBIAR)
        assertEquals("Nueva categoría", TextosDeNuevoMovimiento.NUEVA_CATEGORIA)
        assertEquals("Nombre", TextosDeNuevoMovimiento.NOMBRE)
        assertEquals("Crear categoría", TextosDeNuevoMovimiento.CREAR_CATEGORIA)
        assertEquals("Nueva", TextosDeNuevoMovimiento.NUEVA)
        assertTrue(TextosDeNuevoMovimiento.hoy().startsWith("Hoy "))
    }

    @Test
    fun lasFuentesSeOfrecenConSuEtiqueta() {
        assertEquals(listOf("Efectivo", "Yape", "Plin", "Tarjeta", "Otro"), opcionesDeFuente().map { it.etiqueta })
        assertEquals(PaymentSource.Yape, opcionesDeFuente()[1].fuente)
    }

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-nuevo-movimiento.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun guardarCreaElMovimientoYDevuelveElMensaje() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val registro = crearRegistroDeMovimientos(base)
            val f = teclear("25.5").conConcepto("  Tambo ").conFuente(PaymentSource.Yape).conChip(
                formularioVacio().chipsDeCategoria(propias).first { it.categoria == Categoria.Compras },
            )
            assertEquals("Gasto guardado", guardarNuevoMovimiento(registro, f))
            val guardado = base.movimientos.todos().single()
            assertEquals(25.5, guardado.amount)
            assertEquals("Tambo", guardado.merchant)
            assertEquals(PaymentSource.Yape, guardado.source)
            assertEquals(Categoria.Compras, guardado.category)
        }
    }

    @Test
    fun guardarUnIngresoDiceIngresoGuardado() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val f = teclear("100").conTipo(TipoMovimiento.Ingreso).conConcepto("Venta")
            assertEquals("Ingreso guardado", guardarNuevoMovimiento(crearRegistroDeMovimientos(base), f))
            assertEquals(TipoMovimiento.Ingreso, base.movimientos.todos().single().tipo)
        }
    }

    @Test
    fun sinMontoOSinDetalleNoGuardaNada() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val registro = crearRegistroDeMovimientos(base)
            assertNull(guardarNuevoMovimiento(registro, teclear("25")))
            assertNull(guardarNuevoMovimiento(registro, formularioVacio().conConcepto("Tambo")))
            assertEquals(0, base.movimientos.todos().size)
        }
    }
}
