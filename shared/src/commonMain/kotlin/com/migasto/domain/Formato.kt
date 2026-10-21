package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Instant

// Textos y montos tal como se ven (español de Perú, `S/ 2,655.30`), para que Android e iPhone muestren
// lo mismo y ninguna vista tenga que calcular ni formatear montos.

/** `2,655.30`: miles con coma y decimales con punto, como `NumberFormat('#,##0.00', 'en')`. */
fun formatearMonto(monto: Double, decimales: Int = 2): String {
    val escala = 10.0.pow(decimales)
    val unidades = (abs(monto) * escala).roundToLong()
    val signo = if (monto < 0 && unidades != 0L) "-" else ""
    val enteros = (unidades / escala.toLong()).toString().reversed().chunked(3).joinToString(",").reversed()
    if (decimales == 0) return "$signo$enteros"
    return "$signo$enteros.${(unidades % escala.toLong()).toString().padStart(decimales, '0')}"
}

/** `S/ 2,655.30`. */
fun formatearSoles(monto: Double, decimales: Int = 2): String = "S/ ${formatearMonto(monto, decimales)}"

/** Los ingresos llevan `+ ` delante: `+ S/ 80.00`. */
fun formatearSolesConSigno(monto: Double, esIngreso: Boolean): String =
    if (esIngreso) "+ ${formatearSoles(monto)}" else formatearSoles(monto)

/** Hora de 24 horas en la zona indicada: `11:40`. */
fun formatearHora(fecha: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String {
    val l = fecha.toLocalDateTime(zona)
    return "${l.hour.toString().padStart(2, '0')}:${l.minute.toString().padStart(2, '0')}"
}

private val meses = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

/** `Octubre` para el mes 10. */
fun nombreDelMes(mes: Int): String = meses[mes - 1]

fun textoDelEstadoDelPresupuesto(estado: EstadoDelPresupuesto): String = when (estado) {
    EstadoDelPresupuesto.Bien -> "Gastos: vas bien"
    EstadoDelPresupuesto.Cerca -> "Gastos: cerca del límite"
    EstadoDelPresupuesto.Pasado -> "Gastos: te pasaste"
}

/** `10 % de S/ 1,500`. */
fun textoDelPresupuestoUsado(resumen: ResumenDelMes): String =
    "${resumen.porcentajeDelPresupuesto} % de ${formatearSoles(resumen.presupuesto, decimales = 0)}"

/** Nombre visible de cada fuente de pago (port de `sourceLabel` de Flutter). */
val PaymentSource.etiqueta: String
    get() = when (this) {
        PaymentSource.Yape -> "Yape"
        PaymentSource.Plin -> "Plin"
        PaymentSource.GooglePay -> "Google Wallet"
        PaymentSource.Tarjeta -> "Tarjeta"
        PaymentSource.Efectivo -> "Efectivo"
        PaymentSource.Manual, PaymentSource.Otro -> "Manual"
    }

/** `2 ingresos` si todo lo pendiente son ingresos; si no, `N movimientos`. */
fun textoPorConfirmar(pendientes: List<Movimiento>): String {
    val n = pendientes.size
    return if (pendientes.all { it.esIngreso }) {
        if (n == 1) "1 ingreso" else "$n ingresos"
    } else {
        if (n == 1) "1 movimiento" else "$n movimientos"
    }
}
