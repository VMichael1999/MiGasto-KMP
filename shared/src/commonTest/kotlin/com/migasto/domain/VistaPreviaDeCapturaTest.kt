package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: la hoja de la extensión «Compartir» de iPhone muestra lo que se leyó de la captura (monto, comercio, fuente y
// categoría) antes de guardarlo. La extensión no abre la base: esto sale solo del texto y de las reglas de siempre.
class VistaPreviaDeCapturaTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 14, minuto = 49, mes = 10)
    private val constancia = "¡Yapeaste! S/ 32 Café Central 21 oct. 2026 14:49 DATOS DE LA TRANSACCIÓN Nro. de celular *** *** 277 Destino Dale Nro. de operación 01614398"
    private val clasificador = ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias)

    private fun vista(texto: String = constancia) = vistaPreviaDeCaptura(texto, ahora, utc)

    @Test
    fun leeElMontoElComercioLaFuenteYLaHora() {
        val v = assertNotNull(vista())
        assertEquals(32.0, v.monto)
        assertEquals("S/ 32.00", v.montoTexto)
        assertEquals("a Café Central · Yape · 14:49", v.detalle)
        assertFalse(v.esIngreso)
        assertEquals(PaymentSource.Yape, v.fuente)
        assertEquals("Yape", v.fuenteEtiqueta)
    }

    @Test
    fun sugiereLaCategoriaDeLasReglasYOfreceLasDelTipo() {
        val v = assertNotNull(vista())
        assertEquals(clasificador.clasificarGasto(constancia, "Café Central").clave, v.categoriaSugerida.clave)
        assertEquals(Categoria.paraTipo(TipoMovimiento.Gasto).map { it.clave }, v.opcionesDeCategoria.map { it.clave })
        assertEquals(1, v.opcionesDeCategoria.count { it.elegida })
        assertTrue(v.opcionesDeCategoria.single { it.elegida }.clave == v.categoriaSugerida.clave)
    }

    @Test
    fun unaCategoriaElegidaPasaAEstarMarcada() {
        val v = assertNotNull(vista())
        val otra = v.opcionesDeCategoria.first { !it.elegida }
        val cambiada = v.conCategoria(otra.clave)
        assertEquals(otra.clave, cambiada.categoriaSugerida.clave)
        assertEquals(otra.etiqueta, cambiada.categoriaEtiqueta)
        assertEquals(listOf(otra.clave), cambiada.opcionesDeCategoria.filter { it.elegida }.map { it.clave })
    }

    @Test
    fun unaClaveDesconocidaNoCambiaNada() {
        val v = assertNotNull(vista())
        assertEquals(v.categoriaSugerida.clave, v.conCategoria("no-existe").categoriaSugerida.clave)
    }

    @Test
    fun unTextoQueNoEsUnaConstanciaNoTieneVistaPrevia() {
        assertNull(vista("Hola, esto es una foto de mi perro"))
        // Un pago suelto que no es la constancia de pantalla (el inicio de Yape) tampoco cuenta.
        assertNull(vista("Juan Pérez te yapeó S/ 15.00"))
    }

    @Test
    fun losTextosFijosDeLaHoja() {
        assertEquals("Leído en tu iPhone, sin internet", TextosDeLaCaptura.LEIDO_EN_EL_TELEFONO)
        assertEquals("MiGasto", TextosDeLaCaptura.TITULO)
        assertEquals("Categoría", TextosDeLaCaptura.CATEGORIA)
        assertEquals("Fuente", TextosDeLaCaptura.FUENTE)
        assertEquals("No encontramos un pago en la captura.", TextosDeLaCaptura.SIN_PAGO)
        assertEquals("Comparte la constancia de Yape o Plin, con el monto a la vista.", TextosDeLaCaptura.SIN_PAGO_AYUDA)
    }

    // La categoría elegida en la hoja llega hasta el movimiento guardado

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-vista-previa-captura.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun laCategoriaElegidaEnLaHojaSeGuarda() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val r = RecibidorDeCapturas(base, crearRegistroDeMovimientos(base)) { ahora }
            r.recibir(constancia, Categoria.Transporte.clave)
            assertEquals(Categoria.Transporte, base.movimientos.todos().single().category)
        }
    }

    @Test
    fun sinCategoriaElegidaSeUsaLaDeLasReglas() {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            val r = RecibidorDeCapturas(base, crearRegistroDeMovimientos(base)) { ahora }
            r.recibir(constancia, null)
            assertEquals(clasificador.clasificarGasto(constancia, "Café Central"), base.movimientos.todos().single().category)
        }
    }
}
