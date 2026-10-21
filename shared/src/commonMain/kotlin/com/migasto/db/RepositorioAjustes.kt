package com.migasto.db

import com.migasto.domain.Categoria
import com.migasto.domain.ClaveDePausa
import com.migasto.domain.claveDeAlias
import com.migasto.domain.fechaIsoUtc
import com.migasto.domain.leerFechaIso
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

/**
 * Los ajustes del usuario, en la misma base cifrada. Reemplaza el uso de SharedPreferences de
 * `ExpenseRepositoryImpl` y de los providers de la app de Flutter, con las mismas claves.
 */
class RepositorioAjustes internal constructor(private val db: MiGastoDb) {
    private val consultas get() = db.ajustesQueries

    // Presupuesto

    /** 0 = sin definir: no se inventa un presupuesto. */
    fun presupuesto(): Double = leer(CLAVE_PRESUPUESTO)?.toDoubleOrNull() ?: 0.0

    fun guardarPresupuesto(monto: Double) = guardar(CLAVE_PRESUPUESTO, monto.toString())

    // Categorías aprendidas (comercio en minúsculas -> clave de la categoría)

    fun aprendidas(): Map<String, String> = leerMapa(CLAVE_APRENDIDAS)

    fun categoriaAprendida(comercio: String): Categoria? =
        aprendidas()[claveDeComercio(comercio)]?.let { Categoria.desdeClave(it) ?: Categoria.Otros }

    fun guardarAprendida(comercio: String, categoria: Categoria) =
        guardarMapa(CLAVE_APRENDIDAS, aprendidas() + (claveDeComercio(comercio) to categoria.clave))

    fun olvidarAprendida(comercio: String) =
        guardarMapa(CLAVE_APRENDIDAS, aprendidas() - claveDeComercio(comercio))

    fun olvidarTodasLasAprendidas() = consultas.borrar(CLAVE_APRENDIDAS)

    // Alias (clave normalizada -> nombre)

    fun aliases(): Map<String, String> = leerMapa(CLAVE_ALIAS)

    /** Un alias vacío, o igual al nombre original, quita el nombre guardado. */
    fun establecerAlias(comercio: String, alias: String) {
        val limpio = alias.trim()
        val clave = claveDeAlias(comercio)
        guardarMapa(CLAVE_ALIAS, if (limpio.isEmpty() || limpio == comercio.trim()) aliases() - clave else aliases() + (clave to limpio))
    }

    /** Agrega los de un respaldo; si ya tenías uno para el mismo nombre, se queda el tuyo. */
    fun mezclarAliases(otros: Map<String, String>) = guardarMapa(CLAVE_ALIAS, otros + aliases())

    // Proveedores de pago

    fun proveedoresActivos(): Map<String, Boolean> {
        val guardado = leer(CLAVE_PROVEEDORES)?.let { texto ->
            runCatching {
                Json.parseToJsonElement(texto).jsonObject.mapValues { (it.value as JsonPrimitive).booleanOrNull ?: error("no es booleano") }
            }.getOrNull()
        }
        return guardado ?: PROVEEDORES_POR_DEFECTO
    }

    fun guardarProveedoresActivos(activos: Map<String, Boolean>) =
        guardar(CLAVE_PROVEEDORES, JsonObject(activos.mapValues { JsonPrimitive(it.value) }).toString())

    fun proveedorActivo(proveedor: String): Boolean = proveedoresActivos()[proveedor] == true

    // Preferencias

    /** Apagado por defecto: un ingreso detectado espera la confirmación del usuario. */
    fun guardarIngresosAutomaticamente(): Boolean = leer(CLAVE_INGRESOS_AUTOMATICOS) == "true"

    fun establecerGuardarIngresosAutomaticamente(valor: Boolean) = guardar(CLAVE_INGRESOS_AUTOMATICOS, valor.toString())

    // Lectura de pagos

    /** Los últimos identificadores de notificaciones y constancias ya leídas (los más viejos primero). */
    fun idsLeidos(): List<String> = leer(CLAVE_LEIDOS)?.let { texto ->
        runCatching { Json.parseToJsonElement(texto).jsonArray.map { (it as JsonPrimitive).content } }.getOrNull()
    } ?: emptyList()

    fun guardarIdsLeidos(ids: List<String>) = guardar(CLAVE_LEIDOS, JsonArray(ids.map { JsonPrimitive(it) }).toString())

