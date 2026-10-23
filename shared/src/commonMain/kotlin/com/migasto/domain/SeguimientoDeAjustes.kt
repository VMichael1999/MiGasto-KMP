package com.migasto.domain

import com.migasto.db.BaseMiGasto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Instant

object TextosDeLaPantallaDeAjustes {
    const val TITULO = "Ajustes"
    const val SECCION_REGISTRO = "Registro automático"
    const val SECCION_UBICACION = "Dónde pagaste"
    const val SECCION_DATOS = "Tus datos"
    const val PIE = "Tus datos se guardan solo en este teléfono."
}

/** Los textos de Ajustes que son de iPhone: la sección «Registro en iPhone» (capturas, Atajos y avisos) y la de seguridad. */
object TextosDeAjustesDeIphone {
    const val SECCION_REGISTRO = "Registro en iPhone"
    const val CAPTURAS = "Compartir capturas de Yape y Plin"
    const val ATAJOS = "Apple Pay con Atajos"
    const val AVISOS = "Avisos de pagos"
    const val ACTIVO = "Activo"
    const val ACTIVOS = "Activos"
    const val ACTIVAR = "Activar"
    const val CONFIGURAR = "Configurar"
    const val PIE_DEL_REGISTRO = "En iPhone las apps no pueden leer las notificaciones de otras apps; por eso se registra con capturas y con Atajos."
    const val SECCION_SEGURIDAD = "Seguridad"
    const val FACE_ID = "Bloquear con Face ID"
    const val OCULTAR_MONTOS = "Ocultar montos al abrir"
    const val ABRIR_ATAJOS = "Abrir Atajos"
    const val ABRIR_AJUSTES = "Abrir Ajustes"
    const val ENTENDIDO = "Entendido"
    const val AVISOS_BLOQUEADOS = "Los avisos están apagados para MiGasto. Actívalos en Ajustes ▸ Notificaciones."

    val PASOS_DE_LAS_CAPTURAS = listOf(
        "Abre la captura de Yape o Plin en Fotos.",
        "Toca Compartir y elige MiGasto.",
        "El pago se lee en tu iPhone, sin internet, y se registra al abrir la app.",
    )

    val PASOS_DE_ATAJOS = listOf(
        "Abre Atajos ▸ Automatización ▸ Nueva automatización.",
        "Elige «Transacción» (Wallet) y las tarjetas que quieras.",
        "Agrega la acción «Registrar movimiento» de MiGasto.",
        "Pásale el monto, el comercio y la tarjeta de la transacción.",
    )
}

class ProveedorDeAjuste(val clave: String, val nombre: String, val activo: Boolean)

/** Lo que muestra Ajustes, ya con sus textos. */
class PantallaDeAjustes(
    val presupuesto: Double,
    /** `S/ 1,500.00` o `Sin definir`. */
    val textoDelPresupuesto: String,
    /** El presupuesto como se edita: entero, o vacío si no hay. */
    val presupuestoParaEditar: String,
    val ingresosAutomaticos: Boolean,
    val subtituloDeLosIngresosAutomaticos: String,
    val montoEnPantallaBloqueada: Boolean,
    val bloqueoActivado: Boolean,
    val ocultarMontosAlAbrir: Boolean,
    val subtituloDelMontoEnPantallaBloqueada: String,
    val proveedores: List<ProveedorDeAjuste>,
    val subtituloDelRespaldo: String,
    val aprendidas: List<CategoriaAprendida>,
    val aliases: List<AliasGuardado>,
    val propias: List<CategoriaPropia>,
)

/** El respaldo cifrado, en base64 (Swift no maneja bien los `ByteArray`), o por qué no se pudo hacer. */
class ResultadoDelRespaldo(val datosEnBase64: String?, val error: String?)

/** Un respaldo abierto, con lo que se le cuenta al usuario antes de restaurar, o por qué no se pudo abrir. */
class AperturaDeRespaldo(val contenido: ContenidoDeRespaldo?, val nuevos: Int, val textoParaConfirmar: String?, val error: String?)

/**
 * Lee los ajustes y hace lo que la pantalla permite: presupuesto, interruptores, listas, CSV y respaldo. En Android
 * esto vivía en AjustesViewModel; aquí lo comparten Android e iPhone.
 */
