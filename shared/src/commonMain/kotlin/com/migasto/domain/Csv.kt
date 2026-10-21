package com.migasto.domain

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.datetime.TimeZone

// Port de lib/shared/csv_export.dart.

private fun entreComillas(valor: String) = "\"${valor.replace("\"", "\"\"")}\""

/** Entre comillas solo si hace falta (los nombres propios pueden traer comas). */
private fun entreComillasSiHaceFalta(valor: String) =
    if (valor.any { it == '"' || it == ',' || it == '\n' || it == '\r' }) entreComillas(valor) else valor

/** Como `toStringAsFixed(2)` de Dart. */
internal fun conDosDecimales(valor: Double): String {
    val centimos = (abs(valor) * 100).roundToLong()
    val signo = if (valor < 0 && centimos != 0L) "-" else ""
    return "$signo${centimos / 100}.${(centimos % 100).toString().padStart(2, '0')}"
}

/**
 * CSV de movimientos. No incluye la ubicación: solo viaja lo que el usuario ve en la lista.
 * Las fechas salen en la hora local de [zona].
 */
fun movimientosACsv(
    movimientos: Iterable<Movimiento>,
    categoriasPropias: List<CategoriaPropia> = emptyList(),
    zona: TimeZone = TimeZone.currentSystemDefault(),
): String = buildString {
    append("ID,Tipo,Monto,Contraparte,Categoria,Fuente,Canal,Estado,Fecha,Notas\n")
    for (m in movimientos) {
        listOf(
            m.id,
            m.tipo.clave,
            conDosDecimales(m.amount),
            entreComillas(m.merchant),
            entreComillasSiHaceFalta(etiquetaDeCategoria(m.category, m.categoriaPropia, categoriasPropias)),
            m.source.clave,
            m.canal.clave,
            m.estado.clave,
            fechaIsoLocal(m.date, zona),
            entreComillas(m.notes),
        ).joinTo(this, ",")
        append('\n')
    }
}
