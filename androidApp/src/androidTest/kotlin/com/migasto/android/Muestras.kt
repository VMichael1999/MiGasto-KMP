package com.migasto.android

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import com.migasto.android.movimientos.MovimientosUi
import com.migasto.android.resumen.ResumenUi
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.FilaDeMovimiento
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.FiltrosDeMovimientos
import com.migasto.domain.GrupoDeMovimientos
import com.migasto.domain.OpcionDeTipo
import com.migasto.domain.PaymentSource
import com.migasto.domain.PresupuestoDelResumen

/** Los datos del diseño (`docs/diseno/migasto-kmp.html`), ya armados como los entrega `shared` a la vista. */
object Muestras {
    val carlos = FilaDeMovimiento("carlos", "Carlos", PaymentSource.Plin, "Plin · 11:40", "+ S/ 80.00", true, true, Categoria.TransferenciaRecibida, null)
    val ana = FilaDeMovimiento("ana", "Ana", PaymentSource.Yape, "Yape · 11:25", "+ S/ 150.00", true, true, Categoria.TransferenciaRecibida, null)
    val barberia = FilaDeMovimiento("barberia", "Barbería El Patrón", PaymentSource.Efectivo, "Efectivo · 11:00", "S/ 25.00", false, false, Categoria.Otros, "scissors")
    val minimarket = FilaDeMovimiento("minimarket", "Minimarket La Esquina", PaymentSource.GooglePay, "Google Wallet · 10:20", "S/ 89.20", false, false, Categoria.Compras, null)

    fun resumen(oculto: Boolean = false, presupuesto: Boolean = true) = ResumenUi(
        mes = "Octubre",
        saldo = "2,655.30",
        ingresos = "S/ 2,800.00",
        gastos = "S/ 144.70",
        porConfirmarTexto = "2 ingresos",
        porConfirmarMonto = "+ S/ 230.00",
        presupuesto = if (presupuesto) PresupuestoDelResumen(EstadoDelPresupuesto.Bien, "Gastos: vas bien", "10 % de S/ 1,500", 0.1f) else null,
        hoy = listOf(carlos, ana, barberia, minimarket),
        oculto = oculto,
    )

    fun movimientos(
        tipo: FiltroDeTipo = FiltroDeTipo.Todo,
        busqueda: String = "",
        grupos: List<GrupoDeMovimientos> = listOf(
            GrupoDeMovimientos("Hoy, jueves 1", "S/ 114.20 en gastos", listOf(carlos, ana, barberia, minimarket)),
            GrupoDeMovimientos("Ayer, miércoles 30", null, listOf(carlos.copy(id = "otro"))),
        ),
        hayFiltro: Boolean = false,
        oculto: Boolean = false,
        filtros: FiltrosDeMovimientos = FiltrosDeMovimientos(tipo = tipo, busqueda = busqueda),
    ) = MovimientosUi(
        tipo, busqueda, 2, grupos, hayFiltro, oculto, filtros,
        opcionesDeTipo = listOf(
            OpcionDeTipo(FiltroDeTipo.Todo, "Todo", false),
            OpcionDeTipo(FiltroDeTipo.Gastos, "Gastos", false),
            OpcionDeTipo(FiltroDeTipo.Ingresos, "Ingresos", false),
            OpcionDeTipo(FiltroDeTipo.PorConfirmar, "Por confirmar · 2", true),
        ),
        textoSinMovimientos = if (hayFiltro) "No hay movimientos con esos filtros." else "Todavía no hay movimientos.",
    )
}

/**
 * Comprueba que el elemento se ve, haciendo antes scroll hasta él si está en una lista desplazable: en una pantalla
 * chica (como el emulador de la CI) lo de abajo queda fuera de la vista aunque la app se pueda desplazar hasta ahí.
 */
fun SemanticsNodeInteraction.verEnPantalla(): SemanticsNodeInteraction = apply {
    runCatching { performScrollTo() }
    assertIsDisplayed()
}
