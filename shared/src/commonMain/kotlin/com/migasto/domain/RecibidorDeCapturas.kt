package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Qué pasó con el texto de una captura compartida. */
enum class DesenlaceDeCaptura { Guardado, PorConfirmar, SinPago, ProveedorApagado, YaRegistrado }

/** Lo que se le cuenta al usuario después de compartir una captura: [titulo] y [detalle] ya vienen armados. */
class ResultadoDeLaCaptura(val desenlace: DesenlaceDeCaptura, val titulo: String, val detalle: String)

/**
 * Registra el texto que el sistema leyó de una captura (Vision en iPhone). Usa las mismas reglas que la lectura de
 * pantalla en Android: solo cuenta la constancia, lo que ya se leyó no se repite y los proveedores apagados se ignoran.
 */
class RecibidorDeCapturas(
    base: BaseMiGasto,
    private val registro: RegistroDeMovimientos,
    reloj: () -> Instant,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(base: BaseMiGasto, registro: RegistroDeMovimientos) : this(base, registro, { Clock.System.now() })

    private val lector = LectorDePagos.desdeJson(ReglasIncluidas.lector)
    private val procesador = ProcesadorDePagos(
        lector = lector,
        ajustes = base.ajustes,
        leidos = LeidosEnAjustes(base.ajustes),
        guarda = GuardaDeDuplicados(2.minutes, reloj),
    )

    /** Para Swift, que no ve los valores por defecto de Kotlin. */
    fun recibir(texto: String): ResultadoDeLaCaptura = recibir(texto, null)

    /** [categoriaElegida] es la clave de la categoría que el usuario eligió en la hoja de la extensión, o `null`. */
    fun recibir(texto: String, categoriaElegida: String?): ResultadoDeLaCaptura = when (val lectura = procesador.procesar(texto, OrigenDeLectura.Pantalla)) {
        ResultadoDeLectura.SinPago -> ResultadoDeLaCaptura(
            DesenlaceDeCaptura.SinPago, TextosDeLaCaptura.SIN_PAGO, TextosDeLaCaptura.SIN_PAGO_AYUDA,
        )
        ResultadoDeLectura.ProveedorApagado -> ResultadoDeLaCaptura(
            DesenlaceDeCaptura.ProveedorApagado, "${proveedorDe(texto)} está apagado en Ajustes.",
            "Enciéndelo en Ajustes, Registro automático, y vuelve a compartir la captura.",
        )
        ResultadoDeLectura.YaLeido, ResultadoDeLectura.Duplicado -> yaRegistrado()
        is ResultadoDeLectura.PagoNuevo -> {
            val guardado = registro.guardarPagoDeCaptura(lectura.pago, lectura.texto, categoriaElegida?.let { Categoria.desdeClave(it) })
            if (guardado == null) {
                yaRegistrado()
            } else {
                val pago = lectura.pago
                val textos = textosDelAviso(pago.amount, pago.peer, pago.provider, pago.esIngreso, guardado.guardado)
                ResultadoDeLaCaptura(
                    if (textos.pideConfirmacion) DesenlaceDeCaptura.PorConfirmar else DesenlaceDeCaptura.Guardado,
                    textos.titulo, textos.texto,
                )
            }
        }
    }

    private fun yaRegistrado() = ResultadoDeLaCaptura(
        DesenlaceDeCaptura.YaRegistrado, "Ese pago ya estaba registrado.", "No se guardó otra vez.",
    )

    private fun proveedorDe(texto: String): String =
        lector.leer(texto)?.provider?.etiqueta ?: "Ese proveedor"
}
