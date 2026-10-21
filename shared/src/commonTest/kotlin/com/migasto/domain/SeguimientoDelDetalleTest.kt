package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: en Android esto vivía en DetalleViewModel (observar el movimiento, refrescar al cambiar el alias o las
// categorías, y las acciones). Ahora sale de `shared` para que Android y iPhone hagan lo mismo.
class SeguimientoDelDetalleTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-seguimiento-detalle.db"
    private val utc = TimeZone.UTC

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun gasto(id: String = "g1", estado: EstadoMovimiento = EstadoMovimiento.Confirmado, tipo: TipoMovimiento = TipoMovimiento.Gasto) = Movimiento(
        id = id, amount = 25.0, merchant = "BARBERÍA EL PATRÓN", category = Categoria.Otros, source = PaymentSource.Efectivo,
        date = fecha(dia = 1, hora = 11, minuto = 0, mes = 10), tipo = tipo, estado = estado,
    )

    private fun probar(bloque: suspend (BaseMiGasto, SeguimientoDelDetalle) -> Unit) = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.movimientos.guardar(gasto())
            bloque(base, SeguimientoDelDetalle(base, crearRegistroDeMovimientos(base), "g1", utc))
        }
    }

    private suspend fun SeguimientoDelDetalle.listo() = assertIs<EstadoDelDetalle.Listo>(estado.first()).detalle

    @Test
    fun muestraElMovimientoConSuBoleta() = probar { _, s ->
        val d = s.listo()
        assertEquals("BARBERÍA EL PATRÓN", d.nombre)
        assertEquals("jue 1 oct, 11:00", d.fecha)
        assertEquals("Eliminar gasto", d.textoDeEliminar)
    }

    @Test
    fun siElMovimientoNoExisteLoDice() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val s = SeguimientoDelDetalle(base, crearRegistroDeMovimientos(base), "no-existe", utc)
            assertIs<EstadoDelDetalle.NoExiste>(s.estado.first())
        }
    }

    @Test
    fun elAliasCambiaElNombreAlInstante() = probar { _, s ->
        s.establecerAlias("BARBERÍA EL PATRÓN", "La barbería")
        assertEquals("La barbería", s.listo().nombre)
        assertEquals("BARBERÍA EL PATRÓN", s.listo().nombreOriginal)
        s.establecerAlias("BARBERÍA EL PATRÓN", "")
        assertEquals("BARBERÍA EL PATRÓN", s.listo().nombre)
    }

    @Test
    fun cambiarLaCategoriaLaGuarda() = probar { _, s ->
        s.cambiarCategoria(Categoria.Compras, false, null)
        assertEquals(Categoria.Compras, s.listo().categoria)
    }

    @Test
    fun crearUnaCategoriaPropiaSeLaPoneAlMovimiento() = probar { _, s ->
        s.crearCategoriaYElegirla("  Barbería ", "scissors", TipoMovimiento.Gasto, false)
        val d = s.listo()
        assertEquals("Barbería", d.categoriaEtiqueta)
        assertEquals("scissors", d.iconoPropio)
        assertNotNull(d.categoriaPropiaId)
    }

    @Test
    fun elEstadoTraeLasCategoriasPropiasParaElegir() = probar { base, s ->
        base.categoriasPropias.agregar("Gimnasio", "gimnasio", TipoMovimiento.Gasto)
        val listo = assertIs<EstadoDelDetalle.Listo>(s.estado.first())
        assertEquals(listOf("Gimnasio"), listo.propias.map { it.nombre })
        assertEquals(TipoMovimiento.Gasto, listo.tipo)
    }

    @Test
    fun editarGuardaLosCambiosOAvisaSiNoSirven() = probar { _, s ->
        assertTrue(s.editar("30,5", "Nuevo nombre", "una nota"))
        val d = s.listo()
        assertEquals(30.5, d.monto)
        assertEquals("Nuevo nombre", d.nombreOriginal)
        assertEquals("una nota", d.nota)
        assertFalse(s.editar("0", "Algo", ""))
        assertFalse(s.editar("12", "   ", ""))
        assertFalse(s.editar("abc", "Algo", ""))
        assertEquals(30.5, s.listo().monto)
    }

    @Test
    fun confirmarUnPendienteLoDejaConfirmado() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.movimientos.guardar(gasto("p1", EstadoMovimiento.Pendiente, TipoMovimiento.Ingreso))
            val s = SeguimientoDelDetalle(base, crearRegistroDeMovimientos(base), "p1", utc)
            assertTrue(s.listo().porConfirmar)
            s.confirmar()
            assertFalse(s.listo().porConfirmar)
        }
    }

    @Test
    fun laUbicacionSePoneYSeQuita() = probar { _, s ->
        s.ponerUbicacion(UbicacionCapturada(-12.09, -77.03, 15.0, "Av. Arequipa 3120, San Isidro"))
        assertEquals("Av. Arequipa 3120, San Isidro", s.listo().direccion)
        s.quitarUbicacion()
        assertNull(s.listo().direccion)
    }

    @Test
    fun eliminarDevuelveElOriginalParaDeshacer() = probar { base, s ->
        val original = assertNotNull(s.eliminar())
        assertEquals("g1", original.id)
        assertIs<EstadoDelDetalle.NoExiste>(s.estado.first())
        assertEquals(0, base.movimientos.todos().size)
        // Deshacer: se vuelve a guardar con el mismo id.
        crearRegistroDeMovimientos(base).restaurar(original)
        assertIs<EstadoDelDetalle.Listo>(s.estado.first())
    }

    @Test
    fun eliminarUnoQueYaNoExisteNoDevuelveNada() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            assertNull(SeguimientoDelDetalle(base, crearRegistroDeMovimientos(base), "no-existe", utc).eliminar())
        }
    }
}
