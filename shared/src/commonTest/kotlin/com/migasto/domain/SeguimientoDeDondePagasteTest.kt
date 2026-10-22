package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

// Nuevo: en Android esto lo armaba DondePagasteViewModel (observar los movimientos, agrupar por zona y elegir el
// período). Ahora lo comparten Android e iPhone.
class SeguimientoDeDondePagasteTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-seguimiento-donde-pagaste.db"
    private val ahora = fecha(dia = 27, hora = 12, minuto = 0, mes = 9)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun gasto(id: String, comercio: String, monto: Double, lugar: String?, hace: kotlin.time.Duration = 0.days, conUbicacion: Boolean = true) =
        Movimiento(
            id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = PaymentSource.Tarjeta,
            date = ahora - hace, estado = EstadoMovimiento.Confirmado,
            latitud = if (conUbicacion) -12.09 else null, longitud = if (conUbicacion) -77.03 else null, lugar = lugar,
        )

    private fun probar(bloque: suspend (BaseMiGasto, SeguimientoDeDondePagaste) -> Unit) = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            bloque(base, SeguimientoDeDondePagaste(base, { ahora }, TimeZone.UTC))
        }
    }

    @Test
    fun sinPagosConUbicacionDiceComoAgregarla() = probar { _, s ->
        val p = s.estado.first()
        assertEquals(PeriodoDeUbicaciones.Semana, p.periodo)
        assertTrue(p.filas.isEmpty())
        assertNull(p.limites)
        assertEquals(TextosDeDondePagaste.VACIO, p.textoVacio)
        assertEquals(TextosDeDondePagaste.PIE, p.pie)
        assertEquals(TextosDeDondePagaste.TITULO, p.titulo)
        assertEquals(listOf("Semana", "Mes", "Todo"), p.opcionesDePeriodo.map { it.texto })
    }

    @Test
    fun agrupaPorZonaConElTextoYElTotalYaArmados() = probar { base, s ->
        base.movimientos.guardar(gasto("a", "Wong", 80.0, "Av. Arequipa 3120, San Isidro"))
        base.movimientos.guardar(gasto("b", "Tambo", 25.5, "Calle Los Pinos 10, San Isidro"))
        base.movimientos.guardar(gasto("c", "Plaza Vea", 40.0, "Av. Brasil 100, Jesús María"))
        base.movimientos.guardar(gasto("d", "Sin ubicación", 9.0, null, conUbicacion = false))
        val p = s.estado.first()
        assertEquals(listOf("San Isidro", "Jesús María"), p.filas.map { it.nombre })
        val fila = p.filas.first()
        assertEquals(2, fila.cantidad)
        assertEquals("Wong y 1 más", fila.subtitulo)
        assertEquals("S/ 105.50", fila.totalTexto)
        assertEquals("San Isidro, 2 pagos, S/ 105.50. Wong y 1 más", fila.descripcion(ocultarMontos = false))
        assertEquals("San Isidro, 2 pagos. Wong y 1 más", fila.descripcion(ocultarMontos = true))
        assertNotNull(p.limites)
        assertEquals(3, p.limites?.puntos)
        assertEquals(2, p.zonas.size)
    }

    @Test
    fun elNombreGuardadoReemplazaAlComercio() = probar { base, s ->
        base.movimientos.guardar(gasto("a", "Wong", 80.0, "Av. Arequipa 3120, San Isidro"))
        base.ajustes.establecerAlias("Wong", "Mercado")
        assertEquals("Mercado", s.estado.first().filas.single().subtitulo)
    }

    @Test
    fun elPeriodoFiltraYSeCambiaConElegirPeriodo() = probar { base, s ->
        base.movimientos.guardar(gasto("nuevo", "Wong", 80.0, "Av. Arequipa 3120, San Isidro"))
        base.movimientos.guardar(gasto("viejo", "Tambo", 25.5, "Av. Brasil 100, Jesús María", hace = 20.days))
        assertEquals(listOf("San Isidro"), s.estado.first().filas.map { it.nombre })
        s.elegirPeriodo(PeriodoDeUbicaciones.Todo)
        val p = s.estado.first()
        assertEquals(PeriodoDeUbicaciones.Todo, p.periodo)
        assertEquals(2, p.filas.size)
    }
}
