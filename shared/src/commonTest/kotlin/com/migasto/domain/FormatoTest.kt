package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

// Nuevo: en Flutter el formato salía de NumberFormat('#,##0.00', 'en') dentro de cada pantalla.
class FormatoTest {
    @Test
    fun montosConMilesConComaYDosDecimalesConPunto() {
        assertEquals("2,655.30", formatearMonto(2655.3))
        assertEquals("0.00", formatearMonto(0.0))
        assertEquals("1,200.50", formatearMonto(1200.5))
        assertEquals("144.70", formatearMonto(144.7))
        assertEquals("1,234,567.89", formatearMonto(1234567.891))
        assertEquals("999.00", formatearMonto(999.0))
        assertEquals("1,000.00", formatearMonto(999.999))
    }

    @Test
    fun montosNegativosLlevanSigno() {
        assertEquals("-100.00", formatearMonto(-100.0))
        assertEquals("-1,500.25", formatearMonto(-1500.25))
        assertEquals("0.00", formatearMonto(-0.001))
    }

    @Test
    fun sinDecimales() {
        assertEquals("1,500", formatearMonto(1500.0, decimales = 0))
        assertEquals("1,500", formatearMonto(1500.4, decimales = 0))
        assertEquals("1,501", formatearMonto(1500.5, decimales = 0))
    }

    @Test
    fun solesLlevanElPrefijo() {
        assertEquals("S/ 2,655.30", formatearSoles(2655.3))
        assertEquals("S/ 1,500", formatearSoles(1500.0, decimales = 0))
    }

    @Test
    fun ingresosLlevanMasYGastosNo() {
        assertEquals("+ S/ 80.00", formatearSolesConSigno(80.0, esIngreso = true))
        assertEquals("S/ 25.00", formatearSolesConSigno(25.0, esIngreso = false))
    }

    @Test
    fun horaCortaEnLaZonaLocal() {
        assertEquals("12:41", formatearHora(fecha(), TimeZone.UTC))
        assertEquals("07:41", formatearHora(fecha(), TimeZone.of("America/Lima")))
        assertEquals("09:05", formatearHora(fecha(hora = 9, minuto = 5), TimeZone.UTC))
    }

    @Test
    fun nombresDeLosMeses() {
        assertEquals("Enero", nombreDelMes(1))
        assertEquals("Octubre", nombreDelMes(10))
        assertEquals("Diciembre", nombreDelMes(12))
        assertEquals(12, (1..12).map { nombreDelMes(it) }.toSet().size)
    }

    @Test
    fun textoDelEstadoDelPresupuesto() {
        assertEquals("Gastos: vas bien", textoDelEstadoDelPresupuesto(EstadoDelPresupuesto.Bien))
        assertEquals("Gastos: cerca del límite", textoDelEstadoDelPresupuesto(EstadoDelPresupuesto.Cerca))
        assertEquals("Gastos: te pasaste", textoDelEstadoDelPresupuesto(EstadoDelPresupuesto.Pasado))
    }

    @Test
    fun porcentajeUsadoDelPresupuesto() {
        val r = ResumenDelMes(0.0, 144.7, -144.7, 0.0, 0, 1500.0, 0.5)
        assertEquals("10 % de S/ 1,500", textoDelPresupuestoUsado(r))
    }

    @Test
    fun nombresDeLasFuentes() {
        assertEquals("Yape", PaymentSource.Yape.etiqueta)
        assertEquals("Plin", PaymentSource.Plin.etiqueta)
        assertEquals("Google Wallet", PaymentSource.GooglePay.etiqueta)
        assertEquals("Tarjeta", PaymentSource.Tarjeta.etiqueta)
        assertEquals("Efectivo", PaymentSource.Efectivo.etiqueta)
        assertEquals("Manual", PaymentSource.Manual.etiqueta)
        assertEquals("Manual", PaymentSource.Otro.etiqueta)
    }

    @Test
    fun textoDeLoQueEsperaConfirmacion() {
        fun m(tipo: TipoMovimiento) = Movimiento("x", 1.0, "x", Categoria.Otros, PaymentSource.Yape, fecha(), tipo = tipo)
        assertEquals("1 ingreso", textoPorConfirmar(listOf(m(TipoMovimiento.Ingreso))))
        assertEquals("2 ingresos", textoPorConfirmar(listOf(m(TipoMovimiento.Ingreso), m(TipoMovimiento.Ingreso))))
        assertEquals("2 movimientos", textoPorConfirmar(listOf(m(TipoMovimiento.Ingreso), m(TipoMovimiento.Gasto))))
        assertEquals("1 movimiento", textoPorConfirmar(listOf(m(TipoMovimiento.Gasto))))
    }
}
