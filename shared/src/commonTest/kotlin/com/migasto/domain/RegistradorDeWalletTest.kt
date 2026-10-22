package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Port de RegistrarMovimientoIntent.swift y AmountParser (ios/Runner de la app de Flutter): la acción de Atajos que
// la automatización «Transacción» de Wallet llama al pagar con Apple Pay. Antes dejaba el pago en una cola para
// Flutter; ahora lo guarda `shared` directo, igual que en Android.
class RegistradorDeWalletTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-registrador-wallet.db"
    private val ahora = fecha(dia = 21, hora = 10, minuto = 0, mes = 10)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun probar(bloque: (BaseMiGasto, RegistradorDeWallet) -> Unit) {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            bloque(base, RegistradorDeWallet(crearRegistroDeMovimientos(base)) { ahora })
        }
    }

    // AmountParser

    @Test
    fun leeMontosComoLosEscribeAtajos() {
        assertEquals(25.5, leerMontoDeAtajos("25.50"))
        assertEquals(25.5, leerMontoDeAtajos("S/ 25,50"))
        assertEquals(1200.5, leerMontoDeAtajos("1,200.50"))
        assertEquals(1200.0, leerMontoDeAtajos("S/ 1,200"))
        assertEquals(80.0, leerMontoDeAtajos("80"))
    }

    @Test
    fun unMontoSinNumerosNoSeLee() {
        assertNull(leerMontoDeAtajos(""))
        assertNull(leerMontoDeAtajos("S/"))
        assertNull(leerMontoDeAtajos("abc"))
    }

    // La acción

    @Test
    fun guardaElGastoConfirmadoConTarjetaYCanalWallet() = probar { base, r ->
        val res = r.registrar("S/ 25,50", " Tambo ", "Visa BBVA ···4821", null, null)
        assertEquals(DesenlaceDeWallet.Guardado, res.desenlace)
        assertEquals("Registrado: S/ 25.50 en Tambo", res.titulo)
        assertEquals("Con Visa BBVA ···4821. Ábrelo para cambiar la categoría.", res.detalle)
        val m = base.movimientos.todos().single()
        assertEquals(25.5, m.amount)
        assertEquals("Tambo", m.merchant)
        assertEquals(PaymentSource.Tarjeta, m.source)
        assertEquals(CanalMovimiento.Wallet, m.canal)
        assertEquals(EstadoMovimiento.Confirmado, m.estado)
        assertEquals("Visa BBVA ···4821", m.tarjeta)
        assertEquals("Apple Pay: S/ 25.50 en Tambo", m.textoOriginal)
    }

    @Test
    fun sinComercioLoLlamaDesconocidoYSinTarjetaNoLaMenciona() = probar { base, r ->
        val res = r.registrar("10", "  ", null, null, null)
        assertEquals("Registrado: S/ 10.00 en Desconocido", res.titulo)
        assertEquals("Ábrelo para cambiar la categoría.", res.detalle)
        assertNull(base.movimientos.todos().single().tarjeta)
    }

    @Test
    fun laUbicacionDeAtajosSeGuardaSoloSiVienenLasDosCoordenadas() = probar { base, r ->
        r.registrar("10", "Tambo", null, -12.09, -77.03)
        val conUbicacion = base.movimientos.todos().single()
        assertTrue(conUbicacion.tieneUbicacion)
        assertEquals(-12.09, conUbicacion.latitud)
        base.movimientos.eliminar(conUbicacion.id)
        r.registrar("11", "Tambo", null, -12.09, null)
        assertTrue(!base.movimientos.todos().single().tieneUbicacion)
    }

    @Test
    fun unMontoInvalidoNoGuardaNadaYExplicaComoPasarlo() = probar { base, r ->
        val res = r.registrar("hola", "Tambo", null, null, null)
        assertEquals(DesenlaceDeWallet.MontoInvalido, res.desenlace)
        assertEquals("No pudimos leer el monto. Pásalo como 25.50.", res.titulo)
        assertTrue(base.movimientos.todos().isEmpty())
        assertEquals(DesenlaceDeWallet.MontoInvalido, r.registrar("0", "Tambo", null, null, null).desenlace)
    }

    @Test
    fun elMismoPagoDosVecesSeguidasSeGuardaUnaVez() = probar { base, r ->
        r.registrar("25", "Tambo", "Visa", null, null)
        val otra = r.registrar("25", "Tambo", "Visa", null, null)
        assertEquals(DesenlaceDeWallet.Duplicado, otra.desenlace)
        assertEquals("Ese pago ya estaba registrado.", otra.titulo)
        assertEquals(1, base.movimientos.todos().size)
    }
}
