package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Portado de test/movimiento_test.dart, grupo «Regla de duplicados».
class ReglaDuplicadosTest {
    private val base = fecha()

    private fun movimiento(
        monto: Double = 25.5,
        fuente: PaymentSource = PaymentSource.Yape,
        fecha: Instant = base,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
    ) = Movimiento(
        id = "x",
        amount = monto,
        merchant = "Tambo",
        category = Categoria.Compras,
        source = fuente,
        date = fecha,
        tipo = tipo,
    )

    @Test
    fun mismoMontoYFuenteA1MinutoEsDuplicado() {
        assertTrue(
            esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Yape, base + 1.minutes),
        )
    }

    @Test
    fun aMasDe2MinutosNoEsDuplicado() {
        assertFalse(
            esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Yape, base + 3.minutes),
        )
    }

    @Test
    fun otraFuenteUOtroMontoNoEsDuplicado() {
        assertFalse(esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Plin, base))
        assertFalse(esMovimientoDuplicado(listOf(movimiento()), 30.0, PaymentSource.Yape, base))
    }

    // Casos nuevos: los límites de la regla quedan fijados.
    @Test
    fun justoEn2MinutosTodaviaEsDuplicadoYUnSegundoMasYaNo() {
        assertTrue(
            esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Yape, base + 2.minutes),
        )
        assertFalse(
            esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Yape, base + 2.minutes + 1.seconds),
        )
    }

    @Test
    fun laVentanaValeTambienHaciaAtras() {
        assertTrue(
            esMovimientoDuplicado(listOf(movimiento()), 25.5, PaymentSource.Yape, base - 90.seconds),
        )
    }

    @Test
    fun unaDiferenciaMenorAMedioCentimoCuentaComoElMismoMonto() {
        assertTrue(
            esMovimientoDuplicado(listOf(movimiento()), 25.503, PaymentSource.Yape, base),
        )
        assertFalse(
            esMovimientoDuplicado(listOf(movimiento()), 25.51, PaymentSource.Yape, base),
        )
    }

    @Test
    fun siSeIndicaElTipoUnIngresoNoDuplicaAUnGasto() {
        val gasto = listOf(movimiento(tipo = TipoMovimiento.Gasto))
        assertFalse(esMovimientoDuplicado(gasto, 25.5, PaymentSource.Yape, base, TipoMovimiento.Ingreso))
        assertTrue(esMovimientoDuplicado(gasto, 25.5, PaymentSource.Yape, base, TipoMovimiento.Gasto))
        // Sin tipo, se compara solo monto, fuente y fecha.
        assertTrue(esMovimientoDuplicado(gasto, 25.5, PaymentSource.Yape, base))
    }

    @Test
    fun sinMovimientosPreviosNoHayDuplicado() {
        assertFalse(esMovimientoDuplicado(emptyList(), 25.5, PaymentSource.Yape, base))
    }
}
