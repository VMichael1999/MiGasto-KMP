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

// Port de test/location_test.dart (agregar, negar el permiso, quitar una, borrar todas) y de la parte pura de
// lib/data/services/location_service.dart (los mensajes y cómo se arma la dirección).
class UbicacionTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-ubicacion.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.registro() = RegistroDeMovimientos(
        movimientos = movimientos,
        ajustes = ajustes,
        clasificador = ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias),
        reloj = { fecha() },
        nuevoId = { "id" },
    )

    private fun gasto(
        id: String = "g1",
        conUbicacion: Boolean = false,
        latitud: Double? = if (conUbicacion) -12.09 else null,
        longitud: Double? = if (conUbicacion) -77.03 else null,
        precision: Double? = if (conUbicacion) 15.0 else null,
        lugar: String? = if (conUbicacion) "Av. Arequipa 3120, San Isidro" else null,
    ) = Movimiento(
        id = id, amount = 25.5, merchant = "Tambo", category = Categoria.Compras, source = PaymentSource.Tarjeta, date = fecha(),
        latitud = latitud, longitud = longitud, precision = precision, lugar = lugar,
    )

    private val capturada = UbicacionCapturada(-12.09, -77.03, 15.0, "Av. Arequipa 3120, San Isidro")

    @Test
    fun agregarLaUbicacionLaGuardaConSuDireccionYPrecision() {
        abrir().use { base ->
            base.movimientos.guardar(gasto())
            base.registro().ponerUbicacion("g1", capturada)
            val guardado = base.movimientos.todos().single()
            assertTrue(guardado.tieneUbicacion)
            assertEquals("Av. Arequipa 3120, San Isidro", guardado.lugar)
            assertEquals(15.0, guardado.precision)
        }
    }

    @Test
    fun quitarLaUbicacionDeUnGastoDejaElMovimiento() {
        abrir().use { base ->
            base.movimientos.guardar(gasto(conUbicacion = true))
            base.registro().quitarUbicacion("g1")
            val guardado = base.movimientos.todos().single()
            assertFalse(guardado.tieneUbicacion)
            assertNull(guardado.lugar)
        }
    }

    @Test
    fun borrarTodasLasUbicacionesConservaLosMovimientos() {
        abrir().use { base ->
            base.movimientos.guardar(gasto("a", conUbicacion = true))
            base.movimientos.guardar(gasto("b", conUbicacion = true))
            base.movimientos.guardar(gasto("c"))
            base.registro().quitarTodasLasUbicaciones()
            val todos = base.movimientos.todos()
            assertEquals(3, todos.size)
            assertTrue(todos.none { it.tieneUbicacion || it.lugar != null })
        }
    }

    @Test
    fun cadaFalloTieneSuMensaje() {
        assertEquals("Activa la ubicación de tu teléfono para agregarla.", mensajeDeFallo(MotivoDeFallo.ServiciosApagados))
        assertEquals("Sin el permiso de ubicación no podemos guardar dónde pagaste.", mensajeDeFallo(MotivoDeFallo.Denegada))
        assertEquals(
            "El permiso de ubicación está bloqueado. Actívalo en los ajustes del teléfono.",
            mensajeDeFallo(MotivoDeFallo.DenegadaParaSiempre),
        )
        assertEquals("No pudimos obtener tu ubicación. Prueba de nuevo al aire libre.", mensajeDeFallo(MotivoDeFallo.NoDisponible))
    }

    @Test
    fun soloSePideAbrirLosAjustesSiElPermisoEstaBloqueado() {
        assertTrue(MotivoDeFallo.DenegadaParaSiempre.ofreceAbrirAjustes)
        assertFalse(MotivoDeFallo.Denegada.ofreceAbrirAjustes)
        assertFalse(MotivoDeFallo.ServiciosApagados.ofreceAbrirAjustes)
    }

    @Test
    fun laDireccionJuntaCalleNumeroYZona() {
        assertEquals("Av. Arequipa 3120, San Isidro", armarDireccion("Av. Arequipa", "3120", "San Isidro", "Lima"))
    }

    @Test
    fun sinBarrioUsaLaCiudad() {
        assertEquals("Av. Arequipa 3120, Lima", armarDireccion("Av. Arequipa", "3120", null, "Lima"))
        assertEquals("Av. Arequipa 3120, Lima", armarDireccion("Av. Arequipa", "3120", "  ", "Lima"))
    }

    @Test
    fun sinNumeroNiCalleSeOmiteLoQueFalta() {
        assertEquals("Av. Arequipa, San Isidro", armarDireccion("Av. Arequipa", null, "San Isidro", null))
        assertEquals("San Isidro", armarDireccion(null, null, "San Isidro", "Lima"))
        assertEquals("3120, San Isidro", armarDireccion("", "3120", "San Isidro", null))
    }

    @Test
    fun sinNingunDatoNoHayDireccion() {
        assertNull(armarDireccion(null, null, null, null))
        assertNull(armarDireccion(" ", "", null, "  "))
    }

    @Test
    fun laDireccionAMostrarCaeEnLasCoordenadasSiNoHayLugar() {
        assertEquals("Av. Arequipa 3120, San Isidro", direccionParaMostrar(gasto(conUbicacion = true)))
        assertEquals("-12.09000, -77.03000", direccionParaMostrar(gasto(conUbicacion = true, lugar = null)))
        assertEquals("-12.04640, -77.04280", direccionParaMostrar(gasto(latitud = -12.0464, longitud = -77.0428)))
    }

    @Test
    fun laPrecisionSeRedondeaYSeOmiteSiNoHay() {
        assertEquals("Ubicación del teléfono · precisión ±15 m", textoDeLaPrecision(gasto(conUbicacion = true)))
        assertEquals("Ubicación del teléfono · precisión ±16 m", textoDeLaPrecision(gasto(conUbicacion = true, precision = 15.6)))
        assertEquals("Ubicación del teléfono", textoDeLaPrecision(gasto(conUbicacion = true, precision = null)))
    }

    @Test
    fun losTextosDelBoton() {
        assertEquals("Agregar ubicación", TextosDeUbicacion.AGREGAR)
        assertEquals("Usa tu ubicación actual, solo ahora.", TextosDeUbicacion.AGREGAR_DETALLE)
        assertEquals("Abrir ajustes", TextosDeUbicacion.ABRIR_AJUSTES)
    }
}
