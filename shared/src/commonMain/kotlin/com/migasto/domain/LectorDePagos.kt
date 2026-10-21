package com.migasto.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Port de lib/data/services/payment_reader.dart. Las reglas viven en reader_rules.json,
// el mismo archivo de la app de Flutter, para que ambas reconozcan lo mismo.

/** Un pago leído de una notificación o de una captura. */
data class PagoLeido(
    val amount: Double,
    /** Comercio (gastos) o persona/empresa (ingresos). */
    val peer: String,
    val provider: PaymentSource,
    val esIngreso: Boolean,
) {
    /** `ingreso` o `gasto`, como lo guarda la app de Flutter. */
    val tipoTexto: String get() = if (esIngreso) "ingreso" else "gasto"
}

@Serializable
internal class ProveedorDeReglas(
    val id: String,
    val keywords: List<String>,
    val wordBoundary: Boolean = false,
)

@Serializable
internal class ReglasDelLector(
    val providers: List<ProveedorDeReglas>,
    val amount: String,
    val screenReceipt: String,
    val operationStamp: String,
    val operationId: String,
    val income: List<String>,
    val expense: List<String>,
    val defaultTypeByProvider: Map<String, String> = emptyMap(),
    val peerPrepositions: List<String>,
    val cleanPhrases: List<String>,
    val peerStopWords: List<String>,
    val unknownPeer: String,
)

internal val jsonDeReglas = Json { ignoreUnknownKeys = true }

private const val LETRA_O_NUMERO = "[\\p{L}\\p{N}]"

class LectorDePagos private constructor(private val reglas: ReglasDelLector) {
    private val monto = Regex(reglas.amount, RegexOption.IGNORE_CASE)
    private val ingresos = reglas.income.map { Regex(it, RegexOption.IGNORE_CASE) }
    private val gastos = reglas.expense.map { Regex(it, RegexOption.IGNORE_CASE) }
    private val constancia = Regex(reglas.screenReceipt, RegexOption.IGNORE_CASE)
    private val sello = Regex(reglas.operationStamp, RegexOption.IGNORE_CASE)
    private val operacion = Regex(reglas.operationId, RegexOption.IGNORE_CASE)

    private val preposiciones = reglas.peerPrepositions.joinToString("|")
    private val prepAlInicio = Regex("^\\s*($preposiciones)\\b", RegexOption.IGNORE_CASE)
    private val prepAlFinal = Regex("\\b($preposiciones)\\s*$", RegexOption.IGNORE_CASE)
    private val prepYResto = Regex("^($preposiciones)\\b([\\s\\S]*)", RegexOption.IGNORE_CASE)

    /** Las frases más largas primero para no dejar restos. */
    private val frasesDeLimpieza = reglas.cleanPhrases.sortedByDescending { it.length }.map {
        Regex("(^|[^\\p{L}])${Regex.escape(it)}(?![\\p{L}])", RegexOption.IGNORE_CASE)
    }

    private val palabrasConLimite: Map<String, Regex> = reglas.providers
        .filter { it.wordBoundary }
        .flatMap { it.keywords }
        .associateWith { Regex("(?<!$LETRA_O_NUMERO)${Regex.escape(it)}(?!$LETRA_O_NUMERO)") }

    /** `null` si el texto no es un pago reconocible (no se inventa un gasto). */
    fun leer(texto: String): PagoLeido? {
        val proveedor = proveedor(texto) ?: return null
        val coincidencia = monto.find(texto) ?: return null
        val cantidad = leerMonto(coincidencia.groupValues[1]) ?: return null
        if (cantidad <= 0) return null

        val esIngreso = direccion(texto, proveedor) ?: return null
        val nombre = nombre(texto, coincidencia, esIngreso)
        return PagoLeido(
            amount = cantidad,
            peer = nombre.ifEmpty { reglas.unknownPeer },
            provider = PaymentSource.desdeClave(proveedor)
                ?: error("reader_rules.json trae un proveedor desconocido: $proveedor"),
            esIngreso = esIngreso,
        )
    }

