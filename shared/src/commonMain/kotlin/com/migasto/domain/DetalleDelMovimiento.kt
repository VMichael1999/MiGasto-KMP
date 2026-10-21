package com.migasto.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Port de la boleta de lib/presentation/expenses/movement_detail_screen.dart y de alias_dialog.dart.

/** Todo lo que muestra el detalle de un movimiento, ya armado como texto. */
data class DetalleDelMovimiento(
    val id: String,
    /** `Gasto` o `Ingreso`. */
    val tipoTexto: String,
    /** El nombre que ve el usuario: el alias si lo hay. */
    val nombre: String,
    val nombreOriginal: String,
    val monto: Double,
    /** El monto como se edita: `25.00`, sin separador de miles. */
    val montoParaEditar: String,
    val esIngreso: Boolean,
    val porConfirmar: Boolean,
    /** `jue 1 oct, 11:00`. */
    val fecha: String,
    /** `De` en ingresos, `Para` en gastos. */
    val etiquetaDelNombre: String,
    /** `Recibido por` en ingresos, `Pagado con` en gastos. */
    val etiquetaDeLaFuente: String,
    val fuente: PaymentSource,
    /** `Efectivo` o `Tarjeta · Visa BBVA ···4821`. */
    val fuenteTexto: String,
    val categoria: Categoria,
    val categoriaEtiqueta: String,
    /** Clave del ícono si la categoría es propia del usuario y todavía existe. */
    val iconoPropio: String?,
    val categoriaPropiaId: String?,
    /** Cómo se registró: `Manual`, `Automático, desde la notificación`... */
    val registrado: String,
    /** La nota que escribió el usuario; `null` si no hay (o es el texto automático «Registro manual»). */
    val nota: String?,
    val tieneUbicacion: Boolean,
    val lugar: String?,
    val latitud: Double?,
    val longitud: Double?,
    /** La dirección, o las coordenadas si no se pudo resolver; `null` sin ubicación. */
    val direccion: String?,
    /** `Ubicación del teléfono · precisión ±15 m`; `null` sin ubicación. */
    val textoDeLaUbicacion: String?,
    /** La notificación o el texto leído de la captura; `null` si no hay. */
    val textoOriginal: String?,
    val textoDeConfirmar: String,
    val avisoDeGuardado: String,
    val textoDeEliminar: String,
    val preguntaDeEliminar: String,
    val avisoDeEliminado: String,
)

private const val REGISTRO_MANUAL = "Registro manual"

private val diasCortos = mapOf(
    DayOfWeek.MONDAY to "lun", DayOfWeek.TUESDAY to "mar", DayOfWeek.WEDNESDAY to "mié", DayOfWeek.THURSDAY to "jue",
    DayOfWeek.FRIDAY to "vie", DayOfWeek.SATURDAY to "sáb", DayOfWeek.SUNDAY to "dom",
)

private val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

private fun fechaDelDetalle(fecha: Instant, zona: TimeZone): String {
    val l = fecha.toLocalDateTime(zona)
    return "${diasCortos.getValue(l.dayOfWeek)} ${l.day} ${mesesCortos[l.month.ordinal]}, ${formatearHora(fecha, zona)}"
}

private fun cuandoSeRegistro(m: Movimiento): String = when (m.canal) {
    CanalMovimiento.Notificacion ->
        if (m.source == PaymentSource.GooglePay) "Automático, desde Google Wallet" else "Automático, desde la notificación"
    CanalMovimiento.Wallet -> "Automático, desde Wallet"
    CanalMovimiento.Captura -> "Desde una captura"
    CanalMovimiento.Manual -> "Manual"
}

fun construirDetalle(
    m: Movimiento,
    alias: Map<String, String>,
    propias: List<CategoriaPropia>,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): DetalleDelMovimiento {
    val nombre = nombreVisible(m.merchant, alias)
    val tipo = if (m.esIngreso) "ingreso" else "gasto"
    val propia = propias.firstOrNull { it.id == m.categoriaPropia }
    return DetalleDelMovimiento(
        id = m.id,
        tipoTexto = if (m.esIngreso) "Ingreso" else "Gasto",
        nombre = nombre,
        nombreOriginal = m.merchant,
        monto = m.amount,
        montoParaEditar = conDosDecimales(m.amount),
        esIngreso = m.esIngreso,
        porConfirmar = m.estado == EstadoMovimiento.Pendiente,
        fecha = fechaDelDetalle(m.date, zona),
        etiquetaDelNombre = if (m.esIngreso) "De" else "Para",
        etiquetaDeLaFuente = if (m.esIngreso) "Recibido por" else "Pagado con",
        fuente = m.source,
        fuenteTexto = listOfNotNull(m.source.etiqueta, m.tarjeta).joinToString(" · "),
        categoria = m.category,
        categoriaEtiqueta = etiquetaDeCategoria(m.category, m.categoriaPropia, propias),
        iconoPropio = propia?.icono,
        categoriaPropiaId = propia?.id,
        registrado = cuandoSeRegistro(m),
        nota = m.notes.takeIf { it.isNotEmpty() && it != REGISTRO_MANUAL },
        tieneUbicacion = m.tieneUbicacion,
        lugar = m.lugar,
        latitud = m.latitud.takeIf { m.tieneUbicacion },
        longitud = m.longitud.takeIf { m.tieneUbicacion },
        direccion = if (m.tieneUbicacion) direccionParaMostrar(m) else null,
        textoDeLaUbicacion = if (m.tieneUbicacion) textoDeLaPrecision(m) else null,
        textoOriginal = m.textoOriginal.takeIf { it.isNotEmpty() },
        textoDeConfirmar = if (m.esIngreso) "Guardar ingreso" else "Guardar gasto",
        avisoDeGuardado = if (m.esIngreso) "Ingreso guardado" else "Gasto guardado",
        textoDeEliminar = "Eliminar $tipo",
        preguntaDeEliminar = "¿Eliminar \"$nombre\" por ${formatearSoles(m.amount)}?",
        avisoDeEliminado = "${if (m.esIngreso) "Ingreso" else "Gasto"} de \"$nombre\" eliminado",
    )
}

/** Lo que se escribió como monto, con punto o coma decimal; `null` si no es un número. */
fun parsearMonto(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

/** La nota tal como se edita: el texto automático «Registro manual» se ve como vacío. */
fun notaParaEditar(m: Movimiento): String = if (m.notes == REGISTRO_MANUAL) "" else m.notes

/** Propuesta inicial para el alias: las dos primeras palabras, solo con la primera letra en mayúscula. */
fun sugerenciaDeAlias(nombre: String): String =
    nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
