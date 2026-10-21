package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Port de MyAccessibilityService.handleText (android/app/src/main/.../MyAccessibilityService.kt) y de la
// parte de PaymentNotificationListener que descarta notificaciones viejas o repetidas.
class ProcesadorDePagosTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-procesador.db"
    private var ahora: Instant = fecha(dia = 21, hora = 10, minuto = 0, mes = 10)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.procesador() = ProcesadorDePagos(
        lector = LectorDePagos.desdeJson(ReglasIncluidas.lector),
        ajustes = ajustes,
        leidos = LeidosEnAjustes(ajustes),
        guarda = GuardaDeDuplicados(2.minutes) { ahora },
    )

    private val notificacion = OrigenDeLectura.Notificaciones
    private val yapeIngreso = "Juan Pérez te yapeó S/ 15.00"
    private val constancia = "¡Yapeaste! S/ 1 Michael Anthony Valdiviezo Maza 01 oct. 2026 09:48 a. m. DATOS DE LA TRANSACCIÓN Nro. de celular *** *** 277 Destino Dale Nro. de operación 01614398"

    @Test
    fun unaNotificacionDeYapeEsUnPagoNuevo() {
        abrir().use { base ->
            val r = assertIs<ResultadoDeLectura.PagoNuevo>(base.procesador().procesar(yapeIngreso, notificacion))
            assertEquals(15.0, r.pago.amount)
            assertEquals("Juan Pérez", r.pago.peer)
            assertTrue(r.pago.esIngreso)
            assertEquals(yapeIngreso, r.texto)
        }
    }

    @Test
    fun unTextoSinPagoNoHaceNada() {
        abrir().use { base ->
            assertEquals(ResultadoDeLectura.SinPago, base.procesador().procesar("Tu pedido llegó. Total S/ 20.00", notificacion))
            assertEquals(ResultadoDeLectura.SinPago, base.procesador().procesar("Yape: tienes una nueva promoción", notificacion))
        }
    }

    @Test
    fun unProveedorApagadoSeDescarta() {
        abrir().use { base ->
            base.ajustes.guardarProveedoresActivos(mapOf("yape" to false, "plin" to true, "googlePay" to true))
            assertEquals(ResultadoDeLectura.ProveedorApagado, base.procesador().procesar(yapeIngreso, notificacion))
            assertIs<ResultadoDeLectura.PagoNuevo>(base.procesador().procesar("Plin: enviaste S/ 37.00 a Cineplanet", notificacion))
        }
    }

    @Test
    fun deUnaPantallaSoloCuentaLaConstancia() {
        abrir().use { base ->
            val inicio = "Yape Ver saldo S/ 1,250.00 Ocultar saldo Yapear Servicios Movimientos Juan te yapeó S/ 15.00"
            assertEquals(ResultadoDeLectura.SinPago, base.procesador().procesar(inicio, OrigenDeLectura.Pantalla))
            assertIs<ResultadoDeLectura.PagoNuevo>(base.procesador().procesar(constancia, OrigenDeLectura.Pantalla))
        }
    }

    @Test
    fun laMismaConstanciaVistaDosVecesSeLeeUnaSolaVezPorElNumeroDeOperacion() {
        abrir().use { base ->
            val p = base.procesador()
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar(constancia, OrigenDeLectura.Pantalla))
            ahora += 10.minutes // aunque pase la ventana de duplicados
            assertEquals(ResultadoDeLectura.YaLeido, p.procesar(constancia, OrigenDeLectura.Pantalla))
        }
    }

    @Test
    fun sinNumeroDeOperacionLaFechaYHoraIdentificanElPago() {
        abrir().use { base ->
            val sinNumero = "¡Yapeaste! S/ 1 Michael Anthony Valdiviezo Maza 01 oct. 2026 09:48 a. m. DATOS DE LA TRANSACCIÓN"
            val p = base.procesador()
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar(sinNumero, OrigenDeLectura.Pantalla))
            ahora += 10.minutes
            assertEquals(ResultadoDeLectura.YaLeido, p.procesar(sinNumero, OrigenDeLectura.Pantalla))
        }
    }

    @Test
    fun laNotificacionYLaPantallaDelMismoPagoCuentanUnaVez() {
        abrir().use { base ->
            val p = base.procesador()
            val enNotificacion = "Yape! MICHAEL ANTHONY VALDIVIEZO MAZA te envió un pago por S/ 1"
            val enPantalla = "¡Yapeaste! S/ 1 Michael 01 oct. 2026 09:48 a. m. DATOS DE LA TRANSACCIÓN"
            // Misma fuente, mismo monto, pero uno es ingreso y el otro gasto: son pagos distintos.
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar(enNotificacion, notificacion))
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar(enPantalla, OrigenDeLectura.Pantalla))
            // El mismo gasto visto por la notificación 5 segundos después de la constancia: duplicado.
            ahora += 5.seconds
            assertEquals(ResultadoDeLectura.Duplicado, p.procesar("Yapeaste S/ 1.00 a Michael", notificacion))
        }
    }

    @Test
    fun dosYapesDelMismoMontoSeguidosPorNotificacionValenLosDos() {
        abrir().use { base ->
            val p = base.procesador()
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar("Michael te yapeó S/ 1", notificacion))
            ahora += 60.seconds
            assertIs<ResultadoDeLectura.PagoNuevo>(p.procesar("Michael te yapeó S/ 1", notificacion))
        }
    }

    @Test
    fun elTextoSeRecortaA400Caracteres() {
        abrir().use { base ->
            val largo = "Yapeaste S/ 25.50 a Tambo " + "x".repeat(600)
            val r = assertIs<ResultadoDeLectura.PagoNuevo>(base.procesador().procesar(largo, notificacion))
            assertEquals(400, r.texto.length)
        }
    }

    // Notificaciones nuevas

    @Test
    fun unaNotificacionSoloSeLeeUnaVez() {
        abrir().use { base ->
            val p = base.procesador()
            assertTrue(p.esNotificacionNueva("n|clave|1000", antiguedad = 5.seconds))
            assertFalse(p.esNotificacionNueva("n|clave|1000", antiguedad = 6.seconds))
        }
    }

    @Test
    fun unaNotificacionVieja() {
        abrir().use { base ->
            val p = base.procesador()
            assertFalse(p.esNotificacionNueva("n|vieja|1", antiguedad = 3.minutes + 1.seconds))
            // Una vieja no se marca: si de verdad llegara fresca, se leería.
            assertTrue(p.esNotificacionNueva("n|vieja|1", antiguedad = 1.seconds))
        }
    }

    @Test
    fun losLeidosSobrevivenAlReabrirYSeGuardan200()  {
        abrir().use { base ->
            val leidos = LeidosEnAjustes(base.ajustes)
            assertTrue(leidos.marcarComoLeido("a"))
            repeat(250) { assertTrue(leidos.marcarComoLeido("id$it")) }
        }
        abrir().use { base ->
            val leidos = LeidosEnAjustes(base.ajustes)
            assertFalse(leidos.marcarComoLeido("id249"))
            assertFalse(leidos.marcarComoLeido("id50"))
            assertTrue(leidos.marcarComoLeido("a")) // el más viejo ya se olvidó
        }
    }
}
