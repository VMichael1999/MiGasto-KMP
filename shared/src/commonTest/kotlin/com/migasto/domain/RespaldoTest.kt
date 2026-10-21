package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.measureTime
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

// Portado de test/backup_test.dart (los tres primeros grupos; «restaurar en la app» depende de los
// providers y se porta con las apps) más la prueba con un archivo hecho por Flutter.
class RespaldoTest {
    // 10 000 vueltas: lo mínimo que acepta el lector; el valor real se mide aparte.
    private val servicio = ServicioDeRespaldo(iteraciones = 10_000)
    private val clave = "mi contraseña segura"

    private fun utc(dia: Int, hora: Int, minuto: Int = 0): Instant = fecha(dia = dia, hora = hora, minuto = minuto, mes = 10)

    private fun ejemplo() = ContenidoDeRespaldo(
        creado = utc(1, 12),
        presupuesto = 1500.0,
        aprendidas = mapOf("tambo" to "alimentacion"),
        movimientos = listOf(
            Movimiento(
                id = "a1",
                amount = 25.5,
                merchant = "Tambo Surco",
                category = Categoria.Alimentacion,
                source = PaymentSource.Yape,
                date = utc(1, 9, 30),
                notes = "almuerzo",
                canal = CanalMovimiento.Notificacion,
                estado = EstadoMovimiento.Confirmado,
                latitud = -12.0464,
                longitud = -77.0428,
                precision = 12.5,
                lugar = "Av. Javier Prado Este 425",
                textoOriginal = "Yapeaste S/ 25.50 a Tambo Surco",
            ),
            Movimiento(
                id = "b2",
                amount = 3.0,
                merchant = "ÑAÑO PÉREZ ñ ü 日本",
                category = Categoria.TransferenciaRecibida,
                source = PaymentSource.Yape,
                date = utc(1, 10),
                tipo = TipoMovimiento.Ingreso,
                canal = CanalMovimiento.Notificacion,
            ),
        ),
    )

    private fun hacer(bloque: suspend () -> Unit) = runTest { bloque() }

    @Test
    fun cifraYDescifraSinPerderNingunDato() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val de = servicio.descifrar(datos, clave)

        assertEquals(1500.0, de.presupuesto)
        assertEquals(mapOf("tambo" to "alimentacion"), de.aprendidas)
        assertEquals(utc(1, 12), de.creado)
        assertEquals(2, de.movimientos.size)

        val a = de.movimientos.first()
        assertEquals("a1", a.id)
        assertEquals(25.5, a.amount)
        assertEquals("Tambo Surco", a.merchant)
        assertEquals(Categoria.Alimentacion, a.category)
        assertEquals(PaymentSource.Yape, a.source)
        assertEquals(utc(1, 9, 30), a.date)
        assertEquals("almuerzo", a.notes)
        assertEquals(EstadoMovimiento.Confirmado, a.estado)
        assertEquals(-12.0464, a.latitud)
        assertEquals(-77.0428, a.longitud)
        assertEquals(12.5, a.precision)
        assertEquals("Av. Javier Prado Este 425", a.lugar)
        assertEquals("Yapeaste S/ 25.50 a Tambo Surco", a.textoOriginal)

