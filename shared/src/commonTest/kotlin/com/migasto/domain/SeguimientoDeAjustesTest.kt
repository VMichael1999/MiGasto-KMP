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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: en Android esto vivía en AjustesViewModel (leer los ajustes y las acciones de la pantalla). Ahora lo
// comparten Android e iPhone.
class SeguimientoDeAjustesTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-seguimiento-ajustes.db"
    private val otro = "prueba-seguimiento-ajustes-2.db"
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 10, minuto = 14, mes = 10)

    @AfterTest
    fun limpiar() {
        fabrica.borrar(nombre)
        fabrica.borrar(otro)
    }

    private fun seguimientoDe(base: BaseMiGasto) = SeguimientoDeAjustes(base, crearRegistroDeMovimientos(base), { ahora }, utc)

    private fun probar(bloque: suspend (BaseMiGasto, SeguimientoDeAjustes) -> Unit) = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base -> bloque(base, seguimientoDe(base)) }
    }

    private fun gasto(id: String = "g1", conUbicacion: Boolean = false) = Movimiento(
        id = id, amount = 25.0, merchant = "Tambo", category = Categoria.Compras, source = PaymentSource.Efectivo,
        date = fecha(dia = 1, mes = 10), estado = EstadoMovimiento.Confirmado,
        latitud = if (conUbicacion) -12.09 else null, longitud = if (conUbicacion) -77.03 else null,
        lugar = if (conUbicacion) "San Isidro" else null,
    )

    @Test
    fun alEmpezarNoHayNadaDefinido() = probar { _, s ->
        val p = s.estado.first()
        assertEquals(0.0, p.presupuesto)
        assertEquals("Sin definir", p.textoDelPresupuesto)
        assertEquals("", p.presupuestoParaEditar)
        assertFalse(p.ingresosAutomaticos)
        assertEquals("Apagado: cada ingreso espera tu confirmación.", p.subtituloDeLosIngresosAutomaticos)
        assertEquals("Aún no has hecho uno. Si pierdes el teléfono, pierdes tus datos.", p.subtituloDelRespaldo)
        assertEquals(listOf("Yape", "Plin", "Google Wallet"), p.proveedores.map { it.nombre })
        assertEquals(listOf("yape", "plin", "googlePay"), p.proveedores.map { it.clave })
    }

    @Test
    fun elPresupuestoSeGuardaYSeQuita() = probar { _, s ->
        assertEquals("Presupuesto actualizado", s.guardarPresupuesto(1500.0))
        val p = s.estado.first()
        assertEquals("S/ 1,500.00", p.textoDelPresupuesto)
        assertEquals("1500", p.presupuestoParaEditar)
        assertEquals("Presupuesto quitado", s.guardarPresupuesto(0.0))
        assertEquals("Sin definir", s.estado.first().textoDelPresupuesto)
    }

    @Test
    fun losIngresosAutomaticosSeAlternan() = probar { _, s ->
        s.alternarIngresosAutomaticos()
        val p = s.estado.first()
        assertTrue(p.ingresosAutomaticos)
        assertEquals("Los ingresos se guardan solos, como los gastos.", p.subtituloDeLosIngresosAutomaticos)
        s.alternarIngresosAutomaticos()
        assertFalse(s.estado.first().ingresosAutomaticos)
    }

    @Test
    fun elMontoEnPantallaBloqueadaSeAlternaConSuTexto() = probar { _, s ->
        assertEquals("Oculto: solo dice «Pago detectado»", s.estado.first().subtituloDelMontoEnPantallaBloqueada)
        s.alternarMontoEnPantallaBloqueada()
        val p = s.estado.first()
        assertTrue(p.montoEnPantallaBloqueada)
        assertEquals("Visible: el aviso muestra el monto y el nombre", p.subtituloDelMontoEnPantallaBloqueada)
    }

    @Test
    fun apagarYPrenderUnProveedor() = probar { _, s ->
        s.alternarProveedor("plin")
        assertEquals(listOf(true, false, true), s.estado.first().proveedores.map { it.activo })
        s.alternarProveedor("plin")
        assertEquals(listOf(true, true, true), s.estado.first().proveedores.map { it.activo })
    }

    @Test
    fun lasListasSeVacianDeUnaEnUna() = probar { base, s ->
        base.ajustes.guardarAprendida("Wong", Categoria.Compras)
        base.ajustes.guardarAprendida("Rappi", Categoria.Alimentacion)
        base.ajustes.establecerAlias("BARBERÍA EL PATRÓN", "La barbería")
        val propia = base.categoriasPropias.agregar("Gimnasio", "gimnasio", TipoMovimiento.Gasto)
        val p = s.estado.first()
        assertEquals(listOf("rappi", "wong"), p.aprendidas.map { it.comercio.lowercase() })
        assertEquals(listOf("La barbería"), p.aliases.map { it.nombre })
        assertEquals(listOf("Gimnasio"), p.propias.map { it.nombre })

        s.olvidarAprendida(p.aprendidas.first().comercio)
        assertEquals(1, s.estado.first().aprendidas.size)
        s.olvidarTodasLasAprendidas()
        assertEquals(0, s.estado.first().aprendidas.size)
        s.quitarAlias(p.aliases.single().clave)
        assertEquals(0, s.estado.first().aliases.size)
        s.quitarPropia(propia.id)
        assertEquals(0, s.estado.first().propias.size)
    }

    @Test
    fun borrarLasUbicacionesConservaLosMovimientos() = probar { base, s ->
        base.movimientos.guardar(gasto("a", conUbicacion = true))
        base.movimientos.guardar(gasto("b"))
        s.borrarUbicaciones()
        val todos = base.movimientos.todos()
        assertEquals(2, todos.size)
        assertTrue(todos.none { it.tieneUbicacion })
    }

    @Test
    fun elCsvTraeLosMovimientosSinUbicacion() = probar { base, s ->
        base.movimientos.guardar(gasto(conUbicacion = true))
        val csv = s.csv()
        assertTrue(csv.contains("Tambo"))
        assertFalse(csv.contains("San Isidro"))
    }

    @Test
    fun elRespaldoSeCifraSeAbreYSeRestauraEnOtroTelefono() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.movimientos.guardar(gasto("a"))
            base.movimientos.guardar(gasto("b"))
            base.ajustes.guardarPresupuesto(1500.0)
            val s = seguimientoDe(base)

            val creado = s.crearRespaldo("una-contrasena-larga")
            assertNull(creado.error)
            val datos = assertNotNull(creado.datosEnBase64)
            s.respaldoGuardado()
            assertEquals("Último: 21 oct 2026, 10:14", s.estado.first().subtituloDelRespaldo)

            BaseMiGasto.abrir(fabrica, otro, "otra-clave").use { base2 ->
                val s2 = seguimientoDe(base2)
                val apertura = s2.abrirRespaldo(datos, "una-contrasena-larga")
                assertNull(apertura.error)
                assertEquals(2, apertura.nuevos)
                assertTrue(apertura.textoParaConfirmar!!.contains("2 movimientos en el respaldo, 2 nuevos"))
                assertEquals("Se restauraron 2 movimientos.", s2.restaurar(assertNotNull(apertura.contenido)))
                assertEquals(2, base2.movimientos.todos().size)
                assertEquals("No había nada nuevo: ya tenías todos esos movimientos.", s2.restaurar(assertNotNull(apertura.contenido)))
            }
        }
    }

    @Test
    fun conLaContrasenaEquivocadaOUnArchivoRotoElErrorSeDice() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.movimientos.guardar(gasto())
            val s = seguimientoDe(base)
            val datos = assertNotNull(s.crearRespaldo("una-contrasena-larga").datosEnBase64)
            val mala = s.abrirRespaldo(datos, "otra-contrasena-larga")
            assertNull(mala.contenido)
            assertNotNull(mala.error)
            val rota = s.abrirRespaldo("esto no es base64 ni un respaldo", "una-contrasena-larga")
            assertNull(rota.contenido)
            assertEquals("No se pudo abrir el respaldo.", rota.error)
        }
    }

    @Test
    fun unaContrasenaCortaNoCreaElRespaldo() = probar { _, s ->
        val r = s.crearRespaldo("corta")
        assertNull(r.datosEnBase64)
        assertNotNull(r.error)
    }

    @Test
    fun elNombreDelArchivoYLosTextosFijos() = probar { _, s ->
        assertEquals("MiGasto-respaldo-2026-10-21_1014.mgb", s.nombreDelArchivoDeRespaldo())
        assertEquals("Ajustes", TextosDeLaPantallaDeAjustes.TITULO)
        assertEquals("Registro automático", TextosDeLaPantallaDeAjustes.SECCION_REGISTRO)
        assertEquals("Tus datos se guardan solo en este teléfono.", TextosDeLaPantallaDeAjustes.PIE)
    }

    @Test
    fun elBloqueoSeActivaYSeApaga() = probar { base, s ->
        assertFalse(s.estado.first().bloqueoActivado)
        s.establecerBloqueo(true)
        assertTrue(s.estado.first().bloqueoActivado)
        assertTrue(base.ajustes.bloqueoActivado())
        s.establecerBloqueo(false)
        assertFalse(s.estado.first().bloqueoActivado)
    }
}
