package com.migasto.domain

import kotlin.time.Instant

// Port de lib/domain/entities/movimiento.dart. La `clave` de cada enum es el texto que
// la app de Flutter guarda (`enum.name`): así la base y el respaldo `.mgb` son compatibles.

/** Gasto (sale dinero) o ingreso (entra dinero). */
enum class TipoMovimiento(val clave: String) {
    Gasto("gasto"),
    Ingreso("ingreso"),
}

/** Los ingresos quedan pendientes hasta que el usuario los confirma. */
enum class EstadoMovimiento(val clave: String) {
    Pendiente("pendiente"),
    Confirmado("confirmado"),
}

/** Cómo entró el movimiento a la app. */
enum class CanalMovimiento(val clave: String) {
    Notificacion("notificacion"),
    Wallet("wallet"),
    Captura("captura"),
    Manual("manual"),
}

enum class Categoria(val clave: String) {
    // Gastos
    Alimentacion("alimentacion"),
    Transporte("transporte"),
    Compras("compras"),
    Servicios("servicios"), // luz, agua, internet
    Entretenimiento("entretenimiento"), // Netflix, cine
    Otros("otros"),

    // Ingresos
    Sueldo("sueldo"),
    TransferenciaRecibida("transferenciaRecibida"),
    Venta("venta"),
    OtrosIngresos("otrosIngresos"),
    ;

    val esDeIngreso: Boolean get() = this in ingresos

    companion object {
        val gastos: List<Categoria> =
            listOf(Alimentacion, Transporte, Compras, Servicios, Entretenimiento, Otros)

        val ingresos: List<Categoria> =
            listOf(Sueldo, TransferenciaRecibida, Venta, OtrosIngresos)

        /** Categorías que se pueden elegir para un tipo de movimiento. */
        fun paraTipo(tipo: TipoMovimiento): List<Categoria> =
            if (tipo == TipoMovimiento.Ingreso) ingresos else gastos

        fun desdeClave(clave: String): Categoria? = entries.firstOrNull { it.clave == clave }
    }
}

enum class PaymentSource(val clave: String) {
    Yape("yape"),
    Plin("plin"),
    GooglePay("googlePay"), // Tarjeta con Google Wallet (Android)
    Manual("manual"),
    Tarjeta("tarjeta"), // Tarjeta con Apple Pay u otra
    Efectivo("efectivo"),
    Otro("otro"),
    ;

    companion object {
        fun desdeClave(clave: String): PaymentSource? = entries.firstOrNull { it.clave == clave }
    }
}

/**
 * Un gasto o un ingreso. Dos movimientos son iguales si tienen el mismo [id], como en la
 * app de Flutter. Los montos son `Double`, igual que allá; el paso a céntimos enteros se
 * decide aparte para no romper la compatibilidad del respaldo.
 */
class Movimiento(
    val id: String,
    val amount: Double,
    /** Comercio en gastos; persona o empresa en ingresos. */
    val merchant: String,
    val category: Categoria,
    val source: PaymentSource,
    val date: Instant,
    val notes: String = "",
    val tipo: TipoMovimiento = TipoMovimiento.Gasto,
    val canal: CanalMovimiento = CanalMovimiento.Manual,
    val estado: EstadoMovimiento = EstadoMovimiento.Pendiente,
    /** Nombre de la tarjeta que informa Wallet (opcional). */
    val tarjeta: String? = null,
    /** Dónde se hizo el pago (opcionales). */
    val latitud: Double? = null,
    val longitud: Double? = null,
    /** En metros. */
    val precision: Double? = null,
    val lugar: String? = null,
    /** La notificación o el texto leído de la captura. */
    val textoOriginal: String = "",
    /**
     * Id de la categoría propia del usuario, si eligió una. [category] sigue siendo la de
     * siempre (Otros / Otros ingresos) para los totales.
     */
    val categoriaPropia: String? = null,
) {
    val isConfirmed: Boolean get() = estado == EstadoMovimiento.Confirmado
    val esIngreso: Boolean get() = tipo == TipoMovimiento.Ingreso
    val esGasto: Boolean get() = tipo == TipoMovimiento.Gasto
    val tieneUbicacion: Boolean get() = latitud != null && longitud != null

    /**
     * Copia con los cambios indicados. Igual que `copyWith` de Dart: un argumento `null`
     * conserva el valor actual (no sirve para vaciar un campo), salvo [quitarUbicacion].
     */
    fun copiar(
        id: String? = null,
        amount: Double? = null,
        merchant: String? = null,
        category: Categoria? = null,
        source: PaymentSource? = null,
        date: Instant? = null,
        notes: String? = null,
        tipo: TipoMovimiento? = null,
        canal: CanalMovimiento? = null,
        estado: EstadoMovimiento? = null,
        tarjeta: String? = null,
        latitud: Double? = null,
        longitud: Double? = null,
        precision: Double? = null,
        lugar: String? = null,
        textoOriginal: String? = null,
        quitarUbicacion: Boolean = false,
        categoriaPropia: String? = null,
    ): Movimiento = Movimiento(
        id = id ?: this.id,
        amount = amount ?: this.amount,
        merchant = merchant ?: this.merchant,
        category = category ?: this.category,
        source = source ?: this.source,
        date = date ?: this.date,
        notes = notes ?: this.notes,
        tipo = tipo ?: this.tipo,
        canal = canal ?: this.canal,
        estado = estado ?: this.estado,
        tarjeta = tarjeta ?: this.tarjeta,
        latitud = if (quitarUbicacion) null else latitud ?: this.latitud,
        longitud = if (quitarUbicacion) null else longitud ?: this.longitud,
        precision = if (quitarUbicacion) null else precision ?: this.precision,
        lugar = if (quitarUbicacion) null else lugar ?: this.lugar,
        textoOriginal = textoOriginal ?: this.textoOriginal,
        // Elegir una categoría de siempre quita la propia; elegir una propia la pone.
        categoriaPropia = categoriaPropia ?: if (category != null) null else this.categoriaPropia,
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Movimiento && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Movimiento(id: $id, ${tipo.clave}, amount: $amount, merchant: $merchant)"
}

/**
 * Cómo se guarda la categoría de un movimiento en una sola columna de texto:
 * `otros` o, con categoría propia, `otros|<id>`. Una versión anterior de la app
 * ve solo `otros|…`, no la reconoce y la trata como Otros.
 */
fun categoriaAlmacenada(categoria: Categoria, propia: String?): String =
    if (propia == null) categoria.clave else "${categoria.clave}|$propia"

data class CategoriaLeida(val categoria: Categoria, val propia: String?)

/** Inversa de [categoriaAlmacenada]; si el nombre no se reconoce usa [respaldo]. */
fun leerCategoriaAlmacenada(raw: String?, respaldo: Categoria): CategoriaLeida {
    if (raw.isNullOrEmpty()) return CategoriaLeida(respaldo, null)
    val corte = raw.indexOf('|')
    val nombre = if (corte < 0) raw else raw.substring(0, corte)
    val propia = if (corte < 0 || corte == raw.length - 1) null else raw.substring(corte + 1)
    return CategoriaLeida(Categoria.desdeClave(nombre) ?: respaldo, propia)
}
