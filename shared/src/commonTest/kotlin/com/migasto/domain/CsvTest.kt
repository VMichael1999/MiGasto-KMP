package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Portado del grupo «CSV» de test/lock_and_csv_test.dart. El grupo «Bloqueo» es de la
// interfaz (Face ID, BiometricPrompt) y se porta con las apps.
class CsvTest {
    private val utc = TimeZone.UTC

    @Test
    fun incluyeTipoCanalYEstadoYNoLaUbicacion() {
        val csv = movimientosACsv(
            listOf(
                Movimiento(
                    id = "1",
                    amount = 25.5,
                    merchant = "Tambo \"Miraflores\"",
                    category = Categoria.Compras,
                    source = PaymentSource.Yape,
                    date = fecha(),
                    tipo = TipoMovimiento.Gasto,
                    canal = CanalMovimiento.Notificacion,
                    estado = EstadoMovimiento.Confirmado,
                    latitud = -12.09,
                    longitud = -77.03,
                    lugar = "Av. Arequipa 3120",
                ),
            ),
            zona = utc,
        )
        assertContains(csv, "ID,Tipo,Monto,Contraparte,Categoria,Fuente,Canal,Estado,Fecha,Notas")
        assertContains(csv, "1,gasto,25.50,\"Tambo \"\"Miraflores\"\"\",Compras,yape,notificacion,confirmado,")
        assertFalse("-12.09" in csv)
        assertFalse("Arequipa" in csv)
    }

    @Test
    fun muestraElNombreDeLaCategoriaPropiaEntreComillasSiTraeComas() {
        val propia = CategoriaPropia("c1", "Café, postres", "cafe", TipoMovimiento.Gasto)
        val csv = movimientosACsv(
            listOf(
                Movimiento(
                    id = "2",
                    amount = 8.0,
                    merchant = "Lima Café",
                    category = Categoria.Otros,
                    categoriaPropia = "c1",
                    source = PaymentSource.Efectivo,
                    date = fecha(),
                ),
            ),
            categoriasPropias = listOf(propia),
            zona = utc,
        )
        assertContains(csv, "\"Café, postres\",efectivo")
    }

    // Casos nuevos

    @Test
    fun laFechaSaleSinZonaYConMilisegundosComoEnFlutter() {
        val csv = movimientosACsv(listOf(unGasto()), zona = utc)
        assertEquals(
            "ID,Tipo,Monto,Contraparte,Categoria,Fuente,Canal,Estado,Fecha,Notas\n" +
                "x,gasto,25.50,\"Tambo\",Compras,yape,manual,pendiente,2026-09-27T12:41:00.000,\"\"\n",
            csv,
        )
    }

    @Test
    fun laFechaSeEscribeEnLaHoraLocalDeLaZona() {
        val lima = TimeZone.of("America/Lima")
        // 12:41 UTC son las 07:41 en Lima (UTC-5).
        assertContains(movimientosACsv(listOf(unGasto()), zona = lima), "2026-09-27T07:41:00.000")
    }

    @Test
    fun losMontosSiempreLlevanDosDecimales() {
        fun monto(x: Double) = movimientosACsv(listOf(unGasto(x)), zona = utc).lines()[1].split(',')[2]
        assertEquals("8.00", monto(8.0))
        assertEquals("134.85", monto(134.85))
        assertEquals("1200.50", monto(1200.5))
        assertEquals("0.30", monto(0.1 + 0.2))
        assertEquals("0.00", monto(0.0))
        assertEquals("3200.00", monto(3200.0))
    }

    @Test
    fun lasNotasConSaltosDeLineaQuedanEntreComillas() {
        val csv = movimientosACsv(listOf(unGasto().copiar(notes = "línea 1\nlínea 2")), zona = utc)
        assertContains(csv, "\"línea 1\nlínea 2\"")
    }

    @Test
    fun sinMovimientosSoloVaLaCabecera() {
        assertEquals(
            "ID,Tipo,Monto,Contraparte,Categoria,Fuente,Canal,Estado,Fecha,Notas\n",
            movimientosACsv(emptyList(), zona = utc),
        )
    }

    private fun unGasto(monto: Double = 25.5) = Movimiento(
        id = "x",
        amount = monto,
        merchant = "Tambo",
        category = Categoria.Compras,
        source = PaymentSource.Yape,
        date = fecha(),
    )
}
