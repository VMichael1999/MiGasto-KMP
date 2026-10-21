package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

// Port de _filtered, _buildList y _dayLabel de lib/presentation/expenses/expenses_screen.dart.
class ListaDeMovimientosTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 12, minuto = 0, mes = 10)

    private fun mov(
        id: String,
        monto: Double = 10.0,
        comercio: String = id,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        categoria: Categoria = Categoria.Otros,
        propia: String? = null,
        fuente: PaymentSource = PaymentSource.Yape,
        fecha: Instant = ahora,
    ) = Movimiento(id, monto, comercio, categoria, fuente, fecha, tipo = tipo, estado = estado, categoriaPropia = propia)

    private val propias = listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto))

    private fun filtrar(todos: List<Movimiento>, filtros: FiltrosDeMovimientos, alias: Map<String, String> = emptyMap()) =
        filtrarMovimientos(todos, filtros, alias, propias).map { it.id }

    // Filtros

    @Test
    fun filtraPorTipo() {
        val todos = listOf(
            mov("g"),
            mov("i", tipo = TipoMovimiento.Ingreso),
            mov("p", tipo = TipoMovimiento.Ingreso, estado = EstadoMovimiento.Pendiente),
        )
        assertEquals(setOf("g", "i", "p"), filtrar(todos, FiltrosDeMovimientos()).toSet())
        assertEquals(listOf("g"), filtrar(todos, FiltrosDeMovimientos(tipo = FiltroDeTipo.Gastos)))
        assertEquals(setOf("i", "p"), filtrar(todos, FiltrosDeMovimientos(tipo = FiltroDeTipo.Ingresos)).toSet())
        assertEquals(listOf("p"), filtrar(todos, FiltrosDeMovimientos(tipo = FiltroDeTipo.PorConfirmar)))
    }

    @Test
    fun buscaPorNombreOriginalOPorAliasSinImportarMayusculas() {
        val todos = listOf(mov("a", comercio = "MICHAEL ANTHONY VALDIVIEZO MAZA"), mov("b", comercio = "Tambo Surco"))
        val alias = mapOf("michael anthony valdiviezo maza" to "Mi hermano")
        assertEquals(listOf("a"), filtrar(todos, FiltrosDeMovimientos(busqueda = "valdiviezo"), alias))
        assertEquals(listOf("a"), filtrar(todos, FiltrosDeMovimientos(busqueda = "HERMANO"), alias))
        assertEquals(listOf("b"), filtrar(todos, FiltrosDeMovimientos(busqueda = "  tambo  "), alias))
        assertEquals(emptyList(), filtrar(todos, FiltrosDeMovimientos(busqueda = "zzz"), alias))
    }

    @Test
    fun filtraPorCategoriaDeSiempreOPropia() {
        val todos = listOf(
            mov("c", categoria = Categoria.Compras),
            mov("p", categoria = Categoria.Otros, propia = "c1"),
            mov("o", categoria = Categoria.Otros),
        )
        assertEquals(listOf("c"), filtrar(todos, FiltrosDeMovimientos(categoria = "compras")))
        assertEquals(listOf("p"), filtrar(todos, FiltrosDeMovimientos(categoria = "c1")))
        assertEquals(listOf("o"), filtrar(todos, FiltrosDeMovimientos(categoria = "otros")))
    }

    @Test
    fun unaCategoriaPropiaBorradaCuentaComoOtros() {
        val huerfano = mov("h", categoria = Categoria.Otros, propia = "borrada")
        assertEquals(listOf("h"), filtrar(listOf(huerfano), FiltrosDeMovimientos(categoria = "otros")))
    }

    @Test
    fun filtraPorFuenteYPorRangoDeMontoConLosExtremosIncluidos() {
        val todos = listOf(
            mov("a", monto = 10.0, fuente = PaymentSource.Yape),
            mov("b", monto = 50.0, fuente = PaymentSource.Plin),
            mov("c", monto = 100.0, fuente = PaymentSource.Plin),
        )
        assertEquals(listOf("b", "c"), filtrar(todos, FiltrosDeMovimientos(fuente = PaymentSource.Plin)).sorted())
        assertEquals(setOf("b", "c"), filtrar(todos, FiltrosDeMovimientos(montoMinimo = 50.0, montoMaximo = 100.0)).toSet())
        assertEquals(listOf("b"), filtrar(todos, FiltrosDeMovimientos(fuente = PaymentSource.Plin, montoMaximo = 99.99)))
    }

    @Test
    fun losFiltrosSeCombinanYSeContanLosActivos() {
        val f = FiltrosDeMovimientos(categoria = "compras", fuente = PaymentSource.Yape, montoMinimo = 5.0)
        assertEquals(3, f.cantidadDeFiltrosActivos)
        assertEquals(0, FiltrosDeMovimientos(busqueda = "x", tipo = FiltroDeTipo.Gastos).cantidadDeFiltrosActivos)
        assertEquals(true, FiltrosDeMovimientos(busqueda = "x").hayAlgunFiltro)
        assertEquals(false, FiltrosDeMovimientos().hayAlgunFiltro)
    }

    @Test
    fun ordenaDelMasRecienteAlMasAntiguo() {
        val todos = listOf(mov("viejo", fecha = fecha(dia = 1, mes = 10)), mov("nuevo", fecha = fecha(dia = 20, mes = 10)))
        assertEquals(listOf("nuevo", "viejo"), filtrar(todos, FiltrosDeMovimientos()))
    }

    // Agrupar por día

    @Test
    fun agrupaPorDiaConElTotalDeGastosConfirmados() {
        val todos = listOf(
            mov("a", 25.0, fecha = fecha(dia = 21, hora = 11, minuto = 0, mes = 10)),
            mov("b", 89.2, fecha = fecha(dia = 21, hora = 10, minuto = 20, mes = 10)),
            mov("i", 150.0, tipo = TipoMovimiento.Ingreso, fecha = fecha(dia = 21, hora = 11, minuto = 25, mes = 10)),
            mov("p", 40.0, estado = EstadoMovimiento.Pendiente, fecha = fecha(dia = 21, hora = 9, minuto = 0, mes = 10)),
            mov("ayer", 12.0, fecha = fecha(dia = 20, hora = 18, minuto = 0, mes = 10)),
        )
        val grupos = agruparPorDia(todos, ahora, utc)
        assertEquals(listOf(LocalDate(2026, 10, 21), LocalDate(2026, 10, 20)), grupos.map { it.dia })
        assertEquals(114.2, grupos[0].totalGastos, 0.001) // sin el ingreso ni el pendiente
        assertEquals(listOf("i", "a", "b", "p"), grupos[0].movimientos.map { it.id })
        assertEquals(12.0, grupos[1].totalGastos, 0.001)
    }

    @Test
    fun unDiaSinGastosConfirmadosTieneTotalCero() {
        val grupos = agruparPorDia(listOf(mov("i", tipo = TipoMovimiento.Ingreso)), ahora, utc)
        assertEquals(0.0, grupos.single().totalGastos)
    }

    @Test
    fun elDiaSeCuentaEnLaHoraLocal() {
        val lima = TimeZone.of("America/Lima")
        // 22 de octubre 03:00 UTC es el 21 a las 22:00 en Lima.
        val tarde = mov("t", fecha = fecha(dia = 22, hora = 3, minuto = 0, mes = 10))
        assertEquals(LocalDate(2026, 10, 21), agruparPorDia(listOf(tarde), ahora, lima).single().dia)
        assertEquals(LocalDate(2026, 10, 22), agruparPorDia(listOf(tarde), ahora, utc).single().dia)
    }

    @Test
    fun sinMovimientosNoHayGrupos() = assertEquals(emptyList(), agruparPorDia(emptyList(), ahora, utc))

    @Test
    fun etiquetasDelDia() {
        val hoy = LocalDate(2026, 10, 21)
        assertEquals("Hoy, miércoles 21", etiquetaDelDia(LocalDate(2026, 10, 21), hoy))
        assertEquals("Ayer, martes 20", etiquetaDelDia(LocalDate(2026, 10, 20), hoy))
        assertEquals("Jueves 15 de octubre", etiquetaDelDia(LocalDate(2026, 10, 15), hoy))
        assertEquals("Jueves 1 de octubre", etiquetaDelDia(LocalDate(2026, 10, 1), hoy))
        assertEquals("Domingo 28 de diciembre", etiquetaDelDia(LocalDate(2025, 12, 28), hoy))
    }

    @Test
    fun ayerCruzaElCambioDeMes() {
        assertEquals("Ayer, sábado 31", etiquetaDelDia(LocalDate(2026, 10, 31), LocalDate(2026, 11, 1)))
    }
}
