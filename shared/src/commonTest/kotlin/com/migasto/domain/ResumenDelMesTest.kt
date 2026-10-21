package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

// Nuevo: en Flutter estas cuentas vivían en DashboardScreen. Los datos son los del diseño (HTML):
// ingresos S/ 2,800.00, gastos S/ 144.70, quedan S/ 2,655.30, por confirmar S/ 230.00, presupuesto
// S/ 1,500 con el 10 % usado.
class ResumenDelMesTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 15, hora = 12, minuto = 0, mes = 10)

    private fun mov(
        id: String,
        monto: Double,
        tipo: TipoMovimiento,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        dia: Int = 10,
        mes: Int = 10,
    ) = Movimiento(
        id = id,
        amount = monto,
        merchant = id,
        category = Categoria.Otros,
        source = PaymentSource.Manual,
        date = fecha(dia = dia, mes = mes),
        tipo = tipo,
        estado = estado,
    )

    private val deEjemplo = listOf(
        mov("sueldo", 2800.0, TipoMovimiento.Ingreso),
        mov("tambo", 25.5, TipoMovimiento.Gasto),
        mov("uber", 19.2, TipoMovimiento.Gasto),
        mov("rappi", 100.0, TipoMovimiento.Gasto),
        mov("venta", 230.0, TipoMovimiento.Ingreso, estado = EstadoMovimiento.Pendiente),
    )

    private fun resumen(movs: List<Movimiento> = deEjemplo, presupuesto: Double = 1500.0) =
        calcularResumenDelMes(movs, presupuesto, ahora, utc)

    @Test
    fun losMontosDelDisenoCuadran() {
        val r = resumen()
        assertEquals(2800.0, r.ingresos, 0.001)
        assertEquals(144.7, r.gastos, 0.001)
        assertEquals(2655.3, r.saldo, 0.001)
        assertEquals(230.0, r.porConfirmar, 0.001)
        assertEquals(1, r.cantidadPorConfirmar)
        assertEquals(10, r.porcentajeDelPresupuesto)
        assertEquals(EstadoDelPresupuesto.Bien, r.estadoDelPresupuesto)
    }

    @Test
    fun soloSumaLoConfirmadoDelMesActual() {
        val r = resumen(
            deEjemplo + listOf(
                mov("viejo", 999.0, TipoMovimiento.Gasto, mes = 9),
                mov("pendiente", 50.0, TipoMovimiento.Gasto, estado = EstadoMovimiento.Pendiente),
                mov("otroAnio", 5.0, TipoMovimiento.Gasto).copiar(date = fecha(anio = 2025, mes = 10)),
            ),
        )
        assertEquals(144.7, r.gastos, 0.001)
        assertEquals(2800.0, r.ingresos, 0.001)
    }

    @Test
    fun loPorConfirmarCuentaCualquierMesYElMontoEsSoloDeIngresos() {
        // Como en el banner de Flutter: el monto es lo que van a sumar los ingresos pendientes.
        val r = resumen(
            deEjemplo +
                mov("viejo", 70.0, TipoMovimiento.Ingreso, estado = EstadoMovimiento.Pendiente, mes = 8) +
                mov("gasto", 40.0, TipoMovimiento.Gasto, estado = EstadoMovimiento.Pendiente),
        )
        assertEquals(300.0, r.porConfirmar, 0.001)
        assertEquals(3, r.cantidadPorConfirmar)
    }

    @Test
    fun sinMovimientosTodoEsCero() {
        val r = resumen(emptyList())
        assertEquals(0.0, r.saldo)
        assertEquals(0, r.cantidadPorConfirmar)
        assertEquals(0, r.porcentajeDelPresupuesto)
    }

    @Test
    fun sinPresupuestoNoHayPorcentajeNiEstado() {
        val r = resumen(presupuesto = 0.0)
        assertEquals(false, r.tienePresupuesto)
        assertEquals(0, r.porcentajeDelPresupuesto)
    }

    @Test
    fun elEstadoDelPresupuestoCambiaEn75YEn100() {
        fun estado(gastado: Double) = resumen(listOf(mov("g", gastado, TipoMovimiento.Gasto)), 1000.0).estadoDelPresupuesto
        assertEquals(EstadoDelPresupuesto.Bien, estado(749.0))
        assertEquals(EstadoDelPresupuesto.Cerca, estado(750.0))
        assertEquals(EstadoDelPresupuesto.Cerca, estado(1000.0))
        assertEquals(EstadoDelPresupuesto.Pasado, estado(1000.01))
    }

    @Test
    fun elPorcentajeSeRedondea() {
        assertEquals(10, resumen().porcentajeDelPresupuesto) // 9.65 %
        assertEquals(125, resumen(listOf(mov("g", 1250.0, TipoMovimiento.Gasto)), 1000.0).porcentajeDelPresupuesto)
    }

    @Test
    fun elSaldoPuedeSerNegativo() {
        assertEquals(-100.0, resumen(listOf(mov("g", 100.0, TipoMovimiento.Gasto))).saldo, 0.001)
    }

    @Test
    fun laFraccionDelMesSigueElDia() {
        assertEquals(15.0 / 31.0, resumen().fraccionDelMes, 0.0001)
        assertEquals(1.0, calcularResumenDelMes(emptyList(), 1.0, fecha(dia = 28, mes = 2), utc).fraccionDelMes, 0.0001)
    }

    @Test
    fun elMesSeCuentaEnLaHoraLocal() {
        val lima = TimeZone.of("America/Lima")
        // 2026-11-01 03:00 UTC es el 31 de octubre a las 22:00 en Lima: todavía es octubre.
        val tarde = mov("g", 40.0, TipoMovimiento.Gasto).copiar(date = fecha(dia = 1, hora = 3, minuto = 0, mes = 11))
        assertEquals(40.0, calcularResumenDelMes(listOf(tarde), 0.0, ahora, lima).gastos, 0.001)
        assertEquals(0.0, calcularResumenDelMes(listOf(tarde), 0.0, ahora, utc).gastos, 0.001)
    }
}
