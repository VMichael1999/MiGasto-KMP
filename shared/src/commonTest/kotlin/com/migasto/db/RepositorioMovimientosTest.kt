package com.migasto.db

import com.migasto.domain.CanalMovimiento
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.TipoMovimiento
import com.migasto.domain.fecha
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

// Portado de test/encrypted_database_test.dart («DatabaseManager sobre Drift» y la base cifrada) y
// del último test de test/categoria_propia_test.dart. Que el archivo no sea legible y que SQLCipher
// esté activo se prueban en SqlCipherTest de cada plataforma, porque necesitan leer el archivo.
class RepositorioMovimientosTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-repositorio.db"
    private val clave = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir(clave: String = this.clave) = BaseMiGasto.abrir(fabrica, nombre, clave)

    private fun movimiento(id: String, comercio: String = "Tambo Miraflores", monto: Double = 25.5) = Movimiento(
        id = id,
        amount = monto,
        merchant = comercio,
        category = Categoria.Compras,
        source = PaymentSource.Tarjeta,
        date = fecha(),
        tipo = TipoMovimiento.Gasto,
        canal = CanalMovimiento.Wallet,
        estado = EstadoMovimiento.Confirmado,
        tarjeta = "Visa BBVA ···4821",
        latitud = -12.09,
        longitud = -77.03,
        precision = 15.0,
        lugar = "Av. Arequipa 3120, San Isidro",
        textoOriginal = "Apple Pay: S/ 25.50 en Tambo Miraflores",
    )

    @Test
    fun guardaLeeActualizaYEliminaMovimientosConTodosSusCampos() {
        abrir().use { base ->
            base.movimientos.guardar(movimiento("a"))
            base.movimientos.guardar(movimiento("b", comercio = "Oxxo", monto = 8.9))

            var lista = base.movimientos.todos()
            assertEquals(2, lista.size)
            val a = lista.first { it.id == "a" }
            assertEquals(CanalMovimiento.Wallet, a.canal)
            assertEquals("Visa BBVA ···4821", a.tarjeta)
            assertEquals(-12.09, a.latitud)
            assertEquals(15.0, a.precision)
            assertEquals("Av. Arequipa 3120, San Isidro", a.lugar)
            assertTrue("Apple Pay" in a.textoOriginal)
            assertTrue(a.isConfirmed)
            assertEquals(fecha(), a.date)

            // Actualizar con el mismo id no duplica.
            base.movimientos.guardar(a.copiar(amount = 30.0))
            lista = base.movimientos.todos()
            assertEquals(2, lista.size)
            assertEquals(30.0, lista.first { it.id == "a" }.amount)

            base.movimientos.eliminar("b")
            assertEquals(1, base.movimientos.todos().size)
        }
    }

    @Test
    fun laBaseDevuelveLaCategoriaPropiaTalComoSeGuardo() {
        abrir().use { base ->
            base.movimientos.guardar(movimiento("m1").copiar(category = Categoria.Otros, categoriaPropia = "c1"))
            base.movimientos.guardar(movimiento("m2"))
            val lista = base.movimientos.todos()
            val propia = lista.first { it.id == "m1" }
            assertEquals("c1", propia.categoriaPropia)
            assertEquals(Categoria.Otros, propia.category)
            val normal = lista.first { it.id == "m2" }
            assertNull(normal.categoriaPropia)
            assertEquals(Categoria.Compras, normal.category)
        }
    }

    @Test
    fun conLaClaveCorrectaLosDatosSobrevivenAlReabrir() {
        abrir().use { it.movimientos.guardar(movimiento("u1")) }
        abrir().use { base ->
            assertEquals("Tambo Miraflores", base.movimientos.todos().single().merchant)
        }
    }

    @Test
    fun conOtraClaveNoAbre() {
        abrir().use { it.movimientos.guardar(movimiento("u1")) }
        assertFailsWith<Throwable> {
            abrir("otra-clave").use { it.movimientos.todos() }
        }
    }

    @Test
    fun loQueTrajeOtraVersionConValoresDesconocidosCaeEnValoresSeguros() {
        abrir().use { base ->
            val m = movimiento("x")
            base.movimientos.guardar(m)
            // Simula una fila escrita por otra versión con claves que esta no conoce.
            base.ejecutarSql(
                "UPDATE movimientos SET source_name='paypal', tipo_name='?', estado_name='', canal_name='x', " +
                    "category_name='marte|c9' WHERE uuid='x'",
            )
            val leido = base.movimientos.todos().single()
            assertEquals(PaymentSource.Manual, leido.source)
            assertEquals(TipoMovimiento.Gasto, leido.tipo)
            assertEquals(EstadoMovimiento.Pendiente, leido.estado)
            assertEquals(CanalMovimiento.Manual, leido.canal)
            assertEquals(Categoria.Otros, leido.category)
            assertEquals("c9", leido.categoriaPropia)
        }
    }

    @Test
    fun observarTodosEmiteLaListaYSeActualiza() = runTest {
        abrir().use { base ->
            assertTrue(base.movimientos.observarTodos().first().isEmpty())
            base.movimientos.guardar(movimiento("a"))
            assertEquals(listOf("a"), base.movimientos.observarTodos().first().map { it.id })
        }
    }

    @Test
    fun losMasRecientesVanPrimero() {
        abrir().use { base ->
            base.movimientos.guardar(movimiento("viejo").copiar(date = fecha(dia = 1)))
            base.movimientos.guardar(movimiento("nuevo").copiar(date = fecha(dia = 20)))
            assertEquals(listOf("nuevo", "viejo"), base.movimientos.todos().map { it.id })
        }
    }

    @Test
    fun laBaseNuevaEmpiezaVacia() {
        abrir().use { assertEquals(0, it.movimientos.cantidad()); assertFalse(it.categoriasPropias.todas().isNotEmpty()) }
    }
}
