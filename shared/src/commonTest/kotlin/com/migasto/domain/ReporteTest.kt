package com.migasto.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Port de la lógica de lib/presentation/reports/reports_screen.dart (períodos, comparación, barras por
// mes y tablas por categoría). Los datos son los del diseño (mg-05): octubre deja S/ 2,655.30, que son
// S/ 780.70 más que septiembre; los gastos de octubre son Compras 89.20, Barbería 25.00, Alimentación
// 18.50 y Transporte 12.00.
class ReporteTest {
    private val utc = TimeZone.UTC
    private val hoy = LocalDate(2026, 10, 21)
    private val ahora = fecha(dia = 21, hora = 12, minuto = 0, mes = 10)
    private val propias = listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto))

    private fun mov(
        id: String,
        monto: Double,
        mes: Int,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        dia: Int = 10,
        categoria: Categoria = Categoria.Otros,
        propia: String? = null,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        anio: Int = 2026,
    ) = Movimiento(id, monto, id, categoria, PaymentSource.Manual, fecha(dia = dia, mes = mes, anio = anio), tipo = tipo, estado = estado, categoriaPropia = propia)

    private val delDiseno = listOf(
        // Ingresos de cada mes
        mov("i-jul", 2800.0, 7, TipoMovimiento.Ingreso, categoria = Categoria.Sueldo),
        mov("i-ago", 2800.0, 8, TipoMovimiento.Ingreso, categoria = Categoria.Sueldo),
        mov("i-sep", 2800.0, 9, TipoMovimiento.Ingreso, categoria = Categoria.Sueldo),
        mov("i-oct", 2800.0, 10, TipoMovimiento.Ingreso, categoria = Categoria.Sueldo),
        // Gastos de cada mes
        mov("g-jul", 621.0, 7), mov("g-ago", 906.0, 8), mov("g-sep", 925.4, 9),
        mov("g-oct-1", 89.2, 10, categoria = Categoria.Compras),
        mov("g-oct-2", 25.0, 10, propia = "c1"),
        mov("g-oct-3", 18.5, 10, categoria = Categoria.Alimentacion),
        mov("g-oct-4", 12.0, 10, categoria = Categoria.Transporte),
    )

    private fun reporte(
        seleccion: SeleccionDeReporte = SeleccionDeReporte.delMesDe(hoy),
        movs: List<Movimiento> = delDiseno,
    ) = calcularReporte(movs, seleccion, propias, ahora, utc)

    // Selección y períodos

    @Test
    fun empiezaEnElMesActual() {
        val s = SeleccionDeReporte.delMesDe(hoy)
        assertEquals(ModoDeReporte.Mes, s.modo)
        assertEquals(2026, s.anio)
        assertEquals(10, s.mes)
    }

    @Test
    fun desplazarElMesCruzaElAnio() {
        val enero = SeleccionDeReporte.delMesDe(LocalDate(2026, 1, 15))
        assertEquals(12, enero.desplazar(-1).mes)
        assertEquals(2025, enero.desplazar(-1).anio)
        assertEquals(1, SeleccionDeReporte.delMesDe(LocalDate(2025, 12, 1)).desplazar(1).mes)
        assertEquals(2026, SeleccionDeReporte.delMesDe(LocalDate(2025, 12, 1)).desplazar(1).anio)
    }

    @Test
    fun desplazarElAnio() {
        val s = SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio)
        assertEquals(2025, s.desplazar(-1).anio)
        assertEquals(2027, s.desplazar(1).anio)
    }

    @Test
    fun enRangoNoSeDesplaza() {
        val s = SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 9, 21), LocalDate(2026, 10, 21))
        assertEquals(s, s.desplazar(1))
    }

    @Test
    fun soloSePuedeAvanzarSiElMesSiguienteYaEmpezo() {
        assertFalse(SeleccionDeReporte.delMesDe(hoy).puedeAvanzar(hoy))
        assertTrue(SeleccionDeReporte.delMesDe(hoy).desplazar(-1).puedeAvanzar(hoy))
        assertFalse(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio).puedeAvanzar(hoy))
        assertTrue(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio).desplazar(-1).puedeAvanzar(hoy))
        assertFalse(SeleccionDeReporte.delMesDe(hoy).conRango(hoy, hoy).puedeAvanzar(hoy))
    }

    // Saldo y comparación

    @Test
    fun octubreDejaLoQueDiceElDiseno() {
        val r = reporte()
        assertEquals(2800.0, r.ingresos, 0.001)
        assertEquals(144.7, r.gastos, 0.001)
        assertEquals(2655.3, r.saldo, 0.001)
        assertEquals("En octubre te quedan", r.textoDelSaldo)
        assertEquals("Octubre 2026", r.etiquetaDelPeriodo)
        assertEquals("Octubre", r.nombreDelPeriodo)
    }

    @Test
    fun laComparacionConElMesAnterior() {
        val c = assertNotNullComparacion(reporte())
        assertEquals(780.7, c.diferencia, 0.001)
        assertEquals("septiembre", c.anterior)
        assertEquals("S/ 780.70 más que en septiembre", c.texto())
        assertEquals("S/ •••• más que en septiembre", c.texto("S/ ••••"))
    }

    private fun assertNotNullComparacion(r: Reporte): ComparacionDeReporte =
        r.comparacion ?: error("debía haber comparación")

    @Test
    fun siEsMenosDiceMenos() {
        val c = assertNotNullComparacion(reporte(SeleccionDeReporte.delMesDe(hoy).desplazar(-1)))
        // Septiembre (1,874.60) frente a agosto (1,894.00).
        assertEquals(-19.4, c.diferencia, 0.001)
        assertEquals("S/ 19.40 menos que en agosto", c.texto())
    }

    @Test
    fun siEsIgualDiceIgual() {
        val movs = listOf(mov("a", 100.0, 9, TipoMovimiento.Ingreso), mov("b", 100.0, 10, TipoMovimiento.Ingreso))
        assertEquals("Igual que en septiembre", assertNotNullComparacion(reporte(movs = movs)).texto())
    }

    @Test
    fun sinMovimientosEnElPeriodoAnteriorNoHayComparacion() {
        val soloOctubre = delDiseno.filter { it.date.toString().startsWith("2026-10") }
        assertNull(reporte(movs = soloOctubre).comparacion)
    }

    @Test
    fun soloCuentaLoConfirmado() {
        val r = reporte(movs = delDiseno + mov("p", 500.0, 10, estado = EstadoMovimiento.Pendiente))
        assertEquals(144.7, r.gastos, 0.001)
    }

    @Test
    fun unSaldoNegativoSeMarca() {
        val r = reporte(movs = listOf(mov("g", 100.0, 10)))
        assertEquals(-100.0, r.saldo, 0.001)
        assertTrue(r.saldoNegativo)
        assertFalse(reporte().saldoNegativo)
    }

    @Test
    fun losTextosDelSaldoSegunElMomento() {
        assertEquals("En septiembre te quedaron", reporte(SeleccionDeReporte.delMesDe(hoy).desplazar(-1)).textoDelSaldo)
        assertEquals("En 2026 te quedan", reporte(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio)).textoDelSaldo)
        assertEquals("En 2025 te quedaron", reporte(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio).desplazar(-1)).textoDelSaldo)
        assertEquals("En este rango te quedaron", reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 9, 1), hoy)).textoDelSaldo)
    }

    // Barras por mes

    @Test
    fun enModoMesLasBarrasSonLosCuatroUltimosMeses() {
        val r = reporte()
        assertEquals(listOf("Jul", "Ago", "Set", "Oct"), r.cubos.map { it.etiqueta })
        assertEquals(listOf(2800.0, 2800.0, 2800.0, 2800.0), r.cubos.map { it.ingresos })
        assertEquals(listOf(621.0, 906.0, 925.4, 144.7), r.cubos.map { it.gastos })
        assertEquals(listOf(false, false, false, true), r.cubos.map { it.esUltimo })
    }

    @Test
    fun enModoAnioLasBarrasSonLos12Meses() {
        val r = reporte(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio))
        assertEquals(12, r.cubos.size)
        assertEquals("Ene", r.cubos.first().etiqueta)
        assertEquals("Dic", r.cubos.last().etiqueta)
        assertEquals(144.7, r.cubos[9].gastos, 0.001)
    }

    @Test
    fun enModoRangoLasBarrasSonLosMesesDelRangoConUnMaximoDe12() {
        val corto = reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 8, 20), LocalDate(2026, 10, 5)))
        assertEquals(listOf("Ago", "Set", "Oct"), corto.cubos.map { it.etiqueta })
        val largo = reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2024, 1, 1), LocalDate(2026, 10, 5)))
        assertEquals(12, largo.cubos.size)
    }

    @Test
    fun elEjeVerticalLlegaAlSiguienteMilYNuncaMenosDe1000() {
        assertEquals(3000.0, reporte().topDelEje)
        assertEquals(1000.0, reporte(movs = emptyList()).topDelEje)
        assertEquals(3000.0, reporte(movs = listOf(mov("i", 2000.5, 10, TipoMovimiento.Ingreso))).topDelEje)
        assertEquals(2000.0, reporte(movs = listOf(mov("i", 2000.0, 10, TipoMovimiento.Ingreso))).topDelEje)
    }

    @Test
    fun laDescripcionDelGraficoParaLectoresDePantalla() {
        val d = reporte().descripcionDelGrafico
        assertTrue(d.startsWith("Ingresos y gastos por mes. Jul: 2,800.00 de ingresos y 621.00 de gastos."))
        assertTrue(d.endsWith("Oct: 2,800.00 de ingresos y 144.70 de gastos soles."))
    }

    // Tablas por categoría

    @Test
    fun losGastosDeOctubrePorCategoriaDeMayorAMenor() {
        val r = reporte()
        assertEquals("Gastos de octubre", r.tituloDeGastos)
        assertEquals(listOf("Compras", "Barbería", "Alimentación", "Transporte"), r.gastosPorCategoria.map { it.etiqueta })
        assertEquals(listOf(89.2, 25.0, 18.5, 12.0), r.gastosPorCategoria.map { it.monto })
    }

    @Test
    fun losIngresosPorCategoriaYLaEtiquetaDeTransferencias() {
        val r = reporte(movs = delDiseno + mov("t", 50.0, 10, TipoMovimiento.Ingreso, categoria = Categoria.TransferenciaRecibida))
        assertEquals("Ingresos de octubre", r.tituloDeIngresos)
        assertEquals(listOf("Sueldo", "Transferencias recibidas"), r.ingresosPorCategoria.map { it.etiqueta })
    }

    @Test
    fun sinMovimientosLasTablasEstanVaciasConSuTexto() {
        val r = reporte(movs = emptyList())
        assertEquals(emptyList(), r.gastosPorCategoria)
        assertEquals("Sin gastos en este período.", r.textoSinGastos)
        assertEquals("Sin ingresos en este período.", r.textoSinIngresos)
    }

    @Test
    fun losTitulosSegunElModo() {
        assertEquals("Gastos de 2026", reporte(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio)).tituloDeGastos)
        assertEquals("Ingresos de este rango", reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 9, 1), hoy)).tituloDeIngresos)
    }

    @Test
    fun unaCategoriaPropiaBorradaSeAgrupaComoOtros() {
        val r = calcularReporte(delDiseno, SeleccionDeReporte.delMesDe(hoy), emptyList(), ahora, utc)
        assertEquals(listOf("Compras", "Alimentación", "Otros", "Transporte").sorted(), r.gastosPorCategoria.map { it.etiqueta }.sorted())
    }

    // Período

    @Test
    fun laEtiquetaDelPeriodo() {
        assertEquals("2026", reporte(SeleccionDeReporte.delMesDe(hoy).conModo(ModoDeReporte.Anio)).etiquetaDelPeriodo)
        assertEquals("20 ago – 5 oct", reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 8, 20), LocalDate(2026, 10, 5))).etiquetaDelPeriodo)
        assertEquals("Septiembre 2026", reporte(SeleccionDeReporte.delMesDe(hoy).desplazar(-1)).etiquetaDelPeriodo)
    }

    @Test
    fun enRangoLosExtremosEstanIncluidos() {
        val movs = listOf(mov("a", 10.0, 9, dia = 20), mov("b", 20.0, 10, dia = 5), mov("c", 40.0, 10, dia = 6), mov("d", 80.0, 9, dia = 19))
        val r = reporte(SeleccionDeReporte.delMesDe(hoy).conRango(LocalDate(2026, 9, 20), LocalDate(2026, 10, 5)), movs)
        assertEquals(30.0, r.gastos, 0.001)
    }
}
