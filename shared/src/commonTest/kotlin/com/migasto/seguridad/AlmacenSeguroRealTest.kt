package com.migasto.seguridad

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.Categoria
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.fecha
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** Corre en cada plataforma contra su almacén de verdad: Keychain en iOS, Keystore en Android. */
@SoloConAppAnfitriona
class AlmacenSeguroRealTest {
    private val almacen = almacenSeguroReal()
    private val nombre = "prueba_almacen"
    private val nombreBase = "prueba-almacen.db"
    private val fabrica = fabricaDePrueba()

    @AfterTest
    fun limpiar() {
        almacen.eliminar(nombre)
        almacen.eliminar(ClaveDeLaBase.NOMBRE + "_prueba")
        fabrica.borrar(nombreBase)
    }

    @Test
    fun loQueNoExisteDevuelveNull() {
        assertNull(almacen.leer("no_existe_jamas"))
    }

    @Test
    fun guardaYLee() {
        almacen.guardar(nombre, "a1b2c3")
        assertEquals("a1b2c3", almacen.leer(nombre))
    }

    @Test
    fun guardarDeNuevoReemplazaElValor() {
        almacen.guardar(nombre, "uno")
        almacen.guardar(nombre, "dos")
        assertEquals("dos", almacen.leer(nombre))
    }

    @Test
    fun eliminarLoBorraYNoFallaSiNoExiste() {
        almacen.guardar(nombre, "x")
        almacen.eliminar(nombre)
        assertNull(almacen.leer(nombre))
        almacen.eliminar(nombre)
    }

    @Test
    fun guardaTextosConAcentosYSimbolos() {
        val texto = "ñandú ü 日本 ''\" \\ \n fin"
        almacen.guardar(nombre, texto)
        assertEquals(texto, almacen.leer(nombre))
    }

    @Test
    fun otraInstanciaDelAlmacenLeeLoGuardado() {
        almacen.guardar(nombre, "persistente")
        assertEquals("persistente", almacenSeguroReal().leer(nombre))
    }

    @Test
    fun laBaseSeAbreConLaClaveDelAlmacenYSoloConEsa() {
        val clave = ClaveDeLaBase(almacen, nombre).obtenerOCrear()
        BaseMiGasto.abrir(fabrica, nombreBase, clave).use {
            it.movimientos.guardar(
                Movimiento("m1", 10.0, "Tambo", Categoria.Compras, PaymentSource.Efectivo, fecha()),
            )
        }
        // La misma clave del almacén abre la base.
        val otraVez = ClaveDeLaBase(almacenSeguroReal(), nombre).obtenerOCrear()
        assertEquals(clave, otraVez)
        BaseMiGasto.abrir(fabrica, nombreBase, otraVez).use { assertEquals(1, it.movimientos.cantidad()) }
        // Otra clave no.
        assertNotEquals(clave, "otra")
        assertFailsWith<Throwable> { BaseMiGasto.abrir(fabrica, nombreBase, "otra").use { it.movimientos.cantidad() } }
    }
}
