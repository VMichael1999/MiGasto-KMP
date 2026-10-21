package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Port de la lógica de android/app/src/main/.../OverlayService.kt (la ventana flotante «Pago detectado»):
// cuenta regresiva de 4 s en gastos, ingresos que esperan, qué hace cada botón y qué se guarda. La vista
// (Compose) solo dibuja y llama a estas reglas.
class VentanaDePagoTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-ventana.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun gasto() = PagoLeido(32.0, "Café Central", PaymentSource.Yape, esIngreso = false)
    private fun ingreso() = PagoLeido(15.0, "Juan Pérez", PaymentSource.Yape, esIngreso = true)

    private fun sesion(pago: PagoLeido = gasto(), guardarSolo: Boolean = !pago.esIngreso) =
        SesionDeVentana(pago, "texto", Categoria.Alimentacion, guardarSolo)

    // Cuenta regresiva

    @Test
    fun laCuentaEmpiezaLlenaYDura4Segundos() {
        val s = sesion()
        assertEquals(1f, s.progreso)
        assertEquals(4_000L, SesionDeVentana.CUENTA_REGRESIVA_MS)
        assertTrue(s.tieneCuentaRegresiva)
    }

    @Test
    fun avanzaDeAPasosYSeVaciaHastaCero() {
        val s = sesion()
        assertNull(s.avanzar())
        assertEquals(0.975f, s.progreso, 0.001f)
        repeat(38) { assertNull(s.avanzar()) }
        assertEquals(0.025f, s.progreso, 0.001f)
        assertEquals(AccionDeVentana.GuardarConfirmado, s.avanzar())
        assertEquals(0f, s.progreso)
    }

    @Test
    fun alTocarLaVentanaLaCuentaSePausaYYaNoAvanza() {
        val s = sesion()
        s.avanzar()
        s.pausar()
        assertTrue(s.pausada)
        assertEquals("En pausa", s.textoDeLaHora("14:49"))
        val antes = s.progreso
        repeat(100) { assertNull(s.avanzar()) }
        assertEquals(antes, s.progreso)
    }

    @Test
    fun sinPausaLaHoraSeMuestra() = assertEquals("14:49", sesion().textoDeLaHora("14:49"))

    @Test
    fun unIngresoNoTieneCuentaRegresivaPorDefecto() {
        val s = sesion(ingreso())
        assertFalse(s.tieneCuentaRegresiva)
        repeat(100) { assertNull(s.avanzar()) }
        assertEquals(1f, s.progreso)
        s.pausar()
        assertFalse(s.pausada) // no hay cuenta que pausar
    }

    @Test
    fun conGuardarIngresosAutomaticamenteUnIngresoSigueLaMismaCuenta() {
        val s = sesion(ingreso(), guardarSolo = true)
        assertTrue(s.tieneCuentaRegresiva)
        repeat(39) { assertNull(s.avanzar()) }
        assertEquals(AccionDeVentana.GuardarConfirmado, s.avanzar())
    }

    @Test
    fun unIngresoSinRespuestaQuedaPendienteDespuesDeUnMinuto() {
        assertEquals(60_000L, SesionDeVentana.ESPERA_DEL_INGRESO_MS)
        assertEquals(AccionDeVentana.DejarPendiente, sesion(ingreso()).alExpirarLaEspera())
    }

    // Botones

    @Test
    fun losBotonesDeUnGasto() {
        val s = sesion()
        assertEquals(listOf("Descartar", "Editar", "Guardar"), s.textos("Café Central", "Alimentación").botones)
        assertEquals(AccionDeVentana.Descartar, s.alPrimerBoton())
        assertEquals(AccionDeVentana.DejarPendienteYAbrirLaApp, s.alEditar())
        assertEquals(AccionDeVentana.GuardarConfirmado, s.alGuardar())
    }

    @Test
    fun losBotonesDeUnIngreso() {
        val s = sesion(ingreso())
        assertEquals(listOf("Ignorar", "Guardar ingreso"), s.textos("Juan Pérez", "Transferencia recibida").botones)
        // Ignorar no lo borra: queda en «Por confirmar».
        assertEquals(AccionDeVentana.DejarPendiente, s.alPrimerBoton())
        assertEquals(AccionDeVentana.GuardarConfirmado, s.alGuardar())
    }

    @Test
    fun deslizarHaceLoMismoQueElPrimerBoton() {
        assertEquals(AccionDeVentana.Descartar, sesion().alDeslizar())
        assertEquals(AccionDeVentana.DejarPendiente, sesion(ingreso()).alDeslizar())
    }

    @Test
    fun siLlegaOtroPagoElQueNoSeResolvioQuedaPendiente() {
        assertEquals(AccionDeVentana.DejarPendiente, sesion().alSerReemplazada())
        assertEquals(AccionDeVentana.DejarPendiente, sesion(ingreso()).alSerReemplazada())
    }

    // Textos

    @Test
    fun losTextosDeUnGasto() {
        val t = sesion().textos("Café Central", "Alimentación")
        assertEquals("S/ ", t.prefijoDelMonto)
        assertEquals("32.00", t.monto)
        assertEquals("32.00 soles", t.montoAccesible)
        assertEquals("a ", t.preposicion)
        assertEquals("Café Central", t.nombre)
        assertEquals("Yape", t.fuente)
        assertEquals("Alimentación", t.categoria)
        assertEquals("Categoría: Alimentación. Cambiar", t.descripcionDeLaCategoria)
        assertFalse(t.esIngreso)
    }

    @Test
    fun losTextosDeUnIngreso() {
        val t = sesion(ingreso()).textos("Juan", "Transferencia recibida")
        assertEquals("+ S/ ", t.prefijoDelMonto)
        assertEquals("de ", t.preposicion)
        assertEquals("Tipo: Transferencia recibida. Cambiar", t.descripcionDeLaCategoria)
        assertTrue(t.esIngreso)
    }

    @Test
    fun losMontosGrandesLlevanSeparadorDeMiles() {
        val t = SesionDeVentana(PagoLeido(1200.5, "x", PaymentSource.Plin, false), "", Categoria.Otros, true).textos("x", "Otros")
        assertEquals("1,200.50", t.monto)
        assertEquals("Plin", t.fuente)
    }

    @Test
    fun elBotonPrincipalAnunciaQueSeGuardaSolo() {
        assertEquals("Guardar. Se guarda solo en unos segundos", sesion().textos("x", "Otros").descripcionDelBotonPrincipal)
        assertEquals("Guardar ingreso", sesion(ingreso()).textos("x", "Otros").descripcionDelBotonPrincipal)
    }

    @Test
    fun unPagoSinMontoNoAbreVentana() {
        assertFalse(debeAbrirVentana(PagoLeido(0.0, "x", PaymentSource.Yape, false)))
        assertTrue(debeAbrirVentana(gasto()))
    }

    // Qué se guarda

    private fun BaseMiGasto.registro() = RegistroDeMovimientos(
        movimientos, ajustes, ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias), { fecha() }, { "id-${movimientos.cantidad() + 1}" },
    )

    @Test
    fun guardarConfirmadoGuardaElPago() {
        abrir().use { base ->
            val m = base.registro().aplicar(AccionDeVentana.GuardarConfirmado, gasto(), "Yapeaste S/ 32.00 a Café Central")
            assertEquals(EstadoMovimiento.Confirmado, base.movimientos.todos().single().estado)
            assertEquals(m?.id, base.movimientos.todos().single().id)
        }
    }

    @Test
    fun dejarPendienteLoGuardaSinConfirmar() {
        abrir().use { base ->
            base.registro().aplicar(AccionDeVentana.DejarPendiente, ingreso(), "Juan Pérez te yapeó S/ 15.00")
            assertEquals(EstadoMovimiento.Pendiente, base.movimientos.todos().single().estado)
            assertEquals(Categoria.TransferenciaRecibida, base.movimientos.todos().single().category)
        }
        abrir().use { base ->
            base.registro().aplicar(AccionDeVentana.DejarPendienteYAbrirLaApp, gasto(), "x")
            assertEquals(2, base.movimientos.cantidad())
            assertEquals(EstadoMovimiento.Pendiente, base.movimientos.todos().last().estado)
        }
    }

    @Test
    fun descartarNoGuardaNada() {
        abrir().use { base ->
            assertNull(base.registro().aplicar(AccionDeVentana.Descartar, gasto(), "x"))
            assertEquals(0, base.movimientos.cantidad())
        }
    }

    @Test
    fun laCategoriaQueSeMuestraEsLaSugerida() {
        abrir().use { base ->
            val registro = base.registro()
            assertEquals(Categoria.Alimentacion, registro.categoriaSugerida(PagoLeido(15.5, "Rappi", PaymentSource.Yape, false), "Yapeaste S/ 15.50 a Rappi"))
            base.ajustes.guardarAprendida("Rappi", Categoria.Entretenimiento)
            assertEquals(Categoria.Entretenimiento, registro.categoriaSugerida(PagoLeido(15.5, "Rappi", PaymentSource.Yape, false), "Yapeaste S/ 15.50 a Rappi"))
            assertEquals(Categoria.TransferenciaRecibida, registro.categoriaSugerida(ingreso(), "Juan Pérez te yapeó S/ 15.00"))
        }
    }

    @Test
    fun siElUsuarioPideGuardarLosIngresosSolosLaVentanaLoSabe() {
        abrir().use { base ->
            val registro = base.registro()
            assertFalse(registro.nuevaSesionDeVentana(ingreso(), "t").tieneCuentaRegresiva)
            base.ajustes.establecerGuardarIngresosAutomaticamente(true)
            assertTrue(registro.nuevaSesionDeVentana(ingreso(), "t").tieneCuentaRegresiva)
            assertTrue(registro.nuevaSesionDeVentana(gasto(), "t").tieneCuentaRegresiva)
        }
    }
}
