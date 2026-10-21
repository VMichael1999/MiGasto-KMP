package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// En Android este armado vivía en ResumenViewModel. Aquí sale de la vista para que Android y iPhone muestren
// exactamente lo mismo: cada pantalla solo recibe los textos ya hechos y los dibuja.

/** Una fila de movimiento (en Resumen y en Movimientos). El texto ya viene armado. */
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

class PresupuestoDelResumen(
    val estado: EstadoDelPresupuesto,
    val titulo: String,
    val detalle: String,
    /** Cuánto de la barra se llena, de 0 a 1. */
    val relleno: Float,
)

/** Un mes del menú de la barra superior. */
data class OpcionDeMes(val mes: MesElegido, val nombre: String)

class PantallaDelResumen(
    val mes: String,
    val saldo: String,
    val ingresos: String,
    val gastos: String,
    /** `null` si no hay nada por confirmar. */
    val porConfirmarTexto: String?,
    val porConfirmarMonto: String,
    /** `null` si el usuario no definió presupuesto. */
    val presupuesto: PresupuestoDelResumen?,
    /** Lo de hoy; en un mes anterior, sus últimos cinco movimientos. */
    val hoy: List<FilaDeMovimiento>,
    val textoDelSaldo: String,
    val tituloDeLaLista: String,
    val textoSinMovimientos: String,
    /** Los meses del menú, del más nuevo al más viejo. */
    val meses: List<OpcionDeMes>,
    val mesElegido: MesElegido,
)

private const val MOVIMIENTOS_DE_UN_MES_PASADO = 5

fun armarPantallaDelResumen(
    movimientos: List<Movimiento>,
    presupuesto: Double,
    alias: Map<String, String>,
    propias: List<CategoriaPropia>,
    ahora: Instant,
    zona: TimeZone = TimeZone.currentSystemDefault(),
    mes: MesElegido? = null,
): PantallaDelResumen {
    val resumen = calcularResumenDelMes(movimientos, presupuesto, ahora, zona, mes)
    val hoy = ahora.toLocalDateTime(zona).date
    val delMesActual = MesElegido(hoy.year, hoy.month.ordinal + 1)
    val vistos = mes ?: delMesActual
    val esElActual = vistos == delMesActual
    val deLaLista = movimientos
        .filter { m ->
            val f = m.date.toLocalDateTime(zona).date
            if (esElActual) f == hoy else f.year == vistos.anio && f.month.ordinal + 1 == vistos.mes
        }
        .sortedByDescending { it.date }
        .take(if (esElActual) Int.MAX_VALUE else MOVIMIENTOS_DE_UN_MES_PASADO)
        .map { crearFila(it, alias, propias, zona) }
    val pendientes = movimientos.filter { it.estado == EstadoMovimiento.Pendiente }
    return PantallaDelResumen(
        mes = nombreDelMesElegido(vistos, ahora, zona),
        saldo = formatearMonto(resumen.saldo),
        ingresos = formatearSoles(resumen.ingresos),
        gastos = formatearSoles(resumen.gastos),
        porConfirmarTexto = pendientes.takeIf { it.isNotEmpty() }?.let { textoPorConfirmar(it) },
        porConfirmarMonto = "+ ${formatearSoles(resumen.porConfirmar)}",
        presupuesto = if (resumen.tienePresupuesto) {
            PresupuestoDelResumen(
                estado = resumen.estadoDelPresupuesto,
                titulo = textoDelEstadoDelPresupuesto(resumen.estadoDelPresupuesto),
                detalle = textoDelPresupuestoUsado(resumen),
                relleno = (resumen.gastos / resumen.presupuesto).toFloat().coerceIn(0f, 1f),
            )
        } else {
            null
        },
        hoy = deLaLista,
        textoDelSaldo = textoDelSaldoDelResumen(vistos, ahora, zona),
        tituloDeLaLista = tituloDeLaListaDelResumen(vistos, ahora, zona),
        textoSinMovimientos = textoSinMovimientosEnElResumen(vistos, ahora, zona),
        meses = mesesParaElegir(movimientos, ahora, zona).map { OpcionDeMes(it, nombreDelMesElegido(it, ahora, zona)) },
        mesElegido = vistos,
    )
}

/** La pantalla del Resumen, que se vuelve a emitir cada vez que cambian los movimientos (con el reloj y la zona del teléfono). */
fun observarPantallaDelResumen(base: BaseMiGasto, mes: MesElegido?): Flow<PantallaDelResumen> =
    observarPantallaDelResumen(base, mes, { Clock.System.now() }, TimeZone.currentSystemDefault())

fun observarPantallaDelResumen(base: BaseMiGasto, mes: MesElegido?, reloj: () -> Instant, zona: TimeZone): Flow<PantallaDelResumen> =
    base.movimientos.observarTodos().map { movimientos ->
        armarPantallaDelResumen(
            movimientos, base.ajustes.presupuesto(), base.ajustes.aliases(), base.categoriasPropias.todas(), reloj(), zona, mes,
        )
    }
