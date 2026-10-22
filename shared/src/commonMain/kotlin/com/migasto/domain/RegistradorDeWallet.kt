package com.migasto.domain

import kotlin.time.Clock
import kotlin.time.Instant

// Port de RegistrarMovimientoIntent.swift y AmountParser (ios/Runner de la app de Flutter). Atajos pasa el monto, el
// comercio y la tarjeta de cada pago con Apple Pay; qué se guarda y qué se le dice al usuario sale de aquí.

private val miles = Regex("""^\d{1,3}(,\d{3})+(\.\d+)?$""")

/** Lee montos como `25.50`, `S/ 25,50` o `1,200.50`, igual que los escribe Atajos. */
fun leerMontoDeAtajos(crudo: String): Double? {
    var texto = crudo.filter { it.isDigit() || it == '.' || it == ',' }
    if (texto.isEmpty()) return null
    texto = if (miles.matches(texto)) texto.replace(",", "") else texto.replace(',', '.')
    return texto.toDoubleOrNull()
}

enum class DesenlaceDeWallet { Guardado, MontoInvalido, Duplicado }

/** Lo que se le cuenta al usuario después de registrar el pago: [titulo] y [detalle] ya vienen armados. */
class ResultadoDeWallet(val desenlace: DesenlaceDeWallet, val titulo: String, val detalle: String)

/** La acción «Registrar movimiento» de Atajos: guarda un gasto de Apple Pay, confirmado, con su tarjeta y ubicación. */
class RegistradorDeWallet(
    private val registro: RegistroDeMovimientos,
    private val reloj: () -> Instant,
) {
    /** Swift no ve los valores por defecto de Kotlin. */
    constructor(registro: RegistroDeMovimientos) : this(registro, { Clock.System.now() })

    fun registrar(monto: String, comercio: String, tarjeta: String?, latitud: Double?, longitud: Double?): ResultadoDeWallet {
        val importe = leerMontoDeAtajos(monto)?.takeIf { it > 0 }
            ?: return ResultadoDeWallet(DesenlaceDeWallet.MontoInvalido, "No pudimos leer el monto. Pásalo como 25.50.", "")
        val lugar = comercio.trim().ifEmpty { "Desconocido" }
        val nombreDeLaTarjeta = tarjeta?.trim()?.takeIf { it.isNotEmpty() }
        val conUbicacion = latitud != null && longitud != null
        val guardado = registro.registrarPagoNativo(
            PagoNativo(
                amount = importe, peer = lugar, provider = PaymentSource.Tarjeta.clave, tipo = TipoMovimiento.Gasto,
                textoOriginal = "Apple Pay: ${formatearSoles(importe)} en $lugar", confirmado = true, en = reloj(),
                canal = CanalMovimiento.Wallet, tarjeta = nombreDeLaTarjeta,
                latitud = latitud.takeIf { conUbicacion }, longitud = longitud.takeIf { conUbicacion },
            ),
        ) ?: return ResultadoDeWallet(DesenlaceDeWallet.Duplicado, "Ese pago ya estaba registrado.", "No se guardó otra vez.")
        val aviso = "Ábrelo para cambiar la categoría."
        return ResultadoDeWallet(
            DesenlaceDeWallet.Guardado,
            "Registrado: ${formatearSoles(guardado.amount)} en $lugar",
            if (nombreDeLaTarjeta != null) "Con $nombreDeLaTarjeta. $aviso" else aviso,
        )
    }
}