    /** Número de operación de una constancia de Yape, o `null` si no lo trae. */
    fun numeroDeOperacion(texto: String): String? = operacion.find(texto)?.groupValues?.get(1)

    /** Fecha y hora impresas en la constancia; identifican el pago si falta el número. */
    fun selloDeConstancia(texto: String): String? = sello.find(texto)?.value

    /** `true` si la pantalla es la constancia de un pago, no el inicio con el saldo. */
    fun esConstanciaDePantalla(texto: String): Boolean = constancia.containsMatchIn(texto)

    private fun proveedor(texto: String): String? {
        val minusculas = texto.lowercase()
        for (p in reglas.providers) {
            for (k in p.keywords) {
                val encontrado = if (p.wordBoundary) {
                    palabrasConLimite.getValue(k).containsMatchIn(minusculas)
                } else {
                    k in minusculas
                }
                if (encontrado) return p.id
            }
        }
        return null
    }

    /** `true` ingreso, `false` gasto, `null` si no se puede saber. */
    private fun direccion(texto: String, proveedor: String): Boolean? {
        // Primero los ingresos: "te yapeó" contiene la raíz de "yapeaste".
        if (ingresos.any { it.containsMatchIn(texto) }) return true
        if (gastos.any { it.containsMatchIn(texto) }) return false
        return when (reglas.defaultTypeByProvider[proveedor]) {
            "gasto" -> false
            "ingreso" -> true
            else -> null
        }
    }

    private fun nombre(texto: String, monto: MatchResult, esIngreso: Boolean): String {
        val despues = texto.substring(monto.range.last + 1).trim()
        val antes = texto.substring(0, monto.range.first).trim()

        fun despuesDelMonto(): String =
            prepYResto.find(despues)?.let { limpiar(it.groupValues[2]) } ?: ""

        // En ingresos el nombre suele ir antes ("Juan te yapeó S/ 15"); en gastos,
        // después ("Yapeaste S/ 25 a Tambo"). Se prueba el otro lado si queda vacío.
        val primero = if (esIngreso) limpiar(antes) else despuesDelMonto()
        if (primero.isNotEmpty()) return primero
        val segundo = if (esIngreso) despuesDelMonto() else limpiar(antes)
        if (segundo.isNotEmpty() || esIngreso) return segundo
        return despuesSinPreposicion(despues)
    }

    /**
     * Constancia de Yape al enviar: el nombre va justo después del monto, sin "a" ni "de"
     * ("¡Yapeaste! S/ 1 Juan Pérez 01 oct. 2026 09:48 a. m. DATOS DE LA ..."). Se toma
     * hasta la primera cifra (la fecha) y se descarta si queda demasiado largo.
     */
    private fun despuesSinPreposicion(despues: String): String {
        val cifra = Regex("\\d").find(despues)
        val nombre = limpiar(if (cifra == null) despues else despues.substring(0, cifra.range.first))
        return if (nombre.length in 2..60) nombre else ""
    }

    private fun limpiar(entrada: String): String {
        var t = entrada
        for (corte in reglas.peerStopWords) {
            val i = t.lowercase().indexOf(corte)
            if (i > 0) t = t.substring(0, i)
        }
        for (frase in frasesDeLimpieza) t = t.replace(frase, " ")
        t = t.replace(prepAlInicio, "")
        t = t.replace(prepAlFinal, "")
        t = t.replace(espacios, " ")
        return t.replace(bordes, "")
    }

    companion object {
        private val espacios = Regex("\\s+")
        private val bordes = Regex("^[\\s:,\\-¡!.*_]+|[\\s:,\\-¡!.*_]+$")
        private val milesConComa = Regex("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$")

        fun desdeJson(json: String): LectorDePagos =
            LectorDePagos(jsonDeReglas.decodeFromString<ReglasDelLector>(json))

        /** `1,200.50` -> 1200.5; `12,50` -> 12.5; `25.5` -> 25.5. */
        internal fun leerMonto(crudo: String): Double? {
            val s = crudo.trim()
            return (if (milesConComa.matches(s)) s.replace(",", "") else s.replace(',', '.')).toDoubleOrNull()
        }
    }
}
