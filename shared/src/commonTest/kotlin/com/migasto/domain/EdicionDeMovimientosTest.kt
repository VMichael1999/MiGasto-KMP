package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Port de updateExpense, changeCategory, setLocation y removeLocation de ExpensesNotifier
// (lib/presentation/providers.dart) y de la hoja «Editar» del detalle.
class EdicionDeMovimientosTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-edicion.db"
    private val ahora = fecha(dia = 27, hora = 12, minuto = 41)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.registro() =
        RegistroDeMovimientos(movimientos, ajustes, ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias), { ahora })

    private fun BaseMiGasto.gasto(id: String, comercio: String, categoria: Categoria = Categoria.Otros, tipo: TipoMovimiento = TipoMovimiento.Gasto) =
        movimientos.guardar(
            Movimiento(id, 10.0, comercio, categoria, PaymentSource.Yape, ahora, tipo = tipo, estado = EstadoMovimiento.Confirmado),
        )

    private fun BaseMiGasto.leer(id: String) = movimientos.todos().first { it.id == id }

    // Cambiar la categoría

    @Test
    fun cambiarLaCategoriaDeUnMovimiento() {
        abrir().use { base ->
            base.gasto("a", "Tambo")
            base.registro().cambiarCategoria("a", Categoria.Compras)
            assertEquals(Categoria.Compras, base.leer("a").category)
            assertNull(base.ajustes.categoriaAprendida("Tambo")) // sin «aplicar a todos» no se aprende nada
        }
    }

    @Test
    fun aplicarATodosLosPagosAlMismoComercioYAprenderlo() {
        abrir().use { base ->
            base.gasto("a", "Tambo Surco")
            base.gasto("b", "  TAMBO SURCO ")
            base.gasto("c", "Otro comercio")
            base.gasto("i", "Tambo Surco", tipo = TipoMovimiento.Ingreso, categoria = Categoria.OtrosIngresos)
            base.registro().cambiarCategoria("a", Categoria.Compras, recordar = true)
            assertEquals(Categoria.Compras, base.leer("a").category)
            assertEquals(Categoria.Compras, base.leer("b").category)
            assertEquals(Categoria.Otros, base.leer("c").category)
            assertEquals(Categoria.OtrosIngresos, base.leer("i").category) // los ingresos no se tocan
            assertEquals(Categoria.Compras, base.ajustes.categoriaAprendida("Tambo Surco"))
        }
    }

    @Test
    fun conUnaCategoriaPropiaSeAplicaATodosPeroNoSeAprende() {
        abrir().use { base ->
            base.gasto("a", "Barbería")
            base.gasto("b", "Barbería")
            base.registro().cambiarCategoria("a", Categoria.Otros, recordar = true, propia = "c1")
            assertEquals("c1", base.leer("a").categoriaPropia)
            assertEquals("c1", base.leer("b").categoriaPropia)
            assertNull(base.ajustes.categoriaAprendida("Barbería"))
        }
    }

    @Test
    fun elegirUnaDeSiempreQuitaLaPropia() {
        abrir().use { base ->
            base.gasto("a", "X")
            base.registro().cambiarCategoria("a", Categoria.Otros, propia = "c1")
            base.registro().cambiarCategoria("a", Categoria.Compras)
            assertNull(base.leer("a").categoriaPropia)
            assertEquals(Categoria.Compras, base.leer("a").category)
        }
    }

    @Test
    fun enUnIngresoRecordarNoHaceNada() {
        abrir().use { base ->
            base.gasto("i", "Juan", categoria = Categoria.OtrosIngresos, tipo = TipoMovimiento.Ingreso)
            base.registro().cambiarCategoria("i", Categoria.Venta, recordar = true)
            assertEquals(Categoria.Venta, base.leer("i").category)
            assertNull(base.ajustes.categoriaAprendida("Juan"))
        }
    }

    @Test
    fun cambiarLaCategoriaDeUnMovimientoQueNoExisteNoHaceNada() {
        abrir().use { base -> base.registro().cambiarCategoria("no-existe", Categoria.Compras, recordar = true) }
    }

    // Editar

    @Test
    fun editarMontoComercioYNota() {
        abrir().use { base ->
            base.gasto("a", "Tambo")
            assertTrue(base.registro().editar("a", "30,50", "  Tambo Surco ", "  almuerzo  "))
            val m = base.leer("a")
            assertEquals(30.5, m.amount)
            assertEquals("Tambo Surco", m.merchant)
            assertEquals("almuerzo", m.notes)
        }
    }

    @Test
    fun noSeEditaConUnMontoInvalidoOUnComercioVacio() {
        abrir().use { base ->
            base.gasto("a", "Tambo")
            val registro = base.registro()
            assertFalse(registro.editar("a", "abc", "Tambo", ""))
            assertFalse(registro.editar("a", "0", "Tambo", ""))
            assertFalse(registro.editar("a", "-5", "Tambo", ""))
            assertFalse(registro.editar("a", "10", "   ", ""))
            assertFalse(registro.editar("no-existe", "10", "X", ""))
            assertEquals(10.0, base.leer("a").amount)
            assertEquals("Tambo", base.leer("a").merchant)
        }
    }

    @Test
    fun editarNoCambiaLoDemas() {
        abrir().use { base ->
            base.gasto("a", "Tambo", categoria = Categoria.Compras)
            base.registro().editar("a", "11", "Tambo", "")
            val m = base.leer("a")
            assertEquals(Categoria.Compras, m.category)
            assertEquals(PaymentSource.Yape, m.source)
            assertEquals(EstadoMovimiento.Confirmado, m.estado)
            assertEquals(ahora, m.date)
        }
    }

    // Ubicación

    @Test
    fun ponerYQuitarLaUbicacionDeUnMovimiento() {
        abrir().use { base ->
            base.gasto("a", "Tambo")
            val registro = base.registro()
            registro.ponerUbicacion("a", -12.09, -77.03, 15.0, "Av. Arequipa 3120")
            val con = base.leer("a")
            assertTrue(con.tieneUbicacion)
            assertEquals(15.0, con.precision)
            assertEquals("Av. Arequipa 3120", con.lugar)
            registro.quitarUbicacion("a")
            val sin = base.leer("a")
            assertFalse(sin.tieneUbicacion)
            assertNull(sin.lugar)
            assertNull(sin.precision)
        }
    }

    @Test
    fun quitarTodasLasUbicacionesDejaLosMovimientos() {
        abrir().use { base ->
            base.gasto("a", "A")
            base.gasto("b", "B")
            val registro = base.registro()
            registro.ponerUbicacion("a", 1.0, 2.0, 3.0, "x")
            registro.ponerUbicacion("b", 1.0, 2.0, null, null)
            registro.quitarTodasLasUbicaciones()
            assertEquals(2, base.movimientos.todos().size)
            assertTrue(base.movimientos.todos().none { it.tieneUbicacion || it.lugar != null })
        }
    }
}
