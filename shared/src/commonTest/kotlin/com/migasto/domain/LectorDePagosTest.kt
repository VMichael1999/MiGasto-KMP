package com.migasto.domain

import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Portado de test/payment_reader_test.dart. Textos con el formato de las notificaciones
// de Yape, Plin y Google Wallet; las reglas son las del JSON de Flutter.
class LectorDePagosTest {
    private val lector = LectorDePagos.desdeJson(ReglasIncluidas.lector)

    private fun leer(texto: String): PagoLeido = assertNotNull(lector.leer(texto), "no se leyó: $texto")

    // gastos

    @Test
    fun yapeasteAUnComercio() {
        val p = leer("Yapeaste S/ 25.50 a Tambo")
        assertFalse(p.esIngreso)
        assertEquals(25.5, p.amount)
        assertEquals("Tambo", p.peer)
        assertEquals(PaymentSource.Yape, p.provider)
    }

    @Test
    fun pagasteConGoogleWallet() {
        val p = leer("Google Wallet Compra por S/ 95.00 en Saga Falabella")
        assertFalse(p.esIngreso)
        assertEquals(95.0, p.amount)
        assertEquals("Saga Falabella", p.peer)
        assertEquals(PaymentSource.GooglePay, p.provider)
    }

    @Test
    fun googleWalletSinVerboSeTomaComoGasto() {
        val p = leer("Google Pay S/ 8.90 Oxxo")
        assertFalse(p.esIngreso)
        assertEquals(PaymentSource.GooglePay, p.provider)
    }

    @Test
    fun elComercioNoArrastraConGoogleWallet() {
        val p = leer("Compra por S/ 89.20 en Metro con Google Wallet")
        assertEquals("Metro", p.peer)
        assertEquals(PaymentSource.GooglePay, p.provider)
    }

    @Test
    fun notificacionRealDeYapePagoRecibido() {
        val t = "Yape! MICHAEL ANTHONY VALDIVIEZO MAZA te envió un pago por S/ 1"
        val p = leer("Confirmación de Pago $t $t")
        assertTrue(p.esIngreso)
        assertEquals(1.0, p.amount)
        assertEquals(PaymentSource.Yape, p.provider)
        assertEquals("MICHAEL ANTHONY VALDIVIEZO MAZA", p.peer)
    }

    @Test
    fun constanciaRealDeYapeAlEnviarElNombreVaDespuesDelMonto() {
        val p = leer(
            "¡Yapeaste! S/ 1 Michael Anthony Valdiviezo Maza 01 oct. 2026 " +
                "09:48 a. m. DATOS DE LA TRANSACCIÓN Nro. de celular *** *** 277 Destino Dale",
        )
        assertFalse(p.esIngreso)
        assertEquals(1.0, p.amount)
        assertEquals(PaymentSource.Yape, p.provider)
        assertEquals("Michael Anthony Valdiviezo Maza", p.peer)
    }

    @Test
    fun constanciaRealDeYapeAlPagarUnServicioLaEmpresaEsElNombre() {
        val p = leer(
            "¡Yapeaste el servicio! S/ 134.85 BanBif 01 oct. 2026 12:36 p. m. " +
                "DATOS DE LA TRANSACCIÓN Servicio: Pago de Cuotas Prestamos Código de cliente: " +
                "73654903 Titular: VALDIVIEZO MAZ* Nº de operación: 05205445",
        )
        assertFalse(p.esIngreso)
        assertEquals(134.85, p.amount)
        assertEquals(PaymentSource.Yape, p.provider)
        assertEquals("BanBif", p.peer)
    }

    @Test
    fun plinEnviado() {
        val p = leer("Plin: enviaste S/ 37.00 a Cineplanet")
        assertFalse(p.esIngreso)
        assertEquals(PaymentSource.Plin, p.provider)
        assertEquals("Cineplanet", p.peer)
    }

    // ingresos

    @Test
    fun juanTeYapeoEsIngresoNoGasto() {
        val p = leer("Juan Pérez te yapeó S/ 15.00")
        assertTrue(p.esIngreso)
        assertEquals(15.0, p.amount)
        assertEquals("Juan Pérez", p.peer)
    }

    @Test
    fun recibisteDeEsIngreso() {
        val p = leer("Yape: Recibiste S/ 12.50 de KFC")
        assertTrue(p.esIngreso)
        assertEquals("KFC", p.peer)
    }

