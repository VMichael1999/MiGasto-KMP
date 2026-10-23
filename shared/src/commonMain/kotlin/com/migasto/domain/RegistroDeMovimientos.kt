package com.migasto.domain

import com.migasto.db.RepositorioAjustes
import com.migasto.db.RepositorioMovimientos
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Port de la lógica de ExpensesNotifier (lib/presentation/providers.dart) sin el estado de la
// pantalla: las apps llaman a estos casos de uso y leen de los repositorios.

/** Qué pasó con un pago detectado. */
sealed interface ResultadoDePago {
    /** El mismo monto y fuente ya se registraron hace menos de 2 minutos. */
    data object Duplicado : ResultadoDePago

    /** El proveedor (Yape, Plin, Google Pay) está apagado en los ajustes. */
    data object ProveedorApagado : ResultadoDePago

    /** Hay que mostrar el aviso. Un gasto no se guarda hasta confirmarlo; un ingreso ya queda pendiente. */
    data class Aviso(val movimiento: Movimiento) : ResultadoDePago

    /** Un ingreso que se guardó solo, confirmado, porque el usuario lo pidió. */
    data class GuardadoAutomaticamente(val movimiento: Movimiento) : ResultadoDePago
}

/**
 * Un pago que detectó el código nativo (notificación, captura, Apple Pay). Reemplaza al JSON de
 * la cola nativa: ahora el código nativo lo entrega directo a `shared`.
 */
