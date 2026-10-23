package com.migasto.domain

import com.migasto.domain.reglas.ReglasIncluidas
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

object TextosDeLaCaptura {
    const val TITULO = "MiGasto"
    const val LEIDO_EN_EL_TELEFONO = "Leído en tu iPhone, sin internet"
    const val CATEGORIA = "Categoría"
    const val FUENTE = "Fuente"
    const val SIN_PAGO = "No encontramos un pago en la captura."
    const val SIN_PAGO_AYUDA = "Comparte la constancia de Yape o Plin, con el monto a la vista."
}

class OpcionDeCategoriaDeCaptura(val clave: String, val etiqueta: String, val elegida: Boolean)

/**
 * Lo que se lee de una captura compartida, para mostrarlo en la hoja de la extensión antes de guardar. La extensión no abre
 * la base: esto sale solo del texto y de las reglas de siempre (no cuenta lo que la app aprendió de los cambios del usuario).
 */
class VistaPreviaDeCaptura(
    val monto: Double,
    /** `S/ 32.00`. */
    val montoTexto: String,
    val esIngreso: Boolean,
    /** `a Café Central · Yape · 14:49` (o `de …` si es un ingreso). */
    val detalle: String,
    val fuente: PaymentSource,
    val fuenteEtiqueta: String,
    val categoriaSugerida: Categoria,
    val categoriaEtiqueta: String,
    val opcionesDeCategoria: List<OpcionDeCategoriaDeCaptura>,
) {
    /** La misma vista con otra categoría elegida; una clave que no es de este tipo no cambia nada. */
    fun conCategoria(clave: String): VistaPreviaDeCaptura {
        val nueva = Categoria.paraTipo(if (esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto).firstOrNull { it.clave == clave }
            ?: return this
        return VistaPreviaDeCaptura(
            monto, montoTexto, esIngreso, detalle, fuente, fuenteEtiqueta, nueva, nueva.etiqueta,
            opcionesDeCategoria.map { OpcionDeCategoriaDeCaptura(it.clave, it.etiqueta, it.clave == nueva.clave) },
        )
    }
}

private val lectorDeCapturas by lazy { LectorDePagos.desdeJson(ReglasIncluidas.lector) }
private val clasificadorDeCapturas by lazy { ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias) }

/** La vista previa de [texto], o `null` si no es la constancia de un pago. */
fun vistaPreviaDeCaptura(texto: String, ahora: Instant, zona: TimeZone): VistaPreviaDeCaptura? {
    if (!lectorDeCapturas.esConstanciaDePantalla(texto)) return null
    val pago = lectorDeCapturas.leer(texto) ?: return null
    val tipo = if (pago.esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto
    val categoria = if (pago.esIngreso) clasificadorDeCapturas.clasificarIngreso(texto, pago.peer) else clasificadorDeCapturas.clasificarGasto(texto, pago.peer)
    val preposicion = if (pago.esIngreso) "de" else "a"
    return VistaPreviaDeCaptura(
        monto = pago.amount,
        montoTexto = formatearSoles(pago.amount),
        esIngreso = pago.esIngreso,
        detalle = "$preposicion ${pago.peer} · ${pago.provider.etiqueta} · ${formatearHora(ahora, zona)}",
        fuente = pago.provider,
        fuenteEtiqueta = pago.provider.etiqueta,
        categoriaSugerida = categoria,
        categoriaEtiqueta = categoria.etiqueta,
        opcionesDeCategoria = Categoria.paraTipo(tipo).map { OpcionDeCategoriaDeCaptura(it.clave, it.etiqueta, it == categoria) },
    )
}

/** Swift no ve los valores por defecto de Kotlin: con la hora y la zona del teléfono. */
fun vistaPreviaDeCaptura(texto: String): VistaPreviaDeCaptura? =
    vistaPreviaDeCaptura(texto, Clock.System.now(), TimeZone.currentSystemDefault())