        val b = de.movimientos.last()
        assertEquals("ÑAÑO PÉREZ ñ ü 日本", b.merchant)
        assertEquals(TipoMovimiento.Ingreso, b.tipo)
        assertNull(b.latitud)
    }

    @Test
    fun elArchivoNoDejaVerNadaEnClaro() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val texto = datos.map { (it.toInt() and 0xFF).toChar() }.joinToString("")
        assertFalse("Tambo" in texto)
        assertFalse("Yape" in texto)
        assertFalse("Javier Prado" in texto)
        assertEquals("MGB1", texto.take(4))
    }

    @Test
    fun dosRespaldosIgualesDanArchivosDistintos() = hacer {
        val a = servicio.cifrar(ejemplo(), clave)
        val b = servicio.cifrar(ejemplo(), clave)
        assertFalse(a.contentEquals(b), "la sal y el nonce tienen que ser al azar")
    }

    @Test
    fun unaContrasenaIncorrectaNoAbreElRespaldo() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val error = assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(datos, "otra contraseña") }
        assertContains(error.message, "contraseña")
    }

    @Test
    fun siAlguienAlteraElContenidoSeDetecta() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val alterado = datos.copyOf().also { it[datos.size / 2] = (it[datos.size / 2].toInt() xor 1).toByte() }
        assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(alterado, clave) }
    }

    @Test
    fun siAlguienAlteraLaCabeceraSeDetecta() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val alterado = datos.copyOf().also { it[10] = (it[10].toInt() xor 1).toByte() }
        assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(alterado, clave) }
    }

    @Test
    fun unArchivoQueNoEsDeMiGastoSeRechazaConUnMensajeClaro() = hacer {
        val noEsRespaldo = "esto es un texto cualquiera ".repeat(5).encodeToByteArray()
        val error = assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(noEsRespaldo, clave) }
        assertContains(error.message, "no es un respaldo")
        assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(ByteArray(3), "x") }
    }

    @Test
    fun unaCabeceraConUnNumeroDeVueltasAbsurdoNoCuelgaElTelefono() = hacer {
        val datos = servicio.cifrar(ejemplo(), clave)
        val hostil = datos.copyOf()
        // 4 000 000 000 en 4 bytes big endian.
        byteArrayOf(0xEE.toByte(), 0x6B, 0x28, 0x00).copyInto(hostil, destinationOffset = 4)
        assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(hostil, clave) }
    }

    @Test
    fun laContrasenaCortaSeRechazaAlCrear() = hacer {
        assertFailsWith<ExcepcionDeRespaldo> { servicio.cifrar(ejemplo(), "corta") }
    }

    @Test
    fun conLasVueltasRealesTardaUnTiempoRazonable() = hacer {
        val real = ServicioDeRespaldo()
        assertEquals(210_000, real.iteraciones)
        val duracion = measureTime {
            val datos = real.cifrar(ejemplo(), clave)
            real.descifrar(datos, clave)
        }
        assertTrue(duracion.inWholeSeconds < 60, "tardó $duracion")
    }

    // Compatibilidad con la app de Flutter

    @Test
    fun abreUnRespaldoHechoPorFlutter() = hacer {
        val de = servicio.descifrar(respaldoHechoPorFlutter, CONTRASENA_DE_FLUTTER)

        assertEquals(1500.0, de.presupuesto)
        assertEquals(Instant.parse("2026-10-01T12:00:00Z"), de.creado)
        assertEquals(mapOf("tambo" to "alimentacion"), de.aprendidas)
        assertEquals(mapOf("michael anthony valdiviezo maza" to "Michael"), de.alias)
        assertEquals(listOf(CategoriaPropia("c1", "Barbería", "spa", TipoMovimiento.Gasto)), de.categoriasPropias)
        assertEquals(listOf("a1", "b2", "c3"), de.movimientos.map { it.id })

        val a = de.movimientos[0]
        assertEquals(25.5, a.amount)
        assertEquals(Instant.parse("2026-10-01T09:30:00Z"), a.date)
        assertEquals(Categoria.Alimentacion, a.category)
        assertEquals(CanalMovimiento.Notificacion, a.canal)
        assertEquals(12.5, a.precision)
        assertEquals("Av. Javier Prado Este 425", a.lugar)

        assertEquals("ÑAÑO PÉREZ ñ ü 日本", de.movimientos[1].merchant)
        assertEquals(TipoMovimiento.Ingreso, de.movimientos[1].tipo)
        assertEquals("c1", de.movimientos[2].categoriaPropia)
        assertEquals(Categoria.Otros, de.movimientos[2].category)
    }

    @Test
    fun elRespaldoDeFlutterNoAbreConOtraContrasena() = hacer {
        assertFailsWith<ExcepcionDeRespaldo> { servicio.descifrar(respaldoHechoPorFlutter, "otra contraseña") }
    }

    @Test
    fun laCabeceraDeNuestroArchivoTieneElMismoFormatoQueLaDeFlutter() = hacer {
        val flutter = respaldoHechoPorFlutter
        val nuestro = servicio.cifrar(ejemplo(), clave)
        assertEquals(flutter.copyOfRange(0, 8).toList(), nuestro.copyOfRange(0, 8).toList()) // «MGB1» + 10 000
        assertNotEquals(flutter.copyOfRange(8, 24).toList(), nuestro.copyOfRange(8, 24).toList()) // la sal cambia
    }

    // Formato del contenido (JSON)

    @Test
    fun unRespaldoAnteriorSinAliasNiCategoriasPropiasSeSigueAbriendo() {
        val viejo = """{"formato":1,"app":"MiGasto","creado":"2026-09-01T00:00:00.000Z","presupuesto":700,
            "aprendidas":{},"movimientos":[]}"""
        val de = contenidoDeRespaldoDesdeJson(viejo, TimeZone.UTC)
        assertEquals(emptyMap(), de.alias)
        assertEquals(emptyList(), de.categoriasPropias)
        assertEquals(700.0, de.presupuesto)
    }

    @Test
    fun unRespaldoDeUnaVersionMasNuevaPideActualizar() {
        val nuevo = """{"formato":2,"app":"MiGasto","creado":"2026-09-01T00:00:00.000Z","presupuesto":0,
            "aprendidas":{},"movimientos":[]}"""
        val error = assertFailsWith<ExcepcionDeRespaldo> { contenidoDeRespaldoDesdeJson(nuevo, TimeZone.UTC) }
        assertContains(error.message, "versión más nueva")
    }

    @Test
    fun unJsonDeOtraAppOUnoRotoSeRechaza() {
        assertFailsWith<ExcepcionDeRespaldo> { contenidoDeRespaldoDesdeJson("""{"app":"Otra","formato":1}""", TimeZone.UTC) }
        val roto = assertFailsWith<ExcepcionDeRespaldo> { contenidoDeRespaldoDesdeJson("{no es json", TimeZone.UTC) }
        assertContains(roto.message, "dañado")
    }

    @Test
    fun losValoresDesconocidosCaenEnValoresSeguros() {
        val json = """{"formato":1,"app":"MiGasto","creado":"2026-09-01T00:00:00.000Z","presupuesto":0,"aprendidas":{},
            "movimientos":[{"id":"z","monto":5,"contraparte":"X","categoria":"marte","fuente":"paypal",
            "fecha":"2026-09-01T10:00:00.000","tipo":"?","canal":"?","estado":"?"}]}"""
        val m = contenidoDeRespaldoDesdeJson(json, TimeZone.UTC).movimientos.single()
        assertEquals(5.0, m.amount)
        assertEquals(Categoria.Otros, m.category)
        assertEquals(PaymentSource.Otro, m.source)
        assertEquals(TipoMovimiento.Gasto, m.tipo)
        assertEquals(CanalMovimiento.Manual, m.canal)
        assertEquals(EstadoMovimiento.Pendiente, m.estado)
        assertEquals("", m.notes)
    }

    @Test
    fun laFechaDeUnMovimientoSeEscribeEnHoraLocalComoFlutter() {
        val json = ejemplo().aJson(TimeZone.of("America/Lima"))
        assertContains(json, "\"fecha\":\"2026-10-01T04:30:00.000\"")
        assertContains(json, "\"creado\":\"2026-10-01T12:00:00.000Z\"")
        assertContains(json, "\"app\":\"MiGasto\"")
        assertContains(json, "\"formato\":1")
    }
}