data class PagoNativo(
    val amount: Double,
    val peer: String,
    /** `yape`, `plin`, `googlePay`, `tarjeta`... (clave de [PaymentSource]). */
    val provider: String,
    val tipo: TipoMovimiento,
    val textoOriginal: String,
    val confirmado: Boolean,
    val en: Instant,
    val canal: CanalMovimiento = CanalMovimiento.Notificacion,
    val id: String? = null,
    /** Si ya pasó por los frenos de duplicados de Android, aquí no se vuelve a revisar. */
    val desdeAndroid: Boolean = false,
    val tarjeta: String? = null,
    val categoriaElegida: Categoria? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precision: Double? = null,
    val lugar: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class RegistroDeMovimientos(
    private val movimientos: RepositorioMovimientos,
    private val ajustes: RepositorioAjustes,
    private val clasificador: ClasificadorCategorias,
    private val reloj: () -> Instant = { Clock.System.now() },
    private val nuevoId: () -> String = { Uuid.random().toString() },
) {
    /** Como [registrarPagoDetectado], pero respeta los proveedores que el usuario apagó. */
    fun alDetectarPago(
        monto: Double,
        comercio: String,
        proveedor: String,
        textoOriginal: String,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        canal: CanalMovimiento = CanalMovimiento.Notificacion,
        pendienteActual: Movimiento? = null,
    ): ResultadoDePago =
        if (!ajustes.proveedorActivo(proveedor)) {
            ResultadoDePago.ProveedorApagado
        } else {
            registrarPagoDetectado(monto, comercio, proveedor, textoOriginal, tipo, canal, pendienteActual)
        }

    /**
     * Un pago detectado. Los gastos quedan como aviso para confirmar (se guardan solos tras la cuenta
     * regresiva). Los ingresos se guardan como pendientes y esperan la confirmación del usuario.
     * [pendienteActual] es el aviso que se está mostrando, que también cuenta para los duplicados.
     */
    fun registrarPagoDetectado(
        monto: Double,
        comercio: String,
        proveedor: String,
        textoOriginal: String,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        canal: CanalMovimiento = CanalMovimiento.Notificacion,
        pendienteActual: Movimiento? = null,
    ): ResultadoDePago {
        val fuente = PaymentSource.desdeClave(proveedor) ?: PaymentSource.Otro
        val ahora = reloj()

        // Regla de duplicados: mismo monto y fuente dentro de 2 minutos.
        if (esMovimientoDuplicado(movimientos.todos() + listOfNotNull(pendienteActual), monto, fuente, ahora, tipo)) {
            return ResultadoDePago.Duplicado
        }

        val movimiento = Movimiento(
            id = nuevoId(),
            amount = monto,
            merchant = comercio,
            category = categoriaPara(tipo, textoOriginal, comercio),
            source = fuente,
            date = ahora,
            tipo = tipo,
            canal = canal,
            estado = EstadoMovimiento.Pendiente,
            textoOriginal = textoOriginal,
        )

        if (tipo == TipoMovimiento.Ingreso) {
            if (ajustes.guardarIngresosAutomaticamente()) {
                // El usuario pidió guardar los ingresos solos.
                val guardado = movimiento.copiar(estado = EstadoMovimiento.Confirmado)
                movimientos.guardar(guardado)
                return ResultadoDePago.GuardadoAutomaticamente(guardado)
            }
            // Un ingreso no se guarda solo como confirmado, pero sí queda en «Por confirmar» aunque se ignore el aviso.
            movimientos.guardar(movimiento)
        }
        return ResultadoDePago.Aviso(movimiento)
    }

    /**
     * Guarda un pago que entregó el código nativo, incluso con la app cerrada. Devuelve el movimiento
     * guardado o `null` si era un duplicado.
     */
    fun registrarPagoNativo(pago: PagoNativo): Movimiento? {
        val fuente = PaymentSource.desdeClave(pago.provider) ?: PaymentSource.Otro

        // Un pago que llegó por dos vías cuenta una sola vez. Lo que viene de Android ya pasó por los frenos
        // nativos (que sí distinguen dos yapes iguales seguidos): si se repitiera aquí, el segundo yape del
        // mismo monto se perdería en silencio.
        if (!pago.desdeAndroid && esMovimientoDuplicado(movimientos.todos(), pago.amount, fuente, pago.en, pago.tipo)) {
            return null
        }

        // La categoría elegida en la captura se respeta si corresponde al tipo.
        val elegida = pago.categoriaElegida?.takeIf { it.esDeIngreso == (pago.tipo == TipoMovimiento.Ingreso) }
        val movimiento = Movimiento(
            id = pago.id ?: nuevoId(),
            amount = pago.amount,
            merchant = pago.peer,
            category = elegida ?: categoriaPara(pago.tipo, pago.textoOriginal, pago.peer),
            source = fuente,
            date = pago.en,
            tipo = pago.tipo,
            canal = pago.canal,
            estado = if (pago.confirmado) EstadoMovimiento.Confirmado else EstadoMovimiento.Pendiente,
            tarjeta = pago.tarjeta?.trim()?.takeIf { it.isNotEmpty() },
            latitud = pago.latitud,
            longitud = pago.longitud,
            precision = pago.precision,
            lugar = pago.lugar,
            textoOriginal = pago.textoOriginal,
        )
        movimientos.guardar(movimiento)
        return movimiento
    }

    /**
     * Guarda un pago que leyó el código nativo cuando no hay ventana flotante que mostrar (pantalla apagada o
     * bloqueada, o sin el permiso). Un gasto se guarda confirmado; un ingreso, solo si el usuario pidió guardar
     * los ingresos solos: si no, queda en «Por confirmar». El código nativo ya frenó los duplicados.
     */
    fun guardarPagoLeido(pago: PagoLeido, texto: String): PagoGuardado {
        val guardado = !pago.esIngreso || ajustes.guardarIngresosAutomaticamente()
        val movimiento = registrarPagoNativo(
            PagoNativo(
                amount = pago.amount, peer = pago.peer, provider = pago.provider.clave,
                tipo = if (pago.esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto,
                textoOriginal = texto, confirmado = guardado, en = reloj(), canal = CanalMovimiento.Notificacion,
                id = nuevoId(), desdeAndroid = true,
            ),
        ) ?: error("un pago que llegó por el código nativo nunca se descarta como duplicado")
        return PagoGuardado(movimiento, guardado)
    }

    /**
     * Guarda un pago que se leyó de una captura de pantalla compartida (iPhone). Como no pasó por los frenos de Android,
     * aquí sí se revisa que no sea el mismo pago que llegó hace poco por otra vía: en ese caso devuelve `null`.
     */
    fun guardarPagoDeCaptura(pago: PagoLeido, texto: String, categoriaElegida: Categoria? = null): PagoGuardado? {
        val guardado = !pago.esIngreso || ajustes.guardarIngresosAutomaticamente()
        val movimiento = registrarPagoNativo(
            PagoNativo(
                amount = pago.amount, peer = pago.peer, provider = pago.provider.clave,
                tipo = if (pago.esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto,
                textoOriginal = texto, confirmado = guardado, en = reloj(), canal = CanalMovimiento.Captura,
                id = nuevoId(), categoriaElegida = categoriaElegida,
            ),
        ) ?: return null
        return PagoGuardado(movimiento, guardado)
    }

    /** La categoría que se muestra en la ventana flotante: la aprendida, o la de las reglas. */
    fun categoriaSugerida(pago: PagoLeido, texto: String): Categoria =
        categoriaPara(if (pago.esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto, texto, pago.peer)

    /** Arma la ventana de un pago nuevo; ve si el usuario pidió guardar los ingresos solos. */
    fun nuevaSesionDeVentana(pago: PagoLeido, texto: String): SesionDeVentana =
        SesionDeVentana(pago, texto, categoriaSugerida(pago, texto), guardarSolo = !pago.esIngreso || ajustes.guardarIngresosAutomaticamente())

    /** Hace lo que decidió la ventana. Devuelve el movimiento guardado, o `null` si se descartó. */
    fun aplicar(accion: AccionDeVentana, pago: PagoLeido, texto: String): Movimiento? {
        val confirmado = when (accion) {
            AccionDeVentana.Descartar -> return null
            AccionDeVentana.GuardarConfirmado -> true
            AccionDeVentana.DejarPendiente, AccionDeVentana.DejarPendienteYAbrirLaApp -> false
        }
        return registrarPagoNativo(
            PagoNativo(
                amount = pago.amount, peer = pago.peer, provider = pago.provider.clave,
                tipo = if (pago.esIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto,
                textoOriginal = texto, confirmado = confirmado, en = reloj(), canal = CanalMovimiento.Notificacion,
                id = nuevoId(), desdeAndroid = true,
            ),
        )
    }

    /** Confirma el aviso que se estaba mostrando: lo guarda y aprende la categoría que eligió el usuario. */
    fun confirmarPendiente(pendiente: Movimiento, categoriaFinal: Categoria, notas: String): Movimiento {
        val confirmado = pendiente.copiar(category = categoriaFinal, notes = notas, estado = EstadoMovimiento.Confirmado)
        ajustes.guardarAprendida(pendiente.merchant, categoriaFinal)
        movimientos.guardar(confirmado)
        return confirmado
    }

    fun agregarManual(
        tipo: TipoMovimiento,
        monto: Double,
        comercio: String,
        categoria: Categoria,
        categoriaPropia: String? = null,
        fuente: PaymentSource = PaymentSource.Manual,
    ): Movimiento {
        val movimiento = Movimiento(
            id = nuevoId(),
            amount = monto,
            merchant = comercio,
            category = categoria,
            categoriaPropia = categoriaPropia,
            source = fuente,
            date = reloj(),
            tipo = tipo,
            canal = CanalMovimiento.Manual,
            estado = EstadoMovimiento.Confirmado,
            notes = "Registro manual",
        )
        movimientos.guardar(movimiento)
        return movimiento
    }

    /** Confirma un movimiento que estaba en «Por confirmar». No hace nada si ya no existe. */
    fun confirmar(id: String, categoria: Categoria? = null) {
        val actual = movimientos.todos().firstOrNull { it.id == id } ?: return
        movimientos.guardar(actual.copiar(category = categoria, estado = EstadoMovimiento.Confirmado))
    }

    fun eliminar(id: String) = movimientos.eliminar(id)

    /** Vuelve a guardar un movimiento eliminado, con su mismo id (para «Deshacer»). */
    fun restaurar(movimiento: Movimiento) {
        if (movimientos.todos().any { it.id == movimiento.id }) return
        movimientos.guardar(movimiento)
    }

    /**
     * Cambia la categoría de un movimiento. Con [recordar] (solo en gastos) se aprende para los pagos futuros a
     * ese comercio y se aplica a los demás gastos del mismo comercio; una categoría propia ([propia]) se aplica
     * pero no se aprende. Elegir una de siempre ([propia] = `null`) quita la propia.
     */
    fun cambiarCategoria(id: String, categoria: Categoria, recordar: Boolean = false, propia: String? = null) {
        val actual = movimientos.todos().firstOrNull { it.id == id } ?: return
        movimientos.guardar(actual.copiar(category = categoria, categoriaPropia = propia))
        if (!recordar || actual.esIngreso) return

        if (propia == null) ajustes.guardarAprendida(actual.merchant, categoria)
        val clave = actual.merchant.lowercase().trim()
        movimientos.todos()
            .filter {
                it.esGasto && it.id != id && it.merchant.lowercase().trim() == clave &&
                    (it.category != categoria || it.categoriaPropia != propia)
            }
            .forEach { movimientos.guardar(it.copiar(category = categoria, categoriaPropia = propia)) }
    }

    /**
     * Edita monto, comercio y nota. [monto] es el texto escrito (acepta coma decimal). Devuelve `false`, sin
     * cambiar nada, si el monto no es mayor que cero, el comercio está vacío o el movimiento ya no existe.
     */
    fun editar(id: String, monto: String, comercio: String, notas: String): Boolean {
        val valor = parsearMonto(monto)
        if (valor == null || valor <= 0 || comercio.isBlank()) return false
        val actual = movimientos.todos().firstOrNull { it.id == id } ?: return false
        // `copiar` no puede vaciar un campo, así que la nota se arma con el constructor.
        movimientos.guardar(
            Movimiento(
                id = actual.id, amount = valor, merchant = comercio.trim(), category = actual.category,
                source = actual.source, date = actual.date, notes = notas.trim(), tipo = actual.tipo, canal = actual.canal,
                estado = actual.estado, tarjeta = actual.tarjeta, latitud = actual.latitud, longitud = actual.longitud,
                precision = actual.precision, lugar = actual.lugar, textoOriginal = actual.textoOriginal,
                categoriaPropia = actual.categoriaPropia,
            ),
        )
        return true
    }

    /** Guarda dónde se hizo el pago (solo cuando el usuario lo pide). */
    fun ponerUbicacion(id: String, latitud: Double, longitud: Double, precision: Double?, lugar: String?) {
        val actual = movimientos.todos().firstOrNull { it.id == id } ?: return
        movimientos.guardar(actual.copiar(latitud = latitud, longitud = longitud, precision = precision, lugar = lugar))
    }

    fun ponerUbicacion(id: String, ubicacion: UbicacionCapturada) =
        ponerUbicacion(id, ubicacion.latitud, ubicacion.longitud, ubicacion.precision, ubicacion.lugar)

    /** Quita la ubicación de un solo movimiento. */
    fun quitarUbicacion(id: String) {
        val actual = movimientos.todos().firstOrNull { it.id == id } ?: return
        movimientos.guardar(actual.copiar(quitarUbicacion = true))
    }

    /** Borra todas las ubicaciones; los movimientos se mantienen. */
    fun quitarTodasLasUbicaciones() {
        movimientos.todos().filter { it.tieneUbicacion || it.lugar != null }
            .forEach { movimientos.guardar(it.copiar(quitarUbicacion = true)) }
    }

    private fun categoriaPara(tipo: TipoMovimiento, texto: String, comercio: String): Categoria {
        if (tipo == TipoMovimiento.Ingreso) return clasificador.clasificarIngreso(texto, comercio)
        // 1. Categoría aprendida de los cambios del usuario para este comercio.
        ajustes.categoriaAprendida(comercio)?.takeIf { !it.esDeIngreso }?.let { return it }
        // 2. Reglas por palabras clave.
        return clasificador.clasificarGasto(texto, comercio)
    }
}

/** Los casos de uso de registrar, confirmar y eliminar movimientos sobre [base], con las reglas de categorías incluidas. */
fun crearRegistroDeMovimientos(base: com.migasto.db.BaseMiGasto): RegistroDeMovimientos =
    RegistroDeMovimientos(base.movimientos, base.ajustes, ClasificadorCategorias.desdeJson(com.migasto.domain.reglas.ReglasIncluidas.categorias))
