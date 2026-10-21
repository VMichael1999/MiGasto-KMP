package com.migasto.android

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import com.migasto.android.movimientos.GrupoUi
import com.migasto.android.movimientos.MovimientosUi
import com.migasto.android.resumen.FilaDeMovimiento
import com.migasto.android.resumen.PresupuestoUi
import com.migasto.android.resumen.ResumenUi
import com.migasto.domain.Categoria
import com.migasto.domain.EstadoDelPresupuesto
import com.migasto.domain.FiltroDeTipo
import com.migasto.domain.FiltrosDeMovimientos
import com.migasto.domain.PaymentSource

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
        presupuesto = if (presupuesto) PresupuestoUi(EstadoDelPresupuesto.Bien, "Gastos: vas bien", "10 % de S/ 1,500", 0.1f) else null,
        hoy = listOf(carlos, ana, barberia, minimarket),
        oculto = oculto,
    )

    fun movimientos(
        tipo: FiltroDeTipo = FiltroDeTipo.Todo,
        busqueda: String = "",
        grupos: List<GrupoUi> = listOf(
            GrupoUi("Hoy, jueves 1", "S/ 114.20 en gastos", listOf(carlos, ana, barberia, minimarket)),
            GrupoUi("Ayer, miércoles 30", null, listOf(carlos.copy(id = "otro"))),
        ),
        hayFiltro: Boolean = false,
        oculto: Boolean = false,
        filtros: FiltrosDeMovimientos = FiltrosDeMovimientos(tipo = tipo, busqueda = busqueda),
    ) = MovimientosUi(tipo, busqueda, 2, grupos, hayFiltro, oculto, filtros)
}

/**
 * Comprueba que el elemento se ve, haciendo antes scroll hasta él si está en una lista desplazable: en una pantalla
 * chica (como el emulador de la CI) lo de abajo queda fuera de la vista aunque la app se pueda desplazar hasta ahí.
 */
fun SemanticsNodeInteraction.verEnPantalla(): SemanticsNodeInteraction = apply {
    runCatching { performScrollTo() }
    assertIsDisplayed()
}
