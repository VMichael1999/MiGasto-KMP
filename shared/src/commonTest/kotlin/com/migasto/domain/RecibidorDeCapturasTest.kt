package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Nuevo: lo que iPhone hace con el texto que Vision lee de una captura compartida (en Android, la accesibilidad
// lee la pantalla). Las reglas son las de siempre: solo cuenta la constancia, y lo ya leído no se repite.
class RecibidorDeCapturasTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-recibidor-capturas.db"
    private val ahora = fecha(dia = 21, hora = 10, minuto = 0, mes = 10)
    private val constancia = "¡Yapeaste! S/ 25 Wong 21 oct. 2026 09:48 a. m. DATOS DE LA TRANSACCIÓN Nro. de celular *** *** 277 Destino Dale Nro. de operación 01614398"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun probar(bloque: (BaseMiGasto, RecibidorDeCapturas) -> Unit) {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            bloque(base, RecibidorDeCapturas(base, crearRegistroDeMovimientos(base)) { ahora })
        }
    }

    @Test
    fun laConstanciaDeUnGastoSeGuardaConfirmadaPorCaptura() = probar { base, r ->
        val res = r.recibir(constancia)
        assertEquals(DesenlaceDeCaptura.Guardado, res.desenlace)
        assertEquals("Gasto guardado: S/ 25.00", res.titulo)
        val m = base.movimientos.todos().single()
        assertEquals(CanalMovimiento.Captura, m.canal)
        assertEquals(EstadoMovimiento.Confirmado, m.estado)
        assertEquals(25.0, m.amount)
        assertEquals(PaymentSource.Yape, m.source)
    }

    @Test
    fun laMismaConstanciaDosVecesSeRegistraUnaSolaVez() = probar { base, r ->
        r.recibir(constancia)
        val otra = r.recibir(constancia)
        assertEquals(DesenlaceDeCaptura.YaRegistrado, otra.desenlace)
        assertEquals("Ese pago ya estaba registrado.", otra.titulo)
        assertEquals(1, base.movimientos.todos().size)
    }

    @Test
    fun unPagoQueYaLlegoPorNotificacionNoSeDuplica() = probar { base, r ->
        crearRegistroDeMovimientos(base).guardarPagoLeido(
            PagoLeido(25.0, "Wong", PaymentSource.Yape, esIngreso = false), "Yapeaste S/ 25",
        )
        // Hace menos de 2 minutos, mismo monto y fuente.
        assertEquals(DesenlaceDeCaptura.YaRegistrado, r.recibir(constancia).desenlace)
        assertEquals(1, base.movimientos.todos().size)
    }

    @Test
    fun unTextoQueNoEsUnPagoLoDice() = probar { base, r ->
        val res = r.recibir("Hola, esto es una foto de mi perro")
        assertEquals(DesenlaceDeCaptura.SinPago, res.desenlace)
        assertEquals("No encontramos un pago en la captura.", res.titulo)
        assertTrue(res.detalle.contains("constancia"))
        assertTrue(base.movimientos.todos().isEmpty())
    }

    @Test
    fun conElProveedorApagadoNoSeGuardaNada() = probar { base, r ->
        base.ajustes.guardarProveedoresActivos(mapOf("yape" to false, "plin" to true, "googlePay" to true))
        val res = r.recibir(constancia)
        assertEquals(DesenlaceDeCaptura.ProveedorApagado, res.desenlace)
        assertEquals("Yape está apagado en Ajustes.", res.titulo)
        assertTrue(base.movimientos.todos().isEmpty())
    }
}
