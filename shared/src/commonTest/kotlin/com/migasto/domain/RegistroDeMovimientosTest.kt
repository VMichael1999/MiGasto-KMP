package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Portado de test/movimiento_test.dart («Ingresos» y «Cola nativa») y de test/expenses_notifier_test.dart.
// En Flutter la cola nativa guardaba JSON; aquí el código nativo llama directo a `registrarPagoNativo`.
class RegistroDeMovimientosTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-registro.db"
    private val ahora = fecha(dia = 27, hora = 12, minuto = 41)
    private var contador = 0

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.registro(reloj: () -> Instant = { ahora }) = RegistroDeMovimientos(
        movimientos = movimientos,
        ajustes = ajustes,
        clasificador = ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias),
        reloj = reloj,
        nuevoId = { "id${++contador}" },
    )

    private suspend fun BaseMiGasto.pagoDeJuan(registro: RegistroDeMovimientos, pendiente: Movimiento? = null) =
        registro.registrarPagoDetectado(
            monto = 15.0,
            comercio = "Juan Pérez",
            proveedor = "yape",
            textoOriginal = "Juan Pérez te yapeó S/ 15.00",
            tipo = TipoMovimiento.Ingreso,
            pendienteActual = pendiente,
        )

    // expenses_notifier_test

    @Test
    fun alInicioNoHayMovimientosNiPresupuesto() {
        abrir().use { base ->
            assertEquals(emptyList(), base.movimientos.todos())
            assertEquals(0.0, base.ajustes.presupuesto()) // no se inventa uno
        }
    }

    @Test
    fun unPagoDetectadoQuedaComoAvisoPendienteConSuCategoria() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val r = base.registro().registrarPagoDetectado(25.5, "Tambo", "yape", "Yapeaste S/ 25.50 a Tambo")
            val aviso = assertIs<ResultadoDePago.Aviso>(r).movimiento
            assertEquals(25.5, aviso.amount)
            assertEquals("Tambo", aviso.merchant)
            assertEquals(Categoria.Compras, aviso.category) // Tambo se clasifica como Compras
            assertTrue(!aviso.isConfirmed)
        }
    }

    @Test
    fun confirmarElPendienteLoGuardaYAprendeLaCategoria() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            val aviso = assertIs<ResultadoDePago.Aviso>(
                registro.registrarPagoDetectado(35.0, "Tambo", "yape", "Yapeaste S/ 35.00 a Tambo"),
            ).movimiento

            val guardado = registro.confirmarPendiente(aviso, Categoria.Compras, "Nota de prueba")

            val lista = base.movimientos.todos()
            assertEquals(guardado.id, lista.first().id)
            assertEquals("Tambo", lista.first().merchant)
            assertEquals(35.0, lista.first().amount)
            assertEquals(Categoria.Compras, lista.first().category)
            assertEquals("Nota de prueba", lista.first().notes)
            assertTrue(lista.first().isConfirmed)
            assertEquals(Categoria.Compras, base.ajustes.categoriaAprendida("Tambo"))
        }
    }

    // movimiento_test: Ingresos

    @Test
    fun conGuardarIngresosAutomaticamenteElIngresoQuedaConfirmado() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            base.ajustes.establecerGuardarIngresosAutomaticamente(true)
            val r = base.pagoDeJuan(base.registro())
            val guardado = assertIs<ResultadoDePago.GuardadoAutomaticamente>(r).movimiento
            assertEquals(EstadoMovimiento.Confirmado, guardado.estado)
            assertEquals(listOf(guardado.id), base.movimientos.todos().map { it.id })
        }
    }

    @Test
    fun unIngresoDetectadoQuedaPendienteYNoCuentaComoConfirmado() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            val aviso = assertIs<ResultadoDePago.Aviso>(base.pagoDeJuan(registro)).movimiento
            val guardado = base.movimientos.todos().single()
            assertEquals(TipoMovimiento.Ingreso, guardado.tipo)
            assertEquals(EstadoMovimiento.Pendiente, guardado.estado)
            assertEquals(Categoria.TransferenciaRecibida, guardado.category)
            assertEquals(aviso.id, guardado.id)

            // Ignorar el aviso no lo borra: sigue en «Por confirmar». Luego se confirma.
            assertTrue(!base.movimientos.todos().single().isConfirmed)
            registro.confirmar(guardado.id)
            assertTrue(base.movimientos.todos().single().isConfirmed)
        }
    }

    @Test
    fun unGastoDetectadoNoSeGuardaHastaConfirmarlo() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val r = base.registro().registrarPagoDetectado(25.5, "Tambo", "yape", "Yapeaste S/ 25.50 a Tambo")
            assertEquals(TipoMovimiento.Gasto, assertIs<ResultadoDePago.Aviso>(r).movimiento.tipo)
            assertEquals(emptyList(), base.movimientos.todos())
        }
    }

    @Test
    fun elMismoPagoPorDosViasSeRegistraUnaSolaVez() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            base.pagoDeJuan(registro)
            assertIs<ResultadoDePago.Duplicado>(base.pagoDeJuan(registro))
            assertEquals(1, base.movimientos.todos().size)
        }
    }

    @Test
    fun unGastoPendienteEnElAvisoTambienCuentaParaLosDuplicados() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            val aviso = assertIs<ResultadoDePago.Aviso>(
                registro.registrarPagoDetectado(25.5, "Tambo", "yape", "Yapeaste S/ 25.50 a Tambo"),
            ).movimiento
            assertIs<ResultadoDePago.Duplicado>(
                registro.registrarPagoDetectado(25.5, "Tambo", "yape", "Yapeaste S/ 25.50 a Tambo", pendienteActual = aviso),
            )
        }
    }

    @Test
    fun restaurarDevuelveUnGastoEliminado() {
        abrir().use { base ->
            val registro = base.registro()
            registro.agregarManual(TipoMovimiento.Gasto, 20.0, "Menú", Categoria.Alimentacion)
            val m = base.movimientos.todos().single()
            registro.eliminar(m.id)
            assertEquals(emptyList(), base.movimientos.todos())
            registro.restaurar(m)
            assertEquals(m.id, base.movimientos.todos().single().id)
            // Restaurar dos veces no duplica.
            registro.restaurar(m)
            assertEquals(1, base.movimientos.todos().size)
        }
    }

    // movimiento_test: Cola nativa (ahora, llamadas directas)

    private fun pago(
        monto: Double = 25.5,
        nombre: String = "Tambo",
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        confirmado: Boolean = true,
        en: Instant = ahora,
    ) = PagoNativo(
        amount = monto,
        peer = nombre,
        provider = "yape",
        tipo = tipo,
        textoOriginal = if (tipo == TipoMovimiento.Gasto) "Yapeaste S/ 25.50 a Tambo" else "Juan Pérez te yapeó S/ 15.00",
        confirmado = confirmado,
        en = en,
    )

    @Test
    fun guardaLosPagosDetectadosConLaAppCerrada() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            registro.registrarPagoNativo(pago())
            registro.registrarPagoNativo(pago(15.0, "Juan Pérez", TipoMovimiento.Ingreso, confirmado = false))
            // El mismo gasto llegó dos veces.
            assertNull(registro.registrarPagoNativo(pago(en = ahora + 5.seconds)))

            val lista = base.movimientos.todos()
            assertEquals(2, lista.size)
            val gasto = lista.first { it.esGasto }
            assertEquals(EstadoMovimiento.Confirmado, gasto.estado)
            assertEquals(Categoria.Compras, gasto.category)
            assertEquals(CanalMovimiento.Notificacion, gasto.canal)
            assertEquals("Yapeaste S/ 25.50 a Tambo", gasto.textoOriginal)
            val ingreso = lista.first { it.esIngreso }
            assertEquals(EstadoMovimiento.Pendiente, ingreso.estado)
            assertEquals(Categoria.TransferenciaRecibida, ingreso.category)
        }
    }

    @Test
    fun dosYapesDelMismoMontoSeguidosDeAndroidSeGuardanLosDos() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            fun yape(en: Instant) = pago(1.0, "Michael", TipoMovimiento.Ingreso, confirmado = false, en = en)
                .copy(desdeAndroid = true)
            registro.registrarPagoNativo(yape(ahora))
            registro.registrarPagoNativo(yape(ahora + 1.minutes))
            assertEquals(2, base.movimientos.todos().size)
        }
    }

    @Test
    fun guardarDelAvisoConfirmaElIngresoPendiente() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            registro.registrarPagoNativo(pago(15.0, "Juan Pérez", TipoMovimiento.Ingreso, confirmado = false).copy(id = "ref-1"))
            registro.confirmar("ref-1")
            val m = base.movimientos.todos().single()
            assertEquals("ref-1", m.id)
            assertEquals(EstadoMovimiento.Confirmado, m.estado)
        }
    }

    @Test
    fun ignorarDelAvisoDescartaElIngresoPendiente() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            registro.registrarPagoNativo(pago(15.0, "Juan Pérez", TipoMovimiento.Ingreso, confirmado = false).copy(id = "ref-2"))
            registro.eliminar("ref-2")
            assertEquals(emptyList(), base.movimientos.todos())
        }
    }

    @Test
    fun unaOrdenDelAvisoParaUnMovimientoQueYaNoExisteNoHaceNada() {
        abrir().use { base ->
            val registro = base.registro()
            registro.confirmar("no-existe")
            registro.eliminar("no-existe")
            assertEquals(emptyList(), base.movimientos.todos())
        }
    }

    @Test
    fun unPagoDeApplePayGuardaTarjetaCanalWalletYUbicacion() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            base.registro().registrarPagoNativo(
                PagoNativo(
                    amount = 25.5, peer = "Tambo", provider = "tarjeta", tipo = TipoMovimiento.Gasto,
                    canal = CanalMovimiento.Wallet, tarjeta = "Visa BBVA ···4821", latitud = -12.0914, longitud = -77.0292,
                    textoOriginal = "Apple Pay: S/ 25.50 en Tambo", confirmado = true, en = ahora,
                ),
            )
            val m = base.movimientos.todos().single()
            assertEquals(PaymentSource.Tarjeta, m.source)
            assertEquals(CanalMovimiento.Wallet, m.canal)
            assertEquals("Visa BBVA ···4821", m.tarjeta)
            assertTrue(m.tieneUbicacion)
            assertEquals(EstadoMovimiento.Confirmado, m.estado)
            assertEquals(Categoria.Compras, m.category)
        }
    }

    // Casos nuevos

    @Test
    fun laCategoriaElegidaEnLaCapturaSeRespetaSiCorrespondeAlTipo() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            val registro = base.registro()
            registro.registrarPagoNativo(pago().copy(categoriaElegida = Categoria.Transporte, id = "a"))
            // Una categoría de ingreso en un gasto no corresponde: se clasifica por reglas.
            registro.registrarPagoNativo(pago(30.0, "Tambo").copy(categoriaElegida = Categoria.Sueldo, id = "b"))
            assertEquals(Categoria.Transporte, base.movimientos.todos().first { it.id == "a" }.category)
            assertEquals(Categoria.Compras, base.movimientos.todos().first { it.id == "b" }.category)
        }
    }

    @Test
    fun laCategoriaAprendidaGanaALasReglasEnLosGastosPeroNoEnLosIngresos() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            base.ajustes.guardarAprendida("Tambo", Categoria.Entretenimiento)
            base.ajustes.guardarAprendida("Juan Pérez", Categoria.Sueldo) // es de ingreso: no se usa en un gasto
            val registro = base.registro()
            val gasto = assertIs<ResultadoDePago.Aviso>(
                registro.registrarPagoDetectado(25.5, "Tambo", "yape", "Yapeaste S/ 25.50 a Tambo"),
            ).movimiento
            assertEquals(Categoria.Entretenimiento, gasto.category)
            val gastoAJuan = assertIs<ResultadoDePago.Aviso>(
                registro.registrarPagoDetectado(7.0, "Juan Pérez", "yape", "Yapeaste S/ 7.00 a Juan Pérez"),
            ).movimiento
            assertNotNull(gastoAJuan)
            assertTrue(!gastoAJuan.category.esDeIngreso)
        }
    }

    @Test
    fun unProveedorApagadoNoGeneraAviso() = kotlinx.coroutines.test.runTest {
        abrir().use { base ->
            base.ajustes.guardarProveedoresActivos(mapOf("yape" to true, "plin" to false, "googlePay" to true))
            val registro = base.registro()
            assertIs<ResultadoDePago.ProveedorApagado>(
                registro.alDetectarPago(10.0, "Cineplanet", "plin", "Plin: enviaste S/ 10.00 a Cineplanet"),
            )
            assertIs<ResultadoDePago.Aviso>(
                registro.alDetectarPago(10.0, "Cineplanet", "yape", "Yapeaste S/ 10.00 a Cineplanet"),
            )
        }
    }

    @Test
    fun agregarManualCreaUnMovimientoConfirmadoConElCanalManual() {
        abrir().use { base ->
            base.registro().agregarManual(
                tipo = TipoMovimiento.Ingreso, monto = 180.0, comercio = "Venta de bicicleta", categoria = Categoria.Venta,
            )
            val m = base.movimientos.todos().single()
            assertEquals(TipoMovimiento.Ingreso, m.tipo)
            assertEquals(180.0, m.amount)
            assertEquals(Categoria.Venta, m.category)
            assertEquals(EstadoMovimiento.Confirmado, m.estado)
            assertEquals(CanalMovimiento.Manual, m.canal)
            assertEquals("Registro manual", m.notes)
            assertEquals(ahora, m.date)
        }
    }

    @Test
    fun confirmarPuedeCambiarLaCategoria() {
        abrir().use { base ->
            val registro = base.registro()
            registro.agregarManual(TipoMovimiento.Ingreso, 50.0, "X", Categoria.OtrosIngresos)
            val id = base.movimientos.todos().single().id
            registro.confirmar(id, Categoria.Venta)
            assertEquals(Categoria.Venta, base.movimientos.todos().single().category)
        }
    }
}
