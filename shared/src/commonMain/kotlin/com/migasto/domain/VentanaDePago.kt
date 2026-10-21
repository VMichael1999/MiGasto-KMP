package com.migasto.domain

// Port de la lógica de la ventana flotante «Pago detectado» (OverlayService.kt de la app de Flutter):
//
// - Gasto: cuenta regresiva de 4 s dentro del botón «Guardar» (se vacía de color fuerte a tenue); se pausa al
//   tocar la ventana y, al llegar a 0, se guarda.
// - Deslizar la ventana hacia arriba hace lo mismo que su primer botón (Descartar / Ignorar).
// - Ingreso: sin cuenta regresiva. Por defecto no se guarda solo: espera «Guardar ingreso»; si se ignora, queda
//   en «Por confirmar». Si en Ajustes está activado «Guardar ingresos automáticamente», sigue la misma cuenta
//   regresiva que un gasto.
//
// Esta clase decide qué pasa; la vista (Compose en Android) solo dibuja y avisa de los toques.

/** Qué hay que hacer con el pago cuando se cierra la ventana. */
enum class AccionDeVentana {
    /** Se guarda ya confirmado. */
    GuardarConfirmado,

    /** Se guarda sin confirmar: queda en «Por confirmar». */
    DejarPendiente,

    /** Igual que [DejarPendiente], y además se abre la app para revisarlo. */
    DejarPendienteYAbrirLaApp,

    /** No se guarda nada. */
    Descartar,
}

class TextosDeLaVentana(
    val esIngreso: Boolean,
    /** `S/ ` o `+ S/ `. */
    val prefijoDelMonto: String,
    val monto: String,
    val montoAccesible: String,
    /** `a ` en gastos, `de ` en ingresos. */
    val preposicion: String,
    val nombre: String,
    val fuente: String,
    val categoria: String,
    val descripcionDeLaCategoria: String,
    /** De izquierda a derecha; el último es el principal. */
    val botones: List<String>,
    val descripcionDelBotonPrincipal: String,
)

/** Un pago de 0 o menos no abre ventana. */
fun debeAbrirVentana(pago: PagoLeido): Boolean = pago.amount > 0

class SesionDeVentana(
    val pago: PagoLeido,
    val texto: String,
    val categoria: Categoria,
    /** Hay cuenta regresiva y se guarda solo: siempre en gastos; en ingresos solo si el usuario lo activó. */
    guardarSolo: Boolean,
) {
    val esIngreso: Boolean get() = pago.esIngreso
    val tieneCuentaRegresiva: Boolean = guardarSolo

    var pausada: Boolean = false
        private set

    private var restanteMs: Long = CUENTA_REGRESIVA_MS

    /** Cuánto queda de la cuenta, de 1 (llena) a 0. */
    val progreso: Float get() = restanteMs.toFloat() / CUENTA_REGRESIVA_MS

    /** Pausa la cuenta (al tocar la ventana). Sin cuenta regresiva no hace nada. */
    fun pausar() {
        if (tieneCuentaRegresiva) pausada = true
    }

    /** Pasa un paso de la cuenta. Devuelve la acción cuando llega a cero, y `null` mientras no. */
    fun avanzar(pasoMs: Long = PASO_MS): AccionDeVentana? {
        if (!tieneCuentaRegresiva || pausada) return null
        restanteMs -= pasoMs
        if (restanteMs <= 0) {
            restanteMs = 0
            return AccionDeVentana.GuardarConfirmado
        }
        return null
    }

    fun textoDeLaHora(hora: String): String = if (pausada) "En pausa" else hora

    fun alGuardar() = AccionDeVentana.GuardarConfirmado

    /** «Descartar» en un gasto; «Ignorar» en un ingreso (queda en «Por confirmar»). */
    fun alPrimerBoton() = if (esIngreso) AccionDeVentana.DejarPendiente else AccionDeVentana.Descartar

    fun alDeslizar() = alPrimerBoton()

    /** «Editar» o tocar la categoría: nada se guarda solo y se abre la app. */
    fun alEditar() = AccionDeVentana.DejarPendienteYAbrirLaApp

    /** Un ingreso sin respuesta queda pendiente después de [ESPERA_DEL_INGRESO_MS]. */
    fun alExpirarLaEspera() = AccionDeVentana.DejarPendiente

    /** Si llega otro pago mientras esta ventana sigue sin resolver, este queda pendiente en vez de perderse. */
    fun alSerReemplazada() = AccionDeVentana.DejarPendiente

    fun textos(nombreVisible: String, etiquetaDeLaCategoria: String): TextosDeLaVentana {
        val principal = if (esIngreso) "Guardar ingreso" else "Guardar"
        return TextosDeLaVentana(
            esIngreso = esIngreso,
            prefijoDelMonto = if (esIngreso) "+ S/ " else "S/ ",
            monto = formatearMonto(pago.amount),
            montoAccesible = "${formatearMonto(pago.amount)} soles",
            preposicion = if (esIngreso) "de " else "a ",
            nombre = nombreVisible,
            fuente = pago.provider.etiqueta,
            categoria = etiquetaDeLaCategoria,
            descripcionDeLaCategoria = "${if (esIngreso) "Tipo" else "Categoría"}: $etiquetaDeLaCategoria. Cambiar",
            botones = if (esIngreso) listOf("Ignorar", principal) else listOf("Descartar", "Editar", principal),
            descripcionDelBotonPrincipal = if (tieneCuentaRegresiva) "$principal. Se guarda solo en unos segundos" else principal,
        )
    }

    companion object {
        const val CUENTA_REGRESIVA_MS = 4_000L
        const val PASO_MS = 100L
        const val ESPERA_DEL_INGRESO_MS = 60_000L
    }
}