    // Avisos de configuración pospuestos («Ahora no»)

    fun avisoPospuestoEn(clave: ClaveDePausa): Instant? =
        leer(claveDeAjuste(clave))?.let { runCatching { leerFechaIso(it) }.getOrNull() }

    fun posponerAviso(clave: ClaveDePausa, cuando: Instant) = guardar(claveDeAjuste(clave), fechaIsoUtc(cuando))

    private fun claveDeAjuste(clave: ClaveDePausa) = when (clave) {
        ClaveDePausa.Bateria -> CLAVE_BATERIA_POSPUESTA
        ClaveDePausa.Avisos -> CLAVE_AVISOS_POSPUESTOS
    }

    // Respaldo

    /** Cuándo se hizo el último respaldo; `null` si nunca. Se guarda como en Flutter: UTC en ISO 8601. */
    fun ultimoRespaldo(): Instant? = leer(CLAVE_ULTIMO_RESPALDO)?.let { runCatching { leerFechaIso(it) }.getOrNull() }

    fun guardarUltimoRespaldo(cuando: Instant) = guardar(CLAVE_ULTIMO_RESPALDO, fechaIsoUtc(cuando))

    /** Si el monto y el nombre se ven en la notificación con la pantalla bloqueada. Apagado por defecto. */
    fun mostrarMontoEnPantallaBloqueada(): Boolean = leer(CLAVE_MONTO_BLOQUEADO) == "true"

    fun establecerMostrarMontoEnPantallaBloqueada(valor: Boolean) = guardar(CLAVE_MONTO_BLOQUEADO, valor.toString())

    /** El bloqueo con huella, rostro o PIN del teléfono. Apagado por defecto. */
    fun bloqueoActivado(): Boolean = leer(CLAVE_BLOQUEO) == "true"

    fun establecerBloqueoActivado(valor: Boolean) = guardar(CLAVE_BLOQUEO, valor.toString())

    /** `true` cuando el usuario ya pasó por la bienvenida (la marca `onboarding_complete` de Flutter). */
    fun bienvenidaCompleta(): Boolean = leer(CLAVE_BIENVENIDA) == "true"

    fun establecerBienvenidaCompleta() = guardar(CLAVE_BIENVENIDA, "true")

    // Interno

    internal fun leerCrudo(clave: String): String? = leer(clave)

    internal fun guardarCrudo(clave: String, valor: String) = guardar(clave, valor)

    private fun leer(clave: String): String? = consultas.valor(clave).executeAsOneOrNull()

    private fun guardar(clave: String, valor: String) = consultas.guardar(clave, valor)

    private fun leerMapa(clave: String): Map<String, String> = leer(clave)?.let { texto ->
        runCatching {
            Json.parseToJsonElement(texto).jsonObject.mapValues { (it.value as JsonPrimitive).also { p -> check(p.isString) }.content }
        }.getOrNull()
    } ?: emptyMap()

    private fun guardarMapa(clave: String, mapa: Map<String, String>) =
        guardar(clave, JsonObject(mapa.mapValues { JsonPrimitive(it.value) }).toString())

    private fun claveDeComercio(comercio: String) = comercio.lowercase().trim()

    companion object {
        // Las mismas claves que usa la app de Flutter.
        const val CLAVE_PRESUPUESTO = "migasto_monthly_budget_v2"
        const val CLAVE_PROVEEDORES = "migasto_providers_enabled_v2"
        const val CLAVE_ALIAS = "migasto_aliases_v1"
        const val CLAVE_APRENDIDAS = "migasto_category_overrides_v2"
        const val CLAVE_INGRESOS_AUTOMATICOS = "migasto_auto_save_income"
        const val CLAVE_ULTIMO_RESPALDO = "migasto_last_backup"
        const val CLAVE_LEIDOS = "migasto_handled_notifications"
        const val CLAVE_MONTO_BLOQUEADO = "migasto_show_amount_locked"
        const val CLAVE_BLOQUEO = "migasto_passcode_enabled"
        const val CLAVE_BIENVENIDA = "onboarding_complete"
        const val CLAVE_BATERIA_POSPUESTA = "migasto_battery_banner_dismissed_at"
        const val CLAVE_AVISOS_POSPUESTOS = "migasto_alerts_banner_dismissed_at"

        private val PROVEEDORES_POR_DEFECTO = mapOf("yape" to true, "plin" to true, "googlePay" to true)
    }
}
