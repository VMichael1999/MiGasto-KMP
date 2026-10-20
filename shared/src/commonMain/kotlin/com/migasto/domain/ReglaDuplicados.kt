package com.migasto.domain

import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Port de lib/domain/duplicate_rule.dart.

/**
 * Ventana en la que un pago con el mismo monto y fuente se considera el mismo
 * (p. ej. llega por la notificación y por la pantalla de confirmación).
 */
val ventanaDeDuplicados = 2.minutes

/**
 * `true` si ya existe un movimiento con el mismo monto y fuente dentro de la
 * ventana de [ventanaDeDuplicados] alrededor de [fecha].
 */
fun esMovimientoDuplicado(
    existentes: Iterable<Movimiento>,
    monto: Double,
    fuente: PaymentSource,
    fecha: Instant,
    tipo: TipoMovimiento? = null,
): Boolean = existentes.any { m ->
    m.source == fuente &&
        abs(m.amount - monto) <= 0.004 &&
        (tipo == null || m.tipo == tipo) &&
        (m.date - fecha).absoluteValue <= ventanaDeDuplicados
}
