package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nuevo: en Android este armado vivía en MovimientosViewModel; ahora sale de `shared` para que Android y iPhone
// muestren lo mismo. Los datos son los del diseño (HTML): siete movimientos del jueves 1 de octubre, S/ 144.70 en gastos.
class PantallaDeMovimientosTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 1, hora = 12, minuto = 0, mes = 10)
    private val propias = listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto))

    private fun mov(
        id: String,
        comercio: String,
        monto: Double,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        fuente: PaymentSource = PaymentSource.Efectivo,
        dia: Int = 1,
        mes: Int = 10,
        hora: Int = 10,
        minuto: Int = 0,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        propia: String? = null,
    ) = Movimiento(
        id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = fuente,
        date = fecha(dia = dia, hora = hora, minuto = minuto, mes = mes), tipo = tipo, estado = estado, categoriaPropia = propia,
    )

    private val delDiseno = listOf(
        mov("sueldo", "Sueldo octubre", 2800.0, TipoMovimiento.Ingreso, hora = 7, minuto = 55),
        mov("cafe", "Café Central", 18.5, fuente = PaymentSource.Yape, hora = 8, minuto = 20),
        mov("taxi", "Taxi", 12.0, fuente = PaymentSource.Yape, hora = 9, minuto = 5),
        mov("minimarket", "Minimarket La Esquina", 89.2, fuente = PaymentSource.GooglePay, hora = 10, minuto = 20),
        mov("barberia", "Barbería El Patrón", 25.0, hora = 11, propia = "c1"),
        mov("ana", "Ana", 150.0, TipoMovimiento.Ingreso, PaymentSource.Yape, hora = 11, minuto = 25, estado = EstadoMovimiento.Pendiente),
        mov("carlos", "Carlos", 80.0, TipoMovimiento.Ingreso, PaymentSource.Plin, hora = 11, minuto = 40, estado = EstadoMovimiento.Pendiente),
    )

    private fun pantalla(
        movs: List<Movimiento> = delDiseno,
        filtros: FiltrosDeMovimientos = filtrosVacios(),
        alias: Map<String, String> = emptyMap(),
    ) = armarPantallaDeMovimientos(movs, filtros, alias, propias, ahora, utc)

    @Test
    fun losSieteMovimientosDelDisenoVanEnUnGrupoDeHoy() {
        val p = pantalla()
        assertEquals(1, p.grupos.size)
        val g = p.grupos.single()
        assertEquals("Hoy, jueves 1", g.etiqueta)
        assertEquals("S/ 144.70 en gastos", g.totalDeGastos)
        assertEquals(listOf("carlos", "ana", "barberia", "minimarket", "taxi", "cafe", "sueldo"), g.filas.map { it.id })
    }

    @Test
    fun cadaFilaTraeSusTextosYaArmados() {
        val carlos = pantalla().grupos.single().filas.first()
        assertEquals("Plin · 11:40", carlos.fuenteYHora)
        assertEquals("+ S/ 80.00", carlos.monto)
        assertEquals(true, carlos.porConfirmar)
        assertEquals("scissors", pantalla().grupos.single().filas.first { it.id == "barberia" }.iconoPropio)
    }

    @Test
    fun cuentaLosQueEstanPorConfirmarSinImportarElFiltro() {
        assertEquals(2, pantalla().cantidadPorConfirmar)
        assertEquals(2, pantalla(filtros = filtrosVacios().conTipo(FiltroDeTipo.Gastos)).cantidadPorConfirmar)
    }

    @Test
    fun cadaDiaTieneSuEtiquetaYSoloMuestraGastosSiLosHubo() {
        val ayer = mov("ingreso-ayer", "Venta", 50.0, TipoMovimiento.Ingreso, dia = 30, mes = 9)
        val antes = mov("gasto-antes", "Wong", 40.0, dia = 15, mes = 9)
        val p = pantalla(movs = delDiseno + ayer + antes)
        assertEquals(listOf("Hoy, jueves 1", "Ayer, miércoles 30", "Martes 15 de septiembre"), p.grupos.map { it.etiqueta })
        assertNull(p.grupos[1].totalDeGastos)
        assertEquals("S/ 40.00 en gastos", p.grupos[2].totalDeGastos)
    }

    @Test
    fun elTotalDelDiaNoCuentaLosGastosSinConfirmar() {
        val pendiente = mov("pendiente", "Aviso", 500.0, estado = EstadoMovimiento.Pendiente)
        assertEquals("S/ 144.70 en gastos", pantalla(movs = delDiseno + pendiente).grupos.single().totalDeGastos)
    }

    @Test
    fun elFiltroPorTipoDejaSoloLoDeEseTipo() {
        assertEquals(4, pantalla(filtros = filtrosVacios().conTipo(FiltroDeTipo.Gastos)).grupos.single().filas.size)
        assertEquals(3, pantalla(filtros = filtrosVacios().conTipo(FiltroDeTipo.Ingresos)).grupos.single().filas.size)
        assertEquals(listOf("carlos", "ana"), pantalla(filtros = filtrosVacios().conTipo(FiltroDeTipo.PorConfirmar)).grupos.single().filas.map { it.id })
    }

    @Test
    fun laBusquedaTambienEncuentraPorElNombreQueElUsuarioPuso() {
        val alias = mapOf(claveDeAlias("Barbería El Patrón") to "La barbe")
        assertEquals(listOf("barberia"), pantalla(filtros = filtrosVacios().conBusqueda("barbe"), alias = alias).grupos.single().filas.map { it.id })
        assertEquals(listOf("cafe"), pantalla(filtros = filtrosVacios().conBusqueda("  CAFÉ ")).grupos.single().filas.map { it.id })
    }

    @Test
    fun sinResultadosNoHayGruposYHayFiltro() {
        val p = pantalla(filtros = filtrosVacios().conBusqueda("nada de esto"))
        assertEquals(emptyList(), p.grupos)
        assertTrue(p.hayFiltro)
        assertEquals(false, pantalla().hayFiltro)
    }

    @Test
    fun laHojaDeFiltrosAplicaCategoriaFuenteYRangoDesdeSwift() {
        val f = filtrosVacios().conLaHoja("compras", PaymentSource.GooglePay, 40.0, 120.0)
        assertEquals(3, f.cantidadDeFiltrosActivos)
        assertEquals(40.0, f.desdeDeLaHoja)
        assertEquals(120.0, f.hastaDeLaHoja)
        assertTrue(f.hayRangoDeLaHoja)
        val sin = f.sinLoDeLaHoja()
        assertEquals(0, sin.cantidadDeFiltrosActivos)
        assertEquals(OpcionesDeFiltro.MONTO_MINIMO, sin.desdeDeLaHoja)
        assertEquals(OpcionesDeFiltro.MONTO_MAXIMO, sin.hastaDeLaHoja)
        assertEquals(false, sin.hayRangoDeLaHoja)
    }

    @Test
    fun laHojaNoCambiaElTipoNiLaBusqueda() {
        val f = filtrosVacios().conTipo(FiltroDeTipo.Gastos).conBusqueda("taxi").conLaHoja(null, PaymentSource.Yape, null, null)
        assertEquals(FiltroDeTipo.Gastos, f.tipo)
        assertEquals("taxi", f.busqueda)
        assertEquals(PaymentSource.Yape, f.fuente)
    }

    @Test
    fun losChipsDeTipoTraenSuTextoConLaCuentaDeLosPorConfirmar() {
        val opciones = pantalla().opcionesDeTipo
        assertEquals(listOf("Todo", "Gastos", "Ingresos", "Por confirmar · 2"), opciones.map { it.texto })
        assertEquals(listOf(FiltroDeTipo.Todo, FiltroDeTipo.Gastos, FiltroDeTipo.Ingresos, FiltroDeTipo.PorConfirmar), opciones.map { it.tipo })
        assertEquals(listOf(false, false, false, true), opciones.map { it.esPorConfirmar })
    }

    @Test
    fun siNoHayMovimientosElAvisoDependeDeSiHayFiltros() {
        assertEquals("Todavía no hay movimientos.", pantalla(movs = emptyList()).textoSinMovimientos)
        assertEquals("No hay movimientos con esos filtros.", pantalla(filtros = filtrosVacios().conBusqueda("zzz")).textoSinMovimientos)
    }

    @Test
    fun losTextosFijosDeLaPantallaYDeLaHoja() {
        assertEquals("Movimientos", TextosDeMovimientos.TITULO)
        assertEquals("Filtrar movimientos", TextosDeMovimientos.FILTRAR)
        assertEquals("Quitar filtros", TextosDeMovimientos.QUITAR_FILTROS)
        assertEquals("Limpiar filtros", TextosDeMovimientos.LIMPIAR_FILTROS)
        assertEquals("Aplicar", TextosDeMovimientos.APLICAR)
        assertEquals("Categoría", TextosDeMovimientos.CATEGORIA)
        assertEquals("Fuente", TextosDeMovimientos.FUENTE)
        assertEquals("Rango de monto", TextosDeMovimientos.RANGO)
        assertEquals("Todas", TextosDeMovimientos.TODAS)
        assertEquals("3 filtros puestos", TextosDeMovimientos.filtrosPuestos(3))
    }

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-pantalla-movimientos.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun observarArmaLaPantallaConLoQueHayEnLaBase() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            delDiseno.forEach(base.movimientos::guardar)
            val p = observarPantallaDeMovimientos(base, filtrosVacios().conTipo(FiltroDeTipo.Gastos), { ahora }, utc).first()
            assertEquals(4, p.grupos.single().filas.size)
            assertEquals(2, p.cantidadPorConfirmar)
        }
    }
}
