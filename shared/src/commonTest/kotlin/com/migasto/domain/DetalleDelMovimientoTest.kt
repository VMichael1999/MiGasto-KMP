package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Port de la boleta de lib/presentation/expenses/movement_detail_screen.dart (textos, fecha, canal) y
// de alias_dialog.dart (nombre sugerido).
class DetalleDelMovimientoTest {
    private val utc = TimeZone.UTC
    private val propias = listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto))

    private fun mov(
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        fuente: PaymentSource = PaymentSource.Efectivo,
        canal: CanalMovimiento = CanalMovimiento.Manual,
        notas: String = "Registro manual",
    ) = Movimiento(
        id = "m", amount = 25.0, merchant = "BARBERÍA EL PATRÓN", category = Categoria.Otros, source = fuente,
        date = fecha(dia = 1, hora = 11, minuto = 0, mes = 10), notes = notas, tipo = tipo, canal = canal, estado = estado,
        categoriaPropia = "c1", textoOriginal = "texto",
    )

    private fun detalle(m: Movimiento, alias: Map<String, String> = emptyMap()) = construirDetalle(m, alias, propias, utc)

    @Test
    fun laBoletaDeUnGastoManual() {
        val d = detalle(mov(), mapOf("barbería el patrón" to "Barbería El Patrón"))
        assertEquals("Gasto", d.tipoTexto)
        assertEquals("Barbería El Patrón", d.nombre)
        assertEquals("BARBERÍA EL PATRÓN", d.nombreOriginal)
        assertEquals("Para", d.etiquetaDelNombre)
        assertEquals("Pagado con", d.etiquetaDeLaFuente)
        assertEquals("Efectivo", d.fuenteTexto)
        assertEquals("jue 1 oct, 11:00", d.fecha)
        assertEquals("Barbería", d.categoriaEtiqueta)
        assertEquals("scissors", d.iconoPropio)
        assertEquals("Manual", d.registrado)
        assertNull(d.nota)
        assertEquals(false, d.porConfirmar)
        assertEquals("25.00", d.montoParaEditar)
        assertEquals("Eliminar gasto", d.textoDeEliminar)
        assertEquals("Gasto de \"Barbería El Patrón\" eliminado", d.avisoDeEliminado)
        assertEquals("¿Eliminar \"Barbería El Patrón\" por S/ 25.00?", d.preguntaDeEliminar)
    }

    @Test
    fun laBoletaDeUnIngresoPendiente() {
        val d = detalle(mov(tipo = TipoMovimiento.Ingreso, estado = EstadoMovimiento.Pendiente, fuente = PaymentSource.Yape))
        assertEquals("Ingreso", d.tipoTexto)
        assertEquals("De", d.etiquetaDelNombre)
        assertEquals("Recibido por", d.etiquetaDeLaFuente)
        assertEquals(true, d.porConfirmar)
        assertEquals("Guardar ingreso", d.textoDeConfirmar)
        assertEquals("Eliminar ingreso", d.textoDeEliminar)
        assertEquals("Ingreso guardado", d.avisoDeGuardado)
    }

    @Test
    fun laTarjetaSeAgregaALaFuente() {
        val m = mov(fuente = PaymentSource.Tarjeta, canal = CanalMovimiento.Wallet).copiar(tarjeta = "Visa BBVA ···4821")
        assertEquals("Tarjeta · Visa BBVA ···4821", detalle(m).fuenteTexto)
    }

    @Test
    fun comoSeRegistroSegunElCanalYLaFuente() {
        assertEquals("Automático, desde la notificación", detalle(mov(canal = CanalMovimiento.Notificacion, fuente = PaymentSource.Yape)).registrado)
        assertEquals("Automático, desde Google Wallet", detalle(mov(canal = CanalMovimiento.Notificacion, fuente = PaymentSource.GooglePay)).registrado)
        assertEquals("Automático, desde Wallet", detalle(mov(canal = CanalMovimiento.Wallet)).registrado)
        assertEquals("Desde una captura", detalle(mov(canal = CanalMovimiento.Captura)).registrado)
        assertEquals("Manual", detalle(mov(canal = CanalMovimiento.Manual)).registrado)
    }

    @Test
    fun laNotaSoloSeMuestraSiLaEscribioElUsuario() {
        assertNull(detalle(mov(notas = "")).nota)
        assertNull(detalle(mov(notas = "Registro manual")).nota)
        assertEquals("almuerzo", detalle(mov(notas = "almuerzo")).nota)
    }

    @Test
    fun unaCategoriaPropiaBorradaSeVeComoOtros() {
        val d = construirDetalle(mov(), emptyMap(), emptyList(), utc)
        assertEquals("Otros", d.categoriaEtiqueta)
        assertNull(d.iconoPropio)
    }

    @Test
    fun laFechaEnEspanolSinPuntos() {
        fun f(dia: Int, mes: Int, hora: Int = 9, minuto: Int = 5) =
            detalle(mov().copiar(date = fecha(dia = dia, hora = hora, minuto = minuto, mes = mes))).fecha
        assertEquals("mié 21 oct, 09:05", f(21, 10))
        assertEquals("lun 5 ene, 09:05", f(5, 1))
        assertEquals("sáb 5 sept, 09:05", f(5, 9))
        assertEquals("vie 25 dic, 09:05", f(25, 12))
    }

    @Test
    fun laFechaSeEscribeEnLaHoraLocal() {
        val lima = TimeZone.of("America/Lima")
        assertEquals("jue 1 oct, 06:00", construirDetalle(mov(), emptyMap(), propias, lima).fecha)
    }

    @Test
    fun conUbicacionSeMuestraElLugar() {
        val d = detalle(mov().copiar(latitud = -12.09, longitud = -77.03, lugar = "Av. Arequipa 3120, San Isidro"))
        assertEquals(true, d.tieneUbicacion)
        assertEquals("Av. Arequipa 3120, San Isidro", d.lugar)
        assertEquals(false, detalle(mov()).tieneUbicacion)
    }

    @Test
    fun laUbicacionTraeLasCoordenadasLaDireccionYLaPrecisionParaElMapa() {
        val d = detalle(mov().copiar(latitud = -12.09, longitud = -77.03, precision = 15.0, lugar = "Av. Arequipa 3120, San Isidro"))
        assertEquals(-12.09, d.latitud)
        assertEquals(-77.03, d.longitud)
        assertEquals("Av. Arequipa 3120, San Isidro", d.direccion)
        assertEquals("Ubicación del teléfono · precisión ±15 m", d.textoDeLaUbicacion)
    }

    @Test
    fun sinDireccionElDetalleMuestraLasCoordenadas() {
        val d = detalle(mov().copiar(latitud = -12.09, longitud = -77.03))
        assertEquals("-12.09000, -77.03000", d.direccion)
        assertEquals("Ubicación del teléfono", d.textoDeLaUbicacion)
    }

    @Test
    fun sinUbicacionNoHayCoordenadasNiTextos() {
        val d = detalle(mov())
        assertNull(d.latitud)
        assertNull(d.longitud)
        assertNull(d.direccion)
        assertNull(d.textoDeLaUbicacion)
    }

    @Test
    fun elTextoOriginalSoloSiHay() {
        assertEquals("texto", detalle(mov()).textoOriginal)
        assertNull(construirDetalle(Movimiento("x", 1.0, "x", Categoria.Otros, PaymentSource.Manual, fecha()), emptyMap(), emptyList(), utc).textoOriginal)
    }

    @Test
    fun elNombreSugeridoParaUnAlias() {
        assertEquals("Michael Anthony", sugerenciaDeAlias("MICHAEL ANTHONY VALDIVIEZO MAZA"))
        assertEquals("Tambo Surco", sugerenciaDeAlias("  tambo   surco  "))
        assertEquals("Kfc", sugerenciaDeAlias("KFC"))
        assertEquals("", sugerenciaDeAlias("   "))
    }

    @Test
    fun parsearUnMontoEscrito() {
        assertEquals(12.5, parsearMonto("12,50"))
        assertEquals(12.5, parsearMonto("12.50"))
        assertEquals(1200.0, parsearMonto(" 1200 "))
        assertNull(parsearMonto("abc"))
        assertNull(parsearMonto(""))
    }

    @Test
    fun laNotaParaEditarNoMuestraRegistroManual() {
        assertEquals("", notaParaEditar(mov(notas = "Registro manual")))
        assertEquals("almuerzo", notaParaEditar(mov(notas = "almuerzo")))
    }
}
