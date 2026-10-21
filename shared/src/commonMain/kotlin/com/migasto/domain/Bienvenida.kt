package com.migasto.domain

// Port de la lógica de lib/presentation/onboarding/onboarding_screen.dart. La vista solo dibuja y abre los
// ajustes del sistema; qué paso toca y qué dicen los textos sale de aquí.

/** Qué permisos del sistema están dados ahora. */
data class PermisosDeBienvenida(val lectura: Boolean, val ventana: Boolean, val avisos: Boolean, val bateria: Boolean)

/** Lo que hace el botón principal ahora. */
enum class PasoDeBienvenida { Lectura, Ventana, Avisos, Bateria, Terminar }

data class PasoUi(val numero: Int, val titulo: String, val pista: String, val listo: Boolean) {
    /** Lo que lee el lector de pantalla. */
    val descripcion: String get() = "Paso $numero: $titulo. ${if (listo) "Listo" else pista}"
}

object TextosDeBienvenida {
    const val TITULO = "Registra tus pagos sin escribirlos."
    const val SUBTITULO = "MiGasto anota los pagos de Yape, Plin y Google Wallet por ti y te deja confirmar cada uno."
    const val PRIVACIDAD_TITULO = "Qué lee la app y qué no"
    const val LEE = "Lee: "
    const val LEE_DETALLE =
        "las notificaciones y la pantalla de Yape, Plin y Google Wallet, solo para detectar el monto, el comercio y si el dinero entra o sale."
    const val NO_LEE = "No lee: "
    const val NO_LEE_DETALLE = "tus mensajes, fotos, contactos ni otras apps."
    const val GUARDA = "Dónde se guarda: "
    const val GUARDA_DETALLE = "solo en tu teléfono. No se envía a ningún servidor."
}

/** Los pasos van en orden: el siguiente pendiente es el primero que falta. */
fun siguientePasoDeBienvenida(p: PermisosDeBienvenida): PasoDeBienvenida = when {
    !p.lectura -> PasoDeBienvenida.Lectura
    !p.ventana -> PasoDeBienvenida.Ventana
    !p.avisos -> PasoDeBienvenida.Avisos
    !p.bateria -> PasoDeBienvenida.Bateria
    else -> PasoDeBienvenida.Terminar
}

fun textoDelBotonDeBienvenida(p: PermisosDeBienvenida): String = when (siguientePasoDeBienvenida(p)) {
    PasoDeBienvenida.Lectura -> "Aceptar y activar la lectura"
    PasoDeBienvenida.Ventana -> "Permitir la ventana flotante"
    PasoDeBienvenida.Avisos -> "Permitir los avisos"
    PasoDeBienvenida.Bateria -> "Quitar la restricción de batería"
    PasoDeBienvenida.Terminar -> "Empezar"
}

/** Salir sin dar permisos es posible: la app sirve con registro manual. */
fun textoDelBotonSecundarioDeBienvenida(p: PermisosDeBienvenida): String =
    if (p.lectura) "Terminar después" else "Usar solo registro manual"

fun pasosDeBienvenida(p: PermisosDeBienvenida): List<PasoUi> = listOf(
    PasoUi(1, "Activa la lectura de pagos", "En Acceso a notificaciones, activa MiGasto", p.lectura),
    PasoUi(2, "Permite la ventana flotante", "Para confirmar cada pago al instante. Es opcional.", p.ventana),
    PasoUi(3, "Recibe un aviso de cada pago", "Una notificación cuando detecta un pago con la pantalla bloqueada.", p.avisos),
    PasoUi(4, "Evita que el teléfono la duerma", "En Batería, elige «No restringido». Así no se pierden pagos.", p.bateria),
)
