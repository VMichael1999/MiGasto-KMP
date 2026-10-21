package com.migasto.domain

// Port de la lógica de lib/presentation/expenses/manual_entry_screen.dart y new_category_sheet.dart.

/**
 * Lo que el usuario va tecleando como monto. Es inmutable: cada tecla devuelve una entrada nueva.
 * Teclas: `0`–`9`, `.` y `<` (borrar).
 */
data class EntradaDeMonto(val crudo: String = "") {
    fun presionar(tecla: Char): EntradaDeMonto {
        if (tecla == '<') return EntradaDeMonto(crudo.dropLast(1))
        if (tecla == '.') {
            if ('.' in crudo) return this
            return EntradaDeMonto(if (crudo.isEmpty()) "0." else "$crudo.")
        }
        if (tecla !in '0'..'9') return this
        val partes = crudo.split('.')
        if (partes.size == 2 && partes[1].length >= 2) return this // 2 decimales
        if (partes.size == 1 && partes[0].length >= 7) return this // hasta millones
        return EntradaDeMonto(if (crudo == "0") tecla.toString() else crudo + tecla)
    }

    /** El monto tecleado; un punto al final cuenta como cero decimal. */
    val monto: Double get() = (if (crudo.endsWith('.')) crudo + "0" else crudo).toDoubleOrNull() ?: 0.0

    /** Lo que se ve en pantalla: `0.00`, `180.00` o, mientras se escriben decimales, tal cual (`12.5`). */
    val texto: String
        get() = when {
            crudo.isEmpty() -> "0.00"
            '.' in crudo -> crudo
            else -> formatearMonto(crudo.toDouble())
        }
}

/** El formulario de «Nuevo movimiento». Es inmutable; las pantallas lo guardan y lo reemplazan. */
data class FormularioNuevoMovimiento(
    val tipo: TipoMovimiento = TipoMovimiento.Gasto,
    val entrada: EntradaDeMonto = EntradaDeMonto(),
    val concepto: String = "",
    val fuente: PaymentSource = PaymentSource.Efectivo,
    val categoria: Categoria = categoriaPorDefecto(TipoMovimiento.Gasto),
    /** Id de la categoría propia elegida; [categoria] es entonces la de siempre, para los totales. */
    val categoriaPropia: String? = null,
) {
    val esIngreso: Boolean get() = tipo == TipoMovimiento.Ingreso
    val monto: Double get() = entrada.monto

    /** Se necesita un monto mayor que cero y un detalle. */
    val puedeGuardar: Boolean get() = monto > 0 && concepto.isNotBlank()

    val conceptoLimpio: String get() = concepto.trim()

    val pistaDelConcepto: String get() = if (esIngreso) "De quién o por qué" else "Comercio o detalle"
    val prefijoDeLaFuente: String get() = if (esIngreso) "Recibido por" else "Pagado con"
    val prefijoDelMonto: String get() = if (esIngreso) "+ S/ " else "S/ "
    val mensajeAlGuardar: String get() = if (esIngreso) "Ingreso guardado" else "Gasto guardado"

    /** `Guardar gasto · S/ 25.00`; sin monto, solo `Guardar gasto`. */
    val textoDelBoton: String
        get() {
            val base = if (esIngreso) "Guardar ingreso" else "Guardar gasto"
            return if (monto > 0) "$base · ${formatearSoles(monto)}" else base
        }

    fun conEntrada(nueva: EntradaDeMonto) = copy(entrada = nueva)

    fun conConcepto(texto: String) = copy(concepto = texto)

    fun conFuente(nueva: PaymentSource) = copy(fuente = nueva)

    /** Cambiar de tipo reinicia la categoría: las de gasto y las de ingreso no se mezclan. */
    fun conTipo(nuevo: TipoMovimiento) = copy(tipo = nuevo, categoria = categoriaPorDefecto(nuevo), categoriaPropia = null)

    /** Elegir una categoría de siempre quita la propia. */
    fun conCategoria(nueva: Categoria) = copy(categoria = nueva, categoriaPropia = null)

    /** Elegir una propia guarda también su categoría de siempre (Otros / Otros ingresos). */
    fun conPropia(propia: CategoriaPropia) = copy(categoria = propia.base, categoriaPropia = propia.id)

    companion object {
        fun categoriaPorDefecto(tipo: TipoMovimiento) =
            if (tipo == TipoMovimiento.Ingreso) Categoria.OtrosIngresos else Categoria.Otros

        val fuentesPosibles = listOf(
            PaymentSource.Efectivo, PaymentSource.Yape, PaymentSource.Plin, PaymentSource.Tarjeta, PaymentSource.Otro,
        )

        fun etiquetaDeLaFuente(fuente: PaymentSource) = if (fuente == PaymentSource.Otro) "Otro" else fuente.etiqueta
    }
}

const val LARGO_MAXIMO_DEL_NOMBRE_DE_CATEGORIA = 20

fun limitarNombreDeCategoria(nombre: String) = nombre.take(LARGO_MAXIMO_DEL_NOMBRE_DE_CATEGORIA)

/**
 * Claves de los íconos que se pueden elegir para una categoría propia (las mismas de `customCategoryIcons`
 * de Flutter, más `scissors` del diseño). Cada interfaz dibuja el suyo; una clave desconocida se ve genérica.
 */
val clavesDeIconosPropios: List<String> = listOf(
    "restaurante", "cafe", "pizza", "hamburguesa", "pastel", "bar", "carrito", "bolsa", "tienda", "ropa",
    "bus", "auto", "gasolina", "moto", "avion", "hotel", "casa", "llave", "luz", "agua", "wifi", "celular",
    "tele", "cine", "musica", "juegos", "futbol", "gimnasio", "spa", "salud", "medicina", "escuela", "libro",
    "bebe", "mascota", "regalo", "fiesta", "ahorro", "banco", "tarjeta", "pagos", "trabajo", "herramientas",
    "arreglo", "computadora", "camara", "arte", "lavanderia", "donacion", "corazon", "estrella", "planta",
    "parque", "playa", "maleta", "recibo", "inversion", "flores", "scissors",
)
