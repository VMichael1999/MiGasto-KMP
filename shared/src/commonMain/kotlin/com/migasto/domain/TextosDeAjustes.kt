package com.migasto.domain

import kotlin.math.roundToLong
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Port de los textos y validaciones de lib/presentation/settings/settings_screen.dart que no dependen
// de la pantalla.

/** El error a mostrar al elegir la contraseña del respaldo, o `null` si está bien. */
fun validarContrasenaDeRespaldo(contrasena: String, repetida: String, creando: Boolean): String? = when {
    creando && contrasena.length < ServicioDeRespaldo.LARGO_MINIMO_DE_CONTRASENA ->
        "Usa al menos ${ServicioDeRespaldo.LARGO_MINIMO_DE_CONTRASENA} caracteres."
    creando && contrasena != repetida -> "Las contraseñas no coinciden."
    !creando && contrasena.isEmpty() -> "Escribe la contraseña."
    else -> null
}

private val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

/** `21 oct 2026, 10:14`. */
private fun fechaYHora(fecha: Instant, zona: TimeZone): String {
    val l = fecha.toLocalDateTime(zona)
    return "${l.day} ${mesesCortos[l.month.ordinal]} ${l.year}, ${formatearHora(fecha, zona)}"
}

fun subtituloDelRespaldo(ultimo: Instant?, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    if (ultimo == null) {
        "Aún no has hecho uno. Si pierdes el teléfono, pierdes tus datos."
    } else {
        "Último: ${fechaYHora(ultimo, zona)}"
    }

/** `MiGasto-respaldo-2026-10-21_1014.mgb`. */
fun nombreDelArchivoDeRespaldo(ahora: Instant, zona: TimeZone = TimeZone.currentSystemDefault()): String {
    val l = ahora.toLocalDateTime(zona)
    fun dos(n: Int) = n.toString().padStart(2, '0')
    return "MiGasto-respaldo-${l.year}-${dos(l.month.ordinal + 1)}-${dos(l.day)}_${dos(l.hour)}${dos(l.minute)}.${ServicioDeRespaldo.EXTENSION}"
}

/** Lo que se le cuenta al usuario antes de restaurar. */
fun textoDeRestauracion(contenido: ContenidoDeRespaldo, nuevos: Int, zona: TimeZone = TimeZone.currentSystemDefault()): String =
    "Creado el ${fechaYHora(contenido.creado, zona)}.\n\n" +
        "• ${contenido.movimientos.size} movimientos en el respaldo, $nuevos nuevos para este teléfono.\n" +
        "• Presupuesto: ${formatearSoles(contenido.presupuesto)}.\n\n" +
        "No se borra nada de lo que ya tienes."

fun mensajeDeRestauracion(agregados: Int): String =
    if (agregados == 0) "No había nada nuevo: ya tenías todos esos movimientos." else "Se restauraron $agregados movimientos."

/** El presupuesto escrito; solo vale si es mayor que cero (para quitarlo hay un botón aparte). */
fun parsearPresupuesto(texto: String): Double? = parsearMonto(texto)?.takeIf { it > 0 }

/** El presupuesto como se edita: entero, o vacío si no hay. */
fun presupuestoParaEditar(presupuesto: Double): String = if (presupuesto > 0) presupuesto.roundToLong().toString() else ""

fun textoDelPresupuestoEnAjustes(presupuesto: Double): String = if (presupuesto > 0) formatearSoles(presupuesto) else "Sin definir"

data class CategoriaAprendida(val comercio: String, val categoria: String)

fun nombreDeCategoriaAprendida(clave: String): String = Categoria.desdeClave(clave)?.etiqueta ?: clave

fun aprendidasOrdenadas(aprendidas: Map<String, String>): List<CategoriaAprendida> =
    aprendidas.entries.sortedBy { it.key }.map { CategoriaAprendida(it.key, nombreDeCategoriaAprendida(it.value)) }

data class AliasGuardado(val clave: String, val nombre: String)

fun aliasOrdenados(alias: Map<String, String>): List<AliasGuardado> =
    alias.entries.sortedBy { it.key }.map { AliasGuardado(it.key, it.value) }
