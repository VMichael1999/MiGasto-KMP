package com.migasto.android.resumen

import com.migasto.domain.Categoria
import com.migasto.domain.CategoriaPropia
import com.migasto.domain.EstadoMovimiento
import com.migasto.domain.Movimiento
import com.migasto.domain.PaymentSource
import com.migasto.domain.etiqueta
import com.migasto.domain.formatearHora
import com.migasto.domain.formatearSolesConSigno
import com.migasto.domain.nombreVisible
import kotlinx.datetime.TimeZone

/** Una fila de movimiento (en Resumen y en Movimientos). El texto ya viene armado desde `shared`. */
data class FilaDeMovimiento(
    val id: String,
    val nombre: String,
    val fuente: PaymentSource,
    val fuenteYHora: String,
    val monto: String,
    val esIngreso: Boolean,
    val porConfirmar: Boolean,
    val categoria: Categoria,
    /** Clave del ícono si la categoría es propia del usuario. */
    val iconoPropio: String?,
)

fun crearFila(m: Movimiento, alias: Map<String, String>, propias: List<CategoriaPropia>, zona: TimeZone) = FilaDeMovimiento(
    id = m.id,
    nombre = nombreVisible(m.merchant, alias),
    fuente = m.source,
    fuenteYHora = "${m.source.etiqueta} · ${formatearHora(m.date, zona)}",
    monto = formatearSolesConSigno(m.amount, m.esIngreso),
    esIngreso = m.esIngreso,
    porConfirmar = m.estado == EstadoMovimiento.Pendiente,
    categoria = m.category,
    iconoPropio = propias.firstOrNull { it.id == m.categoriaPropia }?.icono,
)
