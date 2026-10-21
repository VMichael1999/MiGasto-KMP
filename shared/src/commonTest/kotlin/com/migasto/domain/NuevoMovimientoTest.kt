package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Port de la lógica de lib/presentation/expenses/manual_entry_screen.dart (teclado, tipo, categoría,
// «Guardar») y de new_category_sheet.dart. En Flutter vivía en el estado de la pantalla; aquí es de
// `shared` para que Android e iPhone se comporten igual.
class NuevoMovimientoTest {
    private fun teclear(vararg teclas: Char): EntradaDeMonto = teclas.fold(EntradaDeMonto()) { e, t -> e.presionar(t) }

    // Teclado

    @Test
    fun alInicioMuestraCeroConDosDecimales() {
        assertEquals("0.00", EntradaDeMonto().texto)
        assertEquals(0.0, EntradaDeMonto().monto)
    }

    @Test
    fun los180SeEscribenYSeMuestranConDecimales() {
        val e = teclear('1', '8', '0')
        assertEquals(180.0, e.monto)
        assertEquals("180.00", e.texto)
    }

    @Test
    fun elPuntoEnVacioEmpiezaEn0Punto() {
        val e = teclear('.')
        assertEquals("0.", e.texto)
        assertEquals(0.0, e.monto)
    }

    @Test
    fun soloSePermiteUnPunto() = assertEquals("1.5", teclear('1', '.', '5', '.').texto)

    @Test
    fun maximoDosDecimales() {
        val e = teclear('2', '5', '.', '5', '0', '9')
        assertEquals("25.50", e.texto)
        assertEquals(25.5, e.monto)
    }

    @Test
    fun hastaSieteDigitosEnteros() {
        assertEquals("1,234,567.00", teclear('1', '2', '3', '4', '5', '6', '7', '8', '9').texto)
        assertEquals(1234567.0, teclear('1', '2', '3', '4', '5', '6', '7', '8').monto)
    }

    @Test
    fun unCeroAlInicioSeReemplazaPorElSiguienteDigito() {
        assertEquals(5.0, teclear('0', '5').monto)
        assertEquals("0.00", teclear('0').texto)
    }

    @Test
    fun borrarQuitaElUltimoCaracterYNoFallaEnVacio() {
        assertEquals("12", teclear('1', '2', '3', '<').crudo)
        assertEquals("", teclear('<').crudo)
        assertEquals("1", teclear('1', '.', '<').crudo)
    }

    @Test
    fun unPuntoAlFinalCuentaComoCero() = assertEquals(12.0, teclear('1', '2', '.').monto)

    @Test
    fun mientrasSeEscribenDecimalesSeMuestraTalCual() = assertEquals("12.5", teclear('1', '2', '.', '5').texto)

    @Test
    fun teclasQueNoSonDelTecladoSeIgnoran() = assertEquals("1", teclear('1', 'x', '-', ' ').crudo)

    // Formulario

    private fun conMontoYConcepto(f: FormularioNuevoMovimiento = FormularioNuevoMovimiento()) =
        f.conEntrada(teclear('2', '5')).conConcepto("Corte de cabello")

    @Test
    fun empiezaComoUnGastoEnEfectivoSinCategoriaElegida() {
        val f = FormularioNuevoMovimiento()
        assertEquals(TipoMovimiento.Gasto, f.tipo)
        assertEquals(PaymentSource.Efectivo, f.fuente)
        assertEquals(Categoria.Otros, f.categoria)
        assertNull(f.categoriaPropia)
    }

    @Test
    fun sinMontoNiDetalleNoSePuedeGuardar() {
        assertFalse(FormularioNuevoMovimiento().puedeGuardar)
        assertFalse(FormularioNuevoMovimiento().conEntrada(teclear('5')).puedeGuardar)
        assertFalse(FormularioNuevoMovimiento().conConcepto("Menú").puedeGuardar)
        assertFalse(FormularioNuevoMovimiento().conEntrada(teclear('5')).conConcepto("   ").puedeGuardar)
        assertFalse(FormularioNuevoMovimiento().conEntrada(teclear('0', '.')).conConcepto("x").puedeGuardar)
        assertTrue(conMontoYConcepto().puedeGuardar)
    }

    @Test
    fun cambiarDeTipoReiniciaLaCategoria() {
        val f = FormularioNuevoMovimiento().conCategoria(Categoria.Compras).conTipo(TipoMovimiento.Ingreso)
        assertEquals(Categoria.OtrosIngresos, f.categoria)
        assertEquals(Categoria.Otros, f.conTipo(TipoMovimiento.Gasto).categoria)
        assertNull(f.categoriaPropia)
    }

    @Test
    fun elegirUnaPropiaGuardaSuCategoriaBase() {
        val gym = CategoriaPropia("c1", "Gym", "gimnasio", TipoMovimiento.Gasto)
        val f = FormularioNuevoMovimiento().conPropia(gym)
        assertEquals(Categoria.Otros, f.categoria)
        assertEquals("c1", f.categoriaPropia)
        // Elegir una de siempre quita la propia.
        val g = f.conCategoria(Categoria.Compras)
        assertEquals(Categoria.Compras, g.categoria)
        assertNull(g.categoriaPropia)
        // Una propia de ingreso usa la base de ingresos.
        val venta = FormularioNuevoMovimiento().conTipo(TipoMovimiento.Ingreso).conPropia(gym.copy(tipo = TipoMovimiento.Ingreso))
        assertEquals(Categoria.OtrosIngresos, venta.categoria)
    }

    @Test
    fun textoDelBotonSegunElTipoYElMonto() {
        assertEquals("Guardar gasto · S/ 25.00", conMontoYConcepto().textoDelBoton)
        assertEquals("Guardar ingreso · S/ 180.00", FormularioNuevoMovimiento().conTipo(TipoMovimiento.Ingreso).conEntrada(teclear('1', '8', '0')).textoDelBoton)
        assertEquals("Guardar gasto", FormularioNuevoMovimiento().textoDelBoton)
    }

    @Test
    fun textosQueDependenDelTipo() {
        val gasto = FormularioNuevoMovimiento()
        val ingreso = gasto.conTipo(TipoMovimiento.Ingreso)
        assertEquals("Comercio o detalle", gasto.pistaDelConcepto)
        assertEquals("De quién o por qué", ingreso.pistaDelConcepto)
        assertEquals("Pagado con", gasto.prefijoDeLaFuente)
        assertEquals("Recibido por", ingreso.prefijoDeLaFuente)
        assertEquals("Ingreso guardado", ingreso.mensajeAlGuardar)
        assertEquals("Gasto guardado", gasto.mensajeAlGuardar)
        assertEquals("+ S/ ", ingreso.prefijoDelMonto)
        assertEquals("S/ ", gasto.prefijoDelMonto)
    }

    @Test
    fun lasFuentesQueSePuedenElegir() {
        assertEquals(
            listOf(PaymentSource.Efectivo, PaymentSource.Yape, PaymentSource.Plin, PaymentSource.Tarjeta, PaymentSource.Otro),
            FormularioNuevoMovimiento.fuentesPosibles,
        )
        assertEquals("Otro", FormularioNuevoMovimiento.etiquetaDeLaFuente(PaymentSource.Otro))
        assertEquals("Efectivo", FormularioNuevoMovimiento.etiquetaDeLaFuente(PaymentSource.Efectivo))
    }

    @Test
    fun elConceptoSeRecortaAlGuardar() {
        assertEquals("Corte de cabello", conMontoYConcepto().conConcepto("  Corte de cabello ").conceptoLimpio)
    }

    // Categoría nueva

    @Test
    fun elNombreDeUnaCategoriaNuevaTieneUnMaximoDe20Letras() {
        assertEquals(20, "a".repeat(30).let { limitarNombreDeCategoria(it) }.length)
        assertEquals("Barbería", limitarNombreDeCategoria("Barbería"))
    }

    @Test
    fun hayMuchosIconosSinRepetirYIncluyenLosDelDiseno() {
        assertTrue(clavesDeIconosPropios.size >= 50)
        assertEquals(clavesDeIconosPropios.size, clavesDeIconosPropios.toSet().size)
        assertTrue("scissors" in clavesDeIconosPropios)
        assertTrue("gimnasio" in clavesDeIconosPropios)
        assertTrue("restaurante" in clavesDeIconosPropios)
    }
}
