package com.migasto.android

import com.migasto.db.BaseMiGasto
import com.migasto.domain.CanalMovimiento
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.TipoMovimiento
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Datos de ejemplo del diseño (`docs/diseno/migasto-kmp.html`), SOLO en la versión de depuración y solo si
 * la base está vacía. Sirven para comparar la pantalla con el diseño: los montos tienen que cuadrar
 * (ingresos S/ 2,800.00, gastos S/ 144.70, quedan S/ 2,655.30, por confirmar S/ 230.00, presupuesto S/ 1,500).
 * La versión de producción (`src/release`) no trae nada.
 */
object SemillaDeEjemplo {
    fun aplicar(base: BaseMiGasto) {
        if (base.movimientos.cantidad() > 0L) return
        val zona = TimeZone.currentSystemDefault()
        val hoy = Clock.System.now().toLocalDateTime(zona).date
        fun hoyA(h: Int, m: Int) = LocalDateTime(hoy.year, hoy.month, hoy.day, h, m).toInstant(zona)
        // Los otros movimientos del mes caen antes de hoy, para que «Hoy» muestre solo cuatro filas.
        val antes = (hoy.day - 1).coerceAtLeast(1)
        fun enElMes(dia: Int, h: Int) = LocalDateTime(hoy.year, hoy.month, minOf(dia, antes), h, 0).toInstant(zona)

        base.ajustes.guardarPresupuesto(1500.0)
        val barberia = base.categoriasPropias.agregar("Barbería", "scissors", TipoMovimiento.Gasto)

        fun mov(
            id: String, monto: Double, nombre: String, categoria: Categoria, fuente: PaymentSource,
            fecha: kotlin.time.Instant, tipo: TipoMovimiento = TipoMovimiento.Gasto,
            estado: EstadoMovimiento = EstadoMovimiento.Confirmado, propia: String? = null,
            canal: CanalMovimiento = CanalMovimiento.Manual,
        ) = base.movimientos.guardar(
            Movimiento(id, monto, nombre, categoria, fuente, fecha, tipo = tipo, estado = estado, categoriaPropia = propia, canal = canal),
        )

        // Hoy (lo que muestra el diseño).
        mov("ej-carlos", 80.0, "Carlos", Categoria.TransferenciaRecibida, PaymentSource.Plin, hoyA(11, 40),
            TipoMovimiento.Ingreso, EstadoMovimiento.Pendiente, canal = CanalMovimiento.Notificacion)
        mov("ej-ana", 150.0, "Ana", Categoria.TransferenciaRecibida, PaymentSource.Yape, hoyA(11, 25),
            TipoMovimiento.Ingreso, EstadoMovimiento.Pendiente, canal = CanalMovimiento.Notificacion)
        mov("ej-barberia", 25.0, "Barbería El Patrón", Categoria.Otros, PaymentSource.Efectivo, hoyA(11, 0), propia = barberia.id)
        mov("ej-minimarket", 89.2, "Minimarket La Esquina", Categoria.Compras, PaymentSource.GooglePay, hoyA(10, 20),
            canal = CanalMovimiento.Wallet)
        // El resto del mes.
        mov("ej-sueldo", 2800.0, "Sueldo", Categoria.Sueldo, PaymentSource.Manual, enElMes(1, 8), TipoMovimiento.Ingreso)
        mov("ej-cafe", 18.5, "Café Central", Categoria.Alimentacion, PaymentSource.Yape, enElMes(1, 13),
            canal = CanalMovimiento.Notificacion)
        mov("ej-taxi", 12.0, "Taxi", Categoria.Transporte, PaymentSource.Efectivo, enElMes(1, 18))
        // Los tres meses anteriores, para que Reportes tenga con qué comparar (el diseño muestra Jul a Oct).
        listOf(Triple(3, 621.0, "jul"), Triple(2, 906.0, "ago"), Triple(1, 925.4, "sep")).forEach { (atras, gastos, nombre) ->
            val mes = hoy.minus(atras, DateTimeUnit.MONTH)
            fun enMes(dia: Int, h: Int) = LocalDateTime(mes.year, mes.month, dia, h, 0).toInstant(zona)
            mov("ej-sueldo-$nombre", 2800.0, "Sueldo", Categoria.Sueldo, PaymentSource.Manual, enMes(1, 8), TipoMovimiento.Ingreso)
            mov("ej-gastos-$nombre", gastos, "Gastos del mes", Categoria.Compras, PaymentSource.Efectivo, enMes(15, 12))
        }
    }
}
