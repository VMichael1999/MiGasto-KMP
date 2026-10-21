package com.migasto.domain

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// Port del contenido de lib/data/services/backup_service.dart. Las claves del JSON son las de
// la app de Flutter: un respaldo hecho en una app se abre en la otra.

/** Lo que viaja dentro de un respaldo. */
data class ContenidoDeRespaldo(
    val movimientos: List<Movimiento>,
    val presupuesto: Double,
    /** Comercio en minúsculas -> nombre de la categoría que el usuario eligió. */
    val aprendidas: Map<String, String>,
    val creado: Instant,
    /** Nombres que el usuario le puso a personas y comercios (clave normalizada -> nombre). */
    val alias: Map<String, String> = emptyMap(),
    /** Categorías que creó el usuario, con su ícono. */
    val categoriasPropias: List<CategoriaPropia> = emptyList(),
)

/** El respaldo no se pudo crear o leer. [message] se puede mostrar al usuario. */
class ExcepcionDeRespaldo(override val message: String) : Exception(message)

internal const val VERSION_DEL_FORMATO = 1

/** JSON del respaldo. Las fechas de los movimientos van en la hora local de [zona], como en Flutter. */
fun ContenidoDeRespaldo.aJson(zona: TimeZone = TimeZone.currentSystemDefault()): String {
    val objeto = buildJsonObject {
        put("formato", VERSION_DEL_FORMATO)
        put("app", "MiGasto")
        put("creado", fechaIsoUtc(creado))
        put("presupuesto", presupuesto)
        put("aprendidas", JsonObject(aprendidas.mapValues { JsonPrimitive(it.value) }))
        put("alias", JsonObject(alias.mapValues { JsonPrimitive(it.value) }))
        put("categoriasPropias", JsonArray(categoriasPropias.map { it.aJson() }))
        put("movimientos", JsonArray(movimientos.map { movimientoAJson(it, zona) }))
    }
    return objeto.toString()
}

private fun movimientoAJson(m: Movimiento, zona: TimeZone): JsonObject = buildJsonObject {
    put("id", m.id)
    put("monto", m.amount)
    put("contraparte", m.merchant)
    put("categoria", m.category.clave)
    put("categoriaPropia", m.categoriaPropia)
    put("fuente", m.source.clave)
    put("fecha", fechaIsoLocal(m.date, zona))
    put("notas", m.notes)
    put("tipo", m.tipo.clave)
    put("canal", m.canal.clave)
    put("estado", m.estado.clave)
    put("tarjeta", m.tarjeta)
    put("latitud", m.latitud)
    put("longitud", m.longitud)
    put("precision", m.precision)
    put("lugar", m.lugar)
    put("textoOriginal", m.textoOriginal)
}

private const val NO_ES_RESPALDO = "Este archivo no es un respaldo de MiGasto."
private const val DANADO = "El archivo de respaldo está dañado."

/** Lee el JSON de un respaldo. Lanza [ExcepcionDeRespaldo] si no es de MiGasto o está dañado. */
fun contenidoDeRespaldoDesdeJson(
    texto: String,
    zona: TimeZone = TimeZone.currentSystemDefault(),
): ContenidoDeRespaldo {
    try {
        val mapa = Json.parseToJsonElement(texto).jsonObject
        val version = (mapa["formato"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        if (mapa["app"].texto() != "MiGasto" || version == null) throw ExcepcionDeRespaldo(NO_ES_RESPALDO)
        if (version > VERSION_DEL_FORMATO) {
            throw ExcepcionDeRespaldo("Este respaldo es de una versión más nueva de MiGasto. Actualiza la app.")
        }
        return ContenidoDeRespaldo(
            creado = leerFechaIso(mapa.getValue("creado").texto()!!, zona),
            presupuesto = mapa.getValue("presupuesto").numero()!!,
            aprendidas = mapa.getValue("aprendidas").jsonObject.mapValues { it.value.texto()!! },
            // Los respaldos hechos antes de los alias no traen este campo.
            alias = (mapa["alias"] as? JsonObject)?.mapValues { it.value.texto()!! } ?: emptyMap(),
            // Los respaldos hechos antes de las categorías propias no traen este campo.
            categoriasPropias = categoriasPropiasDesdeJson(mapa["categoriasPropias"]),
            movimientos = mapa.getValue("movimientos").jsonArray.map { movimientoDesdeJson(it.jsonObject, zona) },
        )
    } catch (e: ExcepcionDeRespaldo) {
        throw e
    } catch (e: Exception) {
        throw ExcepcionDeRespaldo(DANADO)
    }
}

private fun JsonElement?.texto(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonElement?.numero(): Double? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

private fun JsonElement?.textoOpcional(): String? = if (this == null || this is JsonNull) null else texto()

private fun movimientoDesdeJson(j: JsonObject, zona: TimeZone): Movimiento = Movimiento(
    id = j.getValue("id").texto()!!,
    amount = j.getValue("monto").numero()!!,
    merchant = j.getValue("contraparte").texto()!!,
    category = Categoria.desdeClave(j["categoria"].texto() ?: "") ?: Categoria.Otros,
    categoriaPropia = j["categoriaPropia"].textoOpcional(),
    source = PaymentSource.desdeClave(j["fuente"].texto() ?: "") ?: PaymentSource.Otro,
    date = leerFechaIso(j.getValue("fecha").texto()!!, zona),
    notes = j["notas"].texto() ?: "",
    tipo = TipoMovimiento.entries.firstOrNull { it.clave == j["tipo"].texto() } ?: TipoMovimiento.Gasto,
    canal = CanalMovimiento.entries.firstOrNull { it.clave == j["canal"].texto() } ?: CanalMovimiento.Manual,
    estado = EstadoMovimiento.entries.firstOrNull { it.clave == j["estado"].texto() } ?: EstadoMovimiento.Pendiente,
    tarjeta = j["tarjeta"].textoOpcional(),
    latitud = j["latitud"].numero(),
    longitud = j["longitud"].numero(),
    precision = j["precision"].numero(),
    lugar = j["lugar"].textoOpcional(),
    textoOriginal = j["textoOriginal"].texto() ?: "",
)
