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

// Nuevo: en Android este armado vivía en ResumenViewModel; ahora sale de `shared` para que Android y iPhone muestren
// exactamente lo mismo. Los datos son los del diseño (HTML): quedan S/ 2,655.30, por confirmar S/ 230.00 y un
// presupuesto de S/ 1,500 con el 10 % usado.
class PantallaDelResumenTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 12, minuto = 0, mes = 10)
    private val propias = listOf(CategoriaPropia("c1", "Barbería", "scissors", TipoMovimiento.Gasto))

    private fun mov(
        id: String,
        comercio: String,
        monto: Double,
        tipo: TipoMovimiento = TipoMovimiento.Gasto,
        fuente: PaymentSource = PaymentSource.Efectivo,
        dia: Int = 21,
        hora: Int = 10,
        minuto: Int = 0,
        mes: Int = 10,
        estado: EstadoMovimiento = EstadoMovimiento.Confirmado,
        propia: String? = null,
    ) = Movimiento(
        id = id, amount = monto, merchant = comercio, category = Categoria.Compras, source = fuente,
        date = fecha(dia = dia, hora = hora, minuto = minuto, mes = mes), tipo = tipo, estado = estado, categoriaPropia = propia,
    )

    private val delDiseno = listOf(
        mov("carlos", "Carlos", 80.0, TipoMovimiento.Ingreso, PaymentSource.Plin, hora = 11, minuto = 40, estado = EstadoMovimiento.Pendiente),
        mov("ana", "Ana", 150.0, TipoMovimiento.Ingreso, PaymentSource.Yape, hora = 11, minuto = 25, estado = EstadoMovimiento.Pendiente),
        mov("barberia", "Barbería El Patrón", 25.0, fuente = PaymentSource.Efectivo, hora = 11, propia = "c1"),
        mov("minimarket", "Minimarket La Esquina", 89.2, fuente = PaymentSource.GooglePay, hora = 10, minuto = 20),
        mov("taxi", "Taxi", 12.0, fuente = PaymentSource.Yape, dia = 20, hora = 9, minuto = 5),
        mov("cafe", "Café Central", 18.5, fuente = PaymentSource.Yape, dia = 20, hora = 8, minuto = 20),
        mov("sueldo", "Sueldo octubre", 2800.0, TipoMovimiento.Ingreso, dia = 20, hora = 7, minuto = 55),
    )

    private fun pantalla(
        movs: List<Movimiento> = delDiseno,
        presupuesto: Double = 1500.0,
        alias: Map<String, String> = emptyMap(),
        mes: MesElegido? = null,
    ) = armarPantallaDelResumen(movs, presupuesto, alias, propias, ahora, utc, mes)

    @Test
    fun losMontosDelDisenoCuadran() {
        val p = pantalla()
        assertEquals("Octubre", p.mes)
        assertEquals("2,655.30", p.saldo)
        assertEquals("S/ 2,800.00", p.ingresos)
        assertEquals("S/ 144.70", p.gastos)
        assertEquals("+ S/ 230.00", p.porConfirmarMonto)
        assertEquals("Te quedan de este mes", p.textoDelSaldo)
    }

    @Test
    fun elPresupuestoDiceCuantoSeUso() {
        val b = pantalla().presupuesto!!
        assertEquals(EstadoDelPresupuesto.Bien, b.estado)
        assertEquals("Gastos: vas bien", b.titulo)
        assertEquals("10 % de S/ 1,500", b.detalle)
        assertEquals(0.0965f, b.relleno, 0.001f)
    }

    @Test
    fun sinPresupuestoNoHayBarra() {
        assertNull(pantalla(presupuesto = 0.0).presupuesto)
    }

    @Test
    fun laBarraNuncaPasaDelCien() {
        assertEquals(1f, pantalla(presupuesto = 100.0).presupuesto!!.relleno)
    }

    @Test
    fun lasFilasDeHoyVanDeLaMasNuevaALaMasVieja() {
        val hoy = pantalla().hoy
        assertEquals(listOf("carlos", "ana", "barberia", "minimarket"), hoy.map { it.id })
        assertEquals("Hoy", pantalla().tituloDeLaLista)
    }

    @Test
    fun cadaFilaTraeSuTextoYaArmado() {
        val carlos = pantalla().hoy.first()
        assertEquals("Carlos", carlos.nombre)
        assertEquals("Plin · 11:40", carlos.fuenteYHora)
        assertEquals("+ S/ 80.00", carlos.monto)
        assertEquals(true, carlos.esIngreso)
        assertEquals(true, carlos.porConfirmar)
        val minimarket = pantalla().hoy.last()
        assertEquals("S/ 89.20", minimarket.monto)
        assertEquals(false, minimarket.porConfirmar)
    }

    @Test
    fun laFilaUsaElIconoPropioYElNombreQueElUsuarioPuso() {
        val barberia = pantalla(alias = mapOf(claveDeAlias("Barbería El Patrón") to "La barbería")).hoy.first { it.id == "barberia" }
        assertEquals("scissors", barberia.iconoPropio)
        assertEquals("La barbería", barberia.nombre)
    }

    @Test
    fun loPorConfirmarDiceCuantosHay() {
        assertEquals("2 ingresos", pantalla().porConfirmarTexto)
        assertNull(pantalla(movs = delDiseno.filter { it.estado == EstadoMovimiento.Confirmado }).porConfirmarTexto)
    }

    @Test
    fun sinMovimientosHoyDiceQueNoHay() {
        val p = pantalla(movs = delDiseno.filter { it.date < fecha(dia = 21, mes = 10, hora = 0, minuto = 0) })
        assertEquals(emptyList(), p.hoy)
        assertEquals("Todavía no hay movimientos hoy.", p.textoSinMovimientos)
    }

    @Test
    fun enUnMesPasadoSeVeSuResumenYSusUltimosCincoMovimientos() {
        val septiembre = (1..7).map { mov("s$it", "Compra $it", 10.0, dia = it, mes = 9) } + mov("sueldo-set", "Sueldo", 2500.0, TipoMovimiento.Ingreso, dia = 5, mes = 9)
        val p = pantalla(movs = delDiseno + septiembre, mes = MesElegido(2026, 9))
        assertEquals("Septiembre", p.mes)
        assertEquals("Te quedaron en septiembre", p.textoDelSaldo)
        assertEquals("Últimos de septiembre", p.tituloDeLaLista)
        assertEquals("S/ 2,500.00", p.ingresos)
        assertEquals("S/ 70.00", p.gastos)
        assertEquals(5, p.hoy.size)
        assertEquals("s7", p.hoy.first().id)
    }

    @Test
    fun elMenuDeMesesTraeLosMesesConMovimientos() {
        val p = pantalla(movs = delDiseno + mov("agosto", "Algo", 5.0, mes = 8))
        assertEquals(listOf("Octubre", "Agosto"), p.meses.map { it.nombre })
        assertEquals(MesElegido(2026, 10), p.mesElegido)
    }

    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-pantalla-resumen.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    @Test
    fun observarArmaLaPantallaConLoQueHayEnLaBase() = runTest {
        BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba").use { base ->
            base.ajustes.guardarPresupuesto(1500.0)
            delDiseno.forEach(base.movimientos::guardar)
            val p = observarPantallaDelResumen(base, null, { ahora }, utc).first()
            assertEquals("2,655.30", p.saldo)
            assertEquals("10 % de S/ 1,500", p.presupuesto!!.detalle)
            assertEquals(4, p.hoy.size)
        }
    }
}
