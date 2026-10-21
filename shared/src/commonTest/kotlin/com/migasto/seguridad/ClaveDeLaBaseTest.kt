package com.migasto.seguridad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Porta _loadOrCreateKey de DatabaseManager (lib/data/datasource/local_database.dart): la clave se
// genera en el teléfono la primera vez, son 32 bytes al azar en hexadecimal, y se reutiliza.
class ClaveDeLaBaseTest {
    @Test
    fun laPrimeraVezCreaUnaClaveDe64CaracteresHexadecimales() {
        val clave = ClaveDeLaBase(AlmacenEnMemoria()).obtenerOCrear()
        assertEquals(64, clave.length)
        assertTrue(clave.all { it in '0'..'9' || it in 'a'..'f' }, "no es hexadecimal: $clave")
    }

    @Test
    fun laSiguienteVezDevuelveLaMisma() {
        val almacen = AlmacenEnMemoria()
        val primera = ClaveDeLaBase(almacen).obtenerOCrear()
        assertEquals(primera, ClaveDeLaBase(almacen).obtenerOCrear())
    }

    @Test
    fun cadaTelefonoTieneLaSuya() {
        assertNotEquals(ClaveDeLaBase(AlmacenEnMemoria()).obtenerOCrear(), ClaveDeLaBase(AlmacenEnMemoria()).obtenerOCrear())
    }

    @Test
    fun unaClaveVaciaGuardadaSeReemplaza() {
        val almacen = AlmacenEnMemoria().also { it.guardar(ClaveDeLaBase.NOMBRE, "") }
        val clave = ClaveDeLaBase(almacen).obtenerOCrear()
        assertEquals(64, clave.length)
        assertEquals(clave, almacen.leer(ClaveDeLaBase.NOMBRE))
    }

    @Test
    fun laClaveSeGuardaConElMismoNombreQueEnFlutter() {
        val almacen = AlmacenEnMemoria()
        val clave = ClaveDeLaBase(almacen).obtenerOCrear()
        assertEquals("migasto_db_key", ClaveDeLaBase.NOMBRE)
        assertEquals(clave, almacen.leer("migasto_db_key"))
    }

    @Test
    fun borrarLaClaveHaceQueSeCreeOtra() {
        val almacen = AlmacenEnMemoria()
        val vieja = ClaveDeLaBase(almacen).obtenerOCrear()
        ClaveDeLaBase(almacen).olvidar()
        assertNotEquals(vieja, ClaveDeLaBase(almacen).obtenerOCrear())
    }
}
