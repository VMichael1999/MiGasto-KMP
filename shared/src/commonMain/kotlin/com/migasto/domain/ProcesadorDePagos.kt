package com.migasto.domain

import com.migasto.db.RepositorioAjustes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Port de la parte de decisión de MyAccessibilityService.handleText, DuplicateGuard, HandledStore y
// PaymentNotificationListener (android/app/src/main de la app de Flutter). El código nativo de Android solo
// entrega el texto; qué es un pago nuevo se decide aquí, igual para Android e iPhone.

/** Por dónde llegó un texto: el servicio de notificaciones, la accesibilidad o la pantalla. */
enum class OrigenDeLectura { Notificaciones, Accesibilidad, Pantalla }

/**
 * Decide si un pago leído es el mismo que otro que ya llegó por OTRA vía (por ejemplo, la notificación y la
 * pantalla de la constancia) dentro de [ventana].
 *
 * Dos notificaciones del servicio de notificaciones nunca se frenan entre sí: cada una ya se descarta sola si
 * se repite, así que dos yapes del mismo monto seguidos valen los dos.
 *
 * No es seguro entre hilos: hay que llamarlo siempre desde el mismo.
 */
class GuardaDeDuplicados(
    private val ventana: Duration = 2.minutes,
    private val reloj: () -> Instant,
) {
    private val recientes = LinkedHashMap<String, Pair<Instant, OrigenDeLectura>>()

    fun esDuplicado(clave: String, origen: OrigenDeLectura): Boolean {
        val ahora = reloj()
        recientes.entries.removeAll { ahora - it.value.first > ventana }
        val previo = recientes[clave]
        val ambosPorNotificacion = previo != null && previo.second == OrigenDeLectura.Notificaciones && origen == OrigenDeLectura.Notificaciones
        if (previo != null && !ambosPorNotificacion) return true
        recientes[clave] = ahora to origen
        return false
    }
}

/** Identificadores de lo que ya se leyó, para no registrarlo dos veces. */
interface LeidosStore {
    /** Guarda [id]; devuelve `false` si ya estaba (es decir, ya se había leído). */
    fun marcarComoLeido(id: String): Boolean
}

/** Los últimos 200 identificadores leídos, guardados en los ajustes: sobreviven a reinicios. */
class LeidosEnAjustes(private val ajustes: RepositorioAjustes) : LeidosStore {
    override fun marcarComoLeido(id: String): Boolean {
        val ids = ajustes.idsLeidos()
        if (id in ids) return false
        ajustes.guardarIdsLeidos((ids + id).takeLast(MAXIMO))
        return true
    }

    private companion object {
        const val MAXIMO = 200
    }
}

sealed interface ResultadoDeLectura {
    /** El texto no es un pago (o es la pantalla de inicio de Yape, no una constancia). */
    data object SinPago : ResultadoDeLectura

    /** El proveedor está apagado en los ajustes. */
    data object ProveedorApagado : ResultadoDeLectura

    /** Esta constancia (por su número de operación o su fecha y hora) ya se leyó. */
    data object YaLeido : ResultadoDeLectura

    /** Es el mismo pago que llegó hace menos de 2 minutos por otra vía. */
    data object Duplicado : ResultadoDeLectura

    /** Un pago nuevo. [texto] va recortado a 400 caracteres: el original puede traer datos personales. */
    data class PagoNuevo(val pago: PagoLeido, val texto: String) : ResultadoDeLectura
}

class ProcesadorDePagos(
    private val lector: LectorDePagos,
    private val ajustes: RepositorioAjustes,
    private val leidos: LeidosStore,
    private val guarda: GuardaDeDuplicados,
) {
    /**
     * Notificaciones: al reiniciar el servicio o reinstalar la app, el sistema vuelve a entregar las que siguen
     * en la barra; esas no son pagos nuevos. Devuelve `true` si esta hay que leerla.
     */
    fun esNotificacionNueva(id: String, antiguedad: Duration): Boolean {
        if (antiguedad > ANTIGUEDAD_MAXIMA) return false
        return leidos.marcarComoLeido(id)
    }

    /** Lee un texto (notificación o pantalla) y decide qué hacer con él. */
    fun procesar(texto: String, origen: OrigenDeLectura): ResultadoDeLectura {
        // De una pantalla solo cuenta la constancia: el inicio de Yape (saldo, movimientos) no es un pago,
        // aunque cambie cada vez que se toca «ver saldo».
        if (origen == OrigenDeLectura.Pantalla && !lector.esConstanciaDePantalla(texto)) return ResultadoDeLectura.SinPago
        val pago = lector.leer(texto) ?: return ResultadoDeLectura.SinPago
        if (!ajustes.proveedorActivo(pago.provider.clave)) return ResultadoDeLectura.ProveedorApagado

        // La constancia de Yape trae un número de operación: si ya se leyó, es la misma pantalla vista otra vez.
        lector.numeroDeOperacion(texto)?.let { if (!leidos.marcarComoLeido("op|$it")) return ResultadoDeLectura.YaLeido }
        // Si el número no está a la vista, la fecha y hora de la constancia identifican igual el mismo pago.
        val monto = conDosDecimales(pago.amount)
        lector.selloDeConstancia(texto)?.let { sello ->
            if (!leidos.marcarComoLeido("st|${pago.provider.clave}|${pago.tipoTexto}|$monto|$sello")) return ResultadoDeLectura.YaLeido
        }
        if (guarda.esDuplicado("${pago.provider.clave}|${pago.tipoTexto}|$monto", origen)) return ResultadoDeLectura.Duplicado

        return ResultadoDeLectura.PagoNuevo(pago, texto.take(LARGO_MAXIMO_DEL_TEXTO))
    }

    companion object {
        val ANTIGUEDAD_MAXIMA = 3.minutes
        const val LARGO_MAXIMO_DEL_TEXTO = 400
    }
}

/** Un pago leído que ya quedó guardado; [guardado] dice si está confirmado o espera al usuario. */
data class PagoGuardado(val movimiento: Movimiento, val guardado: Boolean)

/** El título y el texto de la notificación de un pago detectado. */
data class TextosDelAviso(val titulo: String, val texto: String, val pideConfirmacion: Boolean)

fun textosDelAviso(monto: Double, nombre: String, fuente: PaymentSource, esIngreso: Boolean, guardado: Boolean): TextosDelAviso {
    val dinero = formatearSoles(monto)
    val titulo = when {
        esIngreso && guardado -> "Ingreso guardado: $dinero"
        esIngreso -> "Ingreso por confirmar: $dinero"
        else -> "Gasto guardado: $dinero"
    }
    return TextosDelAviso(titulo, "${if (esIngreso) "de" else "a"} $nombre · ${fuente.etiqueta}", pideConfirmacion = esIngreso && !guardado)
}

/** Lo que se ve en la pantalla bloqueada: solo que hubo un pago, sin monto ni nombre. */
val TEXTOS_DEL_AVISO_PUBLICO = TextosDelAviso("MiGasto", "Pago detectado", pideConfirmacion = false)
