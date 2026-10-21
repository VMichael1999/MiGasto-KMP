package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

// Port de la parte «sin ventana» de MyAccessibilityService.deliver (se guarda y se avisa con una
// notificación) y de los textos de PaymentNotifier.notify. Sin cola nativa: se escribe directo en shared.
class GuardarPagoLeidoTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-pago-leido.db"
    private val ahora: Instant = fecha(dia = 21, hora = 10, minuto = 0, mes = 10)
    private var contador = 0

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.registro() = RegistroDeMovimientos(
        movimientos, ajustes, ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias), { ahora }, { "ref-${++contador}" },
    )

    private fun pago(esIngreso: Boolean, monto: Double = 25.5, peer: String = "Tambo", fuente: PaymentSource = PaymentSource.Yape) =
        PagoLeido(monto, peer, fuente, esIngreso)

    @Test
    fun unGastoSeGuardaConfirmado() {
        abrir().use { base ->
            val r = base.registro().guardarPagoLeido(pago(esIngreso = false), "Yapeaste S/ 25.50 a Tambo")
            assertTrue(r.guardado)
            assertEquals("ref-1", r.movimiento.id)
            assertEquals(EstadoMovimiento.Confirmado, base.movimientos.todos().single().estado)
            assertEquals(Categoria.Compras, base.movimientos.todos().single().category)
            assertEquals(CanalMovimiento.Notificacion, base.movimientos.todos().single().canal)
            assertEquals("Yapeaste S/ 25.50 a Tambo", base.movimientos.todos().single().textoOriginal)
        }
    }

    @Test
    fun unIngresoQuedaPorConfirmarSalvoQueElUsuarioPidaGuardarlosSolos() {
        abrir().use { base ->
            val r = base.registro().guardarPagoLeido(pago(esIngreso = true, 15.0, "Juan Pérez"), "Juan Pérez te yapeó S/ 15.00")
            assertFalse(r.guardado)
            assertEquals(EstadoMovimiento.Pendiente, base.movimientos.todos().single().estado)
            assertEquals(Categoria.TransferenciaRecibida, base.movimientos.todos().single().category)
        }
        abrir().use { base ->
            base.ajustes.establecerGuardarIngresosAutomaticamente(true)
            val r = base.registro().guardarPagoLeido(pago(esIngreso = true, 15.0, "Ana"), "Ana te yapeó S/ 15.00")
            assertTrue(r.guardado)
            assertEquals(EstadoMovimiento.Confirmado, base.movimientos.todos().first { it.id == r.movimiento.id }.estado)
        }
    }

    @Test
    fun dosPagosIgualesLeidosPorNotificacionSeGuardanLosDos() {
        abrir().use { base ->
            val registro = base.registro()
            registro.guardarPagoLeido(pago(esIngreso = true, 1.0, "Michael"), "Michael te yapeó S/ 1")
            registro.guardarPagoLeido(pago(esIngreso = true, 1.0, "Michael"), "Michael te yapeó S/ 1")
            assertEquals(2, base.movimientos.todos().size)
        }
    }

    @Test
    fun laFuenteSeGuardaConSuClave() {
        abrir().use { base ->
            base.registro().guardarPagoLeido(pago(esIngreso = false, 95.0, "Saga Falabella", PaymentSource.GooglePay), "Compra por S/ 95.00 en Saga Falabella")
            assertEquals(PaymentSource.GooglePay, base.movimientos.todos().single().source)
        }
    }

    // Textos del aviso

    @Test
    fun losTextosDelAvisoDeUnIngresoPorConfirmar() {
        val t = textosDelAviso(15.0, "Juan Pérez", PaymentSource.Yape, esIngreso = true, guardado = false)
        assertEquals("Ingreso por confirmar: S/ 15.00", t.titulo)
        assertEquals("de Juan Pérez · Yape", t.texto)
        assertTrue(t.pideConfirmacion)
    }

    @Test
    fun losTextosDeUnIngresoYUnGastoGuardados() {
        val ingreso = textosDelAviso(1200.5, "Mi Empresa", PaymentSource.Plin, esIngreso = true, guardado = true)
        assertEquals("Ingreso guardado: S/ 1,200.50", ingreso.titulo)
        assertEquals("de Mi Empresa · Plin", ingreso.texto)
        assertFalse(ingreso.pideConfirmacion)
        val gasto = textosDelAviso(25.5, "Tambo", PaymentSource.GooglePay, esIngreso = false, guardado = true)
        assertEquals("Gasto guardado: S/ 25.50", gasto.titulo)
        assertEquals("a Tambo · Google Wallet", gasto.texto)
        assertFalse(gasto.pideConfirmacion)
    }

    @Test
    fun enLaPantallaBloqueadaSoloSeVeQueHuboUnPago() {
        assertEquals("MiGasto", TEXTOS_DEL_AVISO_PUBLICO.titulo)
        assertEquals("Pago detectado", TEXTOS_DEL_AVISO_PUBLICO.texto)
    }
}
