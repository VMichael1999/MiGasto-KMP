package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: la lupa de la barra de iPhone busca entre todos los movimientos (comercio o nombre guardado), sin los filtros de
// la pantalla Movimientos. Lo que se dice cuando no hay nada que mostrar sale de aquí.
class BusquedaGlobalTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 1, hora = 12, minuto = 0, mes = 10)

    private fun mov(id: String, comercio: String, monto: Double, dia: Int = 1, mes: Int = 10) = Movimiento(
        id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = PaymentSource.Yape,
        date = fecha(dia = dia, hora = 10, minuto = 0, mes = mes), estado = EstadoMovimiento.Confirmado,
    )

    private val todos = listOf(
        mov("a", "Wong", 80.0),
        mov("b", "Tambo", 25.5),
        mov("c", "Wong Express", 12.0, dia = 28, mes = 9),
        mov("d", "Ana", 40.0),
    )

    private fun buscar(texto: String, alias: Map<String, String> = emptyMap()) =
        armarResultadosDeBusqueda(todos, texto, alias, emptyList(), ahora, utc)

    @Test
    fun sinTextoPideEscribirYNoMuestraNada() {
        val r = buscar("   ")
        assertTrue(r.grupos.isEmpty())
        assertEquals(0, r.cantidad)
        assertEquals(TextosDeLaBusqueda.ESCRIBE, r.mensaje)
        assertNull(r.resumen)
    }

    @Test
    fun encuentraPorComercioSinImportarMayusculasYAgrupaPorDia() {
        val r = buscar("wong")
        assertEquals(2, r.cantidad)
        assertEquals("2 resultados", r.resumen)
        assertNull(r.mensaje)
        assertEquals(listOf("Hoy, jueves 1", "Lunes 28 de septiembre"), r.grupos.map { it.etiqueta })
        assertEquals("Wong", r.grupos.first().filas.single().nombre)
    }

    @Test
    fun unSoloResultadoVaEnSingular() {
        assertEquals("1 resultado", buscar("tambo").resumen)
    }

    @Test
    fun tambienBuscaPorElNombreGuardado() {
        val r = buscar("mercado", alias = mapOf(claveDeAlias("Wong") to "Mercado"))
        assertEquals(1, r.cantidad)
        assertEquals("Mercado", r.grupos.single().filas.single().nombre)
    }

    @Test
    fun sinCoincidenciasDiceQueNoEncontroNada() {
        val r = buscar("  zzz ")
        assertEquals(0, r.cantidad)
        assertEquals("No encontramos movimientos con «zzz».", r.mensaje)
        assertNull(r.resumen)
    }

    // La versión que observa la base

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-busqueda-global.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun observaLaBaseYSeActualizaConLosMovimientos() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.movimientos.guardar(mov("x", "Plaza Vea", 30.0))
            val r = observarBusqueda(base, "plaza", { ahora }, utc).first()
            assertEquals(1, r.cantidad)
            assertEquals("Plaza Vea", r.grupos.single().filas.single().nombre)
        }
    }
}
