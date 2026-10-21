package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

// Port de la agrupación de lib/presentation/expenses/where_screen.dart (_Zone y el filtro por período).
class DondePagasteTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 27, hora = 12, minuto = 0, mes = 9)

    private fun mov(
        id: String,
        comercio: String,
        monto: Double,
        lugar: String?,
        lat: Double? = -12.09,
        lng: Double? = -77.03,
        hace: kotlin.time.Duration = 0.days,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
    ) = Movimiento(
        id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = PaymentSource.Tarjeta,
        date = ahora - hace, tipo = tipo, latitud = lat, longitud = lng, lugar = lugar,
    )

    private val movs = listOf(
        mov("a", "Wong", 80.0, "Av. Arequipa 3120, San Isidro", lat = -12.09, lng = -77.03),
        mov("b", "Tambo", 25.5, "Calle Los Pinos 10, San Isidro", lat = -12.10, lng = -77.04),
        mov("c", "Plaza Vea", 40.0, "Av. Brasil 100, Jesús María", lat = -12.07, lng = -77.05),
        mov("d", "Sin lugar", 10.0, null, lat = -12.0, lng = -77.0),
        mov("e", "Sin coordenadas", 99.0, "Av. Lima 1, Barranco", lat = null, lng = null),
    )

    private fun zonas(periodo: PeriodoDeUbicaciones = PeriodoDeUbicaciones.Todo, lista: List<Movimiento> = movs) =
        agruparPorZona(lista, periodo, ahora, utc)

    @Test
    fun agrupaPorLaUltimaParteDeLaDireccion() {
        val z = zonas().first { it.nombre == "San Isidro" }
        assertEquals(listOf("a", "b"), z.movimientos.map { it.id })
    }

    @Test
    fun sinDireccionVaEnSinDireccionYSinCoordenadasNoAparece() {
        val nombres = zonas().map { it.nombre }
        assertTrue("Sin dirección" in nombres)
        assertTrue("Barranco" !in nombres)
    }

    @Test
    fun lasZonasVanDeLaQueTieneMasPagosALaQueTieneMenos() {
        assertEquals("San Isidro", zonas().first().nombre)
        assertEquals(2, zonas().first().movimientos.size)
    }

    @Test
    fun laDireccionSinComaSeUsaCompleta() {
        val z = zonas(lista = listOf(mov("x", "Kiosco", 5.0, "Mercado central")))
        assertEquals("Mercado central", z.single().nombre)
    }

    @Test
    fun elTotalSoloSumaGastos() {
        val lista = listOf(
            mov("g", "Wong", 80.0, "Av. Arequipa 3120, San Isidro"),
            mov("i", "Carlos", 50.0, "Av. Arequipa 3120, San Isidro", tipo = TipoMovimiento.Ingreso),
        )
        assertEquals(80.0, zonas(lista = lista).single().totalGastos)
    }

    @Test
    fun elCentroEsElPromedioDeLasCoordenadas() {
        val z = zonas().first { it.nombre == "San Isidro" }
        assertEquals(-12.095, z.latitudDelCentro, 1e-9)
        assertEquals(-77.035, z.longitudDelCentro, 1e-9)
    }

    @Test
    fun elSubtituloEsElComercioDeMayorMontoYCuantosHayAdemas() {
        val alias = mapOf(claveDeAlias("Wong") to "Wong Arequipa")
        val z = zonas().first { it.nombre == "San Isidro" }
        assertEquals("Wong Arequipa y 1 más", z.subtitulo(alias))
        assertEquals("Wong y 1 más", z.subtitulo(emptyMap()))
        assertEquals("Plaza Vea", zonas().first { it.nombre == "Jesús María" }.subtitulo(emptyMap()))
    }

    @Test
    fun elResumenDeLaZonaDiceCuantosPagosYCuantoSeGasto() {
        val z = zonas().first { it.nombre == "San Isidro" }
        assertEquals("2 pagos · S/ 105.50", z.resumen)
        assertEquals("1 pago · S/ 40.00", zonas().first { it.nombre == "Jesús María" }.resumen)
    }

    @Test
    fun laDescripcionParaElLectorDePantallaJuntaTodo() {
        val z = zonas().first { it.nombre == "San Isidro" }
        assertEquals("San Isidro, 2 pagos, S/ 105.50. Wong y 1 más", z.descripcion(emptyMap()))
    }

    @Test
    fun laSemanaSoloTomaLosUltimosSieteDias() {
        val lista = listOf(
            mov("hoy", "Wong", 10.0, "Av. A 1, Lima"),
            mov("hace6", "Wong", 10.0, "Av. A 1, Lima", hace = 6.days),
            mov("hace7", "Wong", 10.0, "Av. A 1, Lima", hace = 7.days),
            mov("hace20", "Wong", 10.0, "Av. A 1, Lima", hace = 20.days),
        )
        assertEquals(listOf("hoy", "hace6"), zonas(PeriodoDeUbicaciones.Semana, lista).single().movimientos.map { it.id })
    }

    @Test
    fun elMesSoloTomaElMesEnCurso() {
        val lista = listOf(
            mov("hoy", "Wong", 10.0, "Av. A 1, Lima"),
            mov("hace10", "Wong", 10.0, "Av. A 1, Lima", hace = 10.days),
            mov("agosto", "Wong", 10.0, "Av. A 1, Lima", hace = 30.days),
        )
        assertEquals(listOf("hoy", "hace10"), zonas(PeriodoDeUbicaciones.Mes, lista).single().movimientos.map { it.id })
    }

    @Test
    fun todoTomaTodoLoQueTieneUbicacion() {
        assertEquals(4, zonas(PeriodoDeUbicaciones.Todo).sumOf { it.movimientos.size })
    }

    @Test
    fun losLimitesDelMapaAbarcanTodosLosPuntos() {
        val l = limitesDelMapa(zonas())!!
        assertEquals(-12.10, l.sur, 1e-9)
        assertEquals(-12.0, l.norte, 1e-9)
        assertEquals(-77.05, l.oeste, 1e-9)
        assertEquals(-77.0, l.este, 1e-9)
        assertEquals(4, l.puntos)
    }

    @Test
    fun sinZonasNoHayLimites() {
        assertEquals(null, limitesDelMapa(emptyList()))
    }

    @Test
    fun losTextosDeLaPantalla() {
        assertEquals("Dónde pagaste", TextosDeDondePagaste.TITULO)
        assertEquals(listOf("Semana", "Mes", "Todo"), PeriodoDeUbicaciones.entries.map { it.etiqueta })
        assertEquals(
            "Todavía no hay pagos con ubicación en este período. Agrégala desde el detalle de un movimiento.",
            TextosDeDondePagaste.VACIO,
        )
        assertEquals("Solo aparecen los pagos con ubicación. Los que no la tienen no se muestran.", TextosDeDondePagaste.PIE)
        assertEquals("El mapa necesita la clave de Google Maps.", TextosDeDondePagaste.SIN_CLAVE)
    }
}