    @Test
    fun hasRecibidoUnYapeDe() {
        val p = leer("Has recibido un Yape de María López por S/ 40.00")
        assertTrue(p.esIngreso)
        assertEquals(40.0, p.amount)
        assertEquals("María López", p.peer)
    }

    @Test
    fun plinRecibido() {
        val p = leer("Te plineó Carlos Ruiz S/ 85.00 · Plin")
        assertTrue(p.esIngreso)
        assertEquals(PaymentSource.Plin, p.provider)
    }

    // montos

    @Test
    fun milesConComaYDecimalesConPunto() {
        assertEquals(1200.5, leer("Yapeaste S/ 1,200.50 a Wong").amount)
    }

    @Test
    fun decimalConComa() {
        assertEquals(12.5, leer("Yapeaste S/ 12,50 a Wong").amount)
    }

    @Test
    fun sinDecimales() {
        assertEquals(30.0, leer("Yapeaste S/ 30 a Wong").amount)
    }

    // no se inventa nada

    @Test
    fun textoDeOtraApp() {
        assertNull(lector.leer("Tu pedido llegó. Total S/ 20.00"))
    }

    @Test
    fun yapeSinMonto() {
        assertNull(lector.leer("Yape: tienes una nueva promoción"))
    }

    @Test
    fun yapeConMontoPeroSinSaberSiEntraOSale() {
        assertNull(lector.leer("Yape S/ 20.00"))
    }

    @Test
    fun explinNoCuentaComoPlin() {
        assertNull(lector.leer("Explin S/ 20.00 pagaste"))
    }

    // número de operación

    @Test
    fun tomaElNumeroDeOperacionDeLasConstanciasReales() {
        assertEquals(
            "01614398",
            lector.numeroDeOperacion(
                "DATOS DE LA TRANSACCIÓN Nro. de celular *** *** 277 Destino Dale " +
                    "Nro. de operación 01614398 Más en Yape",
            ),
        )
        assertEquals(
            "05205445",
            lector.numeroDeOperacion("Titular: VALDIVIEZO MAZ* Nº de operación: 05205445 Yapear otro servicio"),
        )
    }

    @Test
    fun laFechaYHoraDeLaConstanciaIdentificanElPagoSiFaltaElNumero() {
        assertEquals(
            "01 oct. 2026 09:48 a. m.",
            lector.selloDeConstancia("¡Yapeaste! S/ 1 Michael 01 oct. 2026 09:48 a. m. DATOS"),
        )
        assertEquals(
            "01 oct. 2026 12:36 p. m.",
            lector.selloDeConstancia("S/ 134.85 BanBif 01 oct. 2026 12:36 p. m. DATOS"),
        )
        assertNull(lector.selloDeConstancia("Yape! MICHAEL te envió un pago por S/ 1"))
    }

    @Test
    fun deUnaPantallaSoloCuentaLaConstanciaNoElInicioConElSaldo() {
        assertTrue(
            lector.esConstanciaDePantalla(
                "¡Yapeaste! S/ 1 Michael 01 oct. 2026 09:48 a. m. DATOS DE LA TRANSACCIÓN Nro. de celular",
            ),
        )
        assertTrue(
            lector.esConstanciaDePantalla("¡Yapeaste el servicio! S/ 134.85 BanBif DATOS DE LA TRANSACCIÓN Servicio"),
        )
        assertFalse(
            lector.esConstanciaDePantalla(
                "Yape Ver saldo S/ 1,250.00 Ocultar saldo Yapear Servicios Movimientos Juan te yapeó S/ 15.00",
            ),
        )
    }

    @Test
    fun unTextoSinNumeroDeOperacionNoInventaUno() {
        assertNull(lector.numeroDeOperacion("Yape! MICHAEL te envió un pago por S/ 1"))
        assertNull(lector.numeroDeOperacion("Código de cliente: 73654903"))
    }

    // casos nuevos

    @Test
    fun siNoSeSabeQuienElNombreEsDesconocido() {
        assertEquals("Desconocido", leer("Yape: Recibiste S/ 10.00").peer)
    }

    @Test
    fun unMontoCeroNoEsUnPago() {
        assertNull(lector.leer("Yapeaste S/ 0.00 a Tambo"))
    }

    @Test
    fun elTipoEnTextoCoincideConElDeFlutter() {
        assertEquals("gasto", leer("Yapeaste S/ 25.50 a Tambo").tipoTexto)
        assertEquals("ingreso", leer("Juan Pérez te yapeó S/ 15.00").tipoTexto)
    }
}