class SeguimientoDeAjustes(
    private val base: BaseMiGasto,
    private val registro: RegistroDeMovimientos,
    private val reloj: () -> Instant,
    private val zona: TimeZone,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(base: BaseMiGasto, registro: RegistroDeMovimientos) :
        this(base, registro, { Clock.System.now() }, TimeZone.currentSystemDefault())

    private val refresco = MutableStateFlow(0)

    val estado: Flow<PantallaDeAjustes> = refresco.map {
        val a = base.ajustes
        val activos = a.proveedoresActivos()
        PantallaDeAjustes(
            presupuesto = a.presupuesto(),
            textoDelPresupuesto = textoDelPresupuestoEnAjustes(a.presupuesto()),
            presupuestoParaEditar = presupuestoParaEditar(a.presupuesto()),
            ingresosAutomaticos = a.guardarIngresosAutomaticamente(),
            subtituloDeLosIngresosAutomaticos = if (a.guardarIngresosAutomaticamente()) {
                "Los ingresos se guardan solos, como los gastos."
            } else {
                "Apagado: cada ingreso espera tu confirmación."
            },
            montoEnPantallaBloqueada = a.mostrarMontoEnPantallaBloqueada(),
            bloqueoActivado = a.bloqueoActivado(),
            ocultarMontosAlAbrir = a.ocultarMontosAlAbrir(),
            subtituloDelMontoEnPantallaBloqueada = if (a.mostrarMontoEnPantallaBloqueada()) {
                "Visible: el aviso muestra el monto y el nombre"
            } else {
                "Oculto: solo dice «Pago detectado»"
            },
            proveedores = listOf("yape" to "Yape", "plin" to "Plin", "googlePay" to "Google Wallet")
                .map { (clave, nombre) -> ProveedorDeAjuste(clave, nombre, activos[clave] == true) },
            subtituloDelRespaldo = subtituloDelRespaldo(a.ultimoRespaldo(), zona),
            aprendidas = aprendidasOrdenadas(a.aprendidas()),
            aliases = aliasOrdenados(a.aliases()),
            propias = base.categoriasPropias.todas(),
        )
    }

    /** Vuelve a leer todo (las categorías propias o los movimientos pueden haber cambiado en otra pantalla). */
    fun refrescar() = refresco.update { it + 1 }

    private inline fun cambiar(bloque: () -> Unit) {
        bloque()
        refrescar()
    }

    /** Guarda el presupuesto (`0` lo quita) y devuelve el aviso para el usuario. */
    fun guardarPresupuesto(monto: Double): String {
        cambiar { base.ajustes.guardarPresupuesto(monto) }
        return if (monto > 0) "Presupuesto actualizado" else "Presupuesto quitado"
    }

    fun alternarIngresosAutomaticos() =
        cambiar { base.ajustes.establecerGuardarIngresosAutomaticamente(!base.ajustes.guardarIngresosAutomaticamente()) }

    fun alternarMontoEnPantallaBloqueada() =
        cambiar { base.ajustes.establecerMostrarMontoEnPantallaBloqueada(!base.ajustes.mostrarMontoEnPantallaBloqueada()) }

    /** Activa o apaga el bloqueo de la app (la autenticación del teléfono la pide cada sistema antes de activarlo). */
    fun establecerBloqueo(activado: Boolean) = cambiar { base.ajustes.establecerBloqueoActivado(activado) }

    fun alternarOcultarMontosAlAbrir() =
        cambiar { base.ajustes.establecerOcultarMontosAlAbrir(!base.ajustes.ocultarMontosAlAbrir()) }

    fun alternarProveedor(clave: String) = cambiar {
        val activos = base.ajustes.proveedoresActivos()
        base.ajustes.guardarProveedoresActivos(activos + (clave to !(activos[clave] ?: false)))
    }

    fun olvidarAprendida(comercio: String) = cambiar { base.ajustes.olvidarAprendida(comercio) }

    fun olvidarTodasLasAprendidas() = cambiar { base.ajustes.olvidarTodasLasAprendidas() }

    fun quitarAlias(clave: String) = cambiar { base.ajustes.establecerAlias(clave, "") }

    fun quitarPropia(id: String) = cambiar { base.categoriasPropias.quitar(id) }

    fun borrarUbicaciones() = cambiar { registro.quitarTodasLasUbicaciones() }

    /** Todos los movimientos como CSV, sin ubicaciones. */
    fun csv(): String = movimientosACsv(base.movimientos.todos(), base.categoriasPropias.todas())

    private fun respaldoDeLaApp() = RespaldoDeLaApp(base.movimientos, base.ajustes, base.categoriasPropias)

    /** Cifra el respaldo con [contrasena]. Tarda unos segundos: deriva la clave con 210 000 vueltas. */
    suspend fun crearRespaldo(contrasena: String): ResultadoDelRespaldo = withContext(Dispatchers.Default) {
        try {
            val datos = ServicioDeRespaldo().cifrar(respaldoDeLaApp().contenido(), contrasena)
            ResultadoDelRespaldo(Base64.Default.encode(datos), null)
        } catch (e: ExcepcionDeRespaldo) {
            ResultadoDelRespaldo(null, e.message)
        } catch (e: Exception) {
            ResultadoDelRespaldo(null, "No se pudo crear el respaldo.")
        }
    }

    /** Anota que el respaldo ya se guardó (para «Último: …»). */
    fun respaldoGuardado() = cambiar { base.ajustes.guardarUltimoRespaldo(reloj()) }

    /** `MiGasto-respaldo-2026-10-21_1014.mgb`. */
    fun nombreDelArchivoDeRespaldo(): String = nombreDelArchivoDeRespaldo(reloj(), zona)

    /** Abre un respaldo (en base64) con su contraseña y cuenta cuántos movimientos serían nuevos. */
    suspend fun abrirRespaldo(datosEnBase64: String, contrasena: String): AperturaDeRespaldo = withContext(Dispatchers.Default) {
        try {
            val contenido = ServicioDeRespaldo().descifrar(Base64.Default.decode(datosEnBase64), contrasena)
            val nuevos = respaldoDeLaApp().cuantosSonNuevos(contenido)
            AperturaDeRespaldo(contenido, nuevos, textoDeRestauracion(contenido, nuevos, zona), null)
        } catch (e: ExcepcionDeRespaldo) {
            AperturaDeRespaldo(null, 0, null, e.message)
        } catch (e: Exception) {
            AperturaDeRespaldo(null, 0, null, "No se pudo abrir el respaldo.")
        }
    }

    /** Agrega lo que falta y devuelve el aviso para el usuario («Se restauraron 12 movimientos.»). */
    fun restaurar(contenido: ContenidoDeRespaldo): String {
        val agregados = respaldoDeLaApp().restaurar(contenido)
        refrescar()
        return mensajeDeRestauracion(agregados)
    }
}
