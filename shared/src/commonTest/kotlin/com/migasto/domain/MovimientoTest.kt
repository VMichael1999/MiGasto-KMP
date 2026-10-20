package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Comportamiento de lib/domain/entities/movimiento.dart. Los tests de movimiento_test.dart
// que pasan por la base, los providers o la cola nativa se portan con esas capas.
class MovimientoTest {
    private fun movimiento(id: String = "a") = Movimiento(
        id = id,
        amount = 25.5,
        merchant = "Tambo",
        category = Categoria.Compras,
        source = PaymentSource.Yape,
        date = fecha(),
    )

    @Test
    fun losValoresPorDefectoSonLosDeUnGastoManualPendiente() {
        val m = movimiento()
        assertEquals(TipoMovimiento.Gasto, m.tipo)
        assertEquals(CanalMovimiento.Manual, m.canal)
        assertEquals(EstadoMovimiento.Pendiente, m.estado)
        assertEquals("", m.notes)
        assertEquals("", m.textoOriginal)
        assertTrue(m.esGasto)
        assertFalse(m.esIngreso)
        assertFalse(m.isConfirmed)
        assertFalse(m.tieneUbicacion)
    }

    @Test
    fun dosMovimientosSonIgualesSiTienenElMismoId() {
        assertEquals(movimiento("a"), movimiento("a").copiar(amount = 99.0))
        assertNotEquals(movimiento("a"), movimiento("b"))
        assertEquals(movimiento("a").hashCode(), movimiento("a").hashCode())
    }

    @Test
    fun lasCategoriasDeGastoYDeIngresoNoSeMezclan() {
        assertEquals(6, Categoria.gastos.size)
        assertEquals(4, Categoria.ingresos.size)
        assertEquals(Categoria.entries.size, Categoria.gastos.size + Categoria.ingresos.size)
        assertTrue(Categoria.Sueldo.esDeIngreso)
        assertFalse(Categoria.Alimentacion.esDeIngreso)
        assertEquals(Categoria.ingresos, Categoria.paraTipo(TipoMovimiento.Ingreso))
        assertEquals(Categoria.gastos, Categoria.paraTipo(TipoMovimiento.Gasto))
    }

    @Test
    fun tieneUbicacionSoloConLatitudYLongitud() {
        assertFalse(movimiento().copiar(latitud = -12.09).tieneUbicacion)
        assertTrue(movimiento().copiar(latitud = -12.09, longitud = -77.03).tieneUbicacion)
    }

    @Test
    fun quitarubicacionBorraLosCuatroDatosDelLugar() {
        val conLugar = movimiento().copiar(
            latitud = -12.09, longitud = -77.03, precision = 15.0, lugar = "Av. Arequipa 3120",
        )
        val sinLugar = conLugar.copiar(quitarUbicacion = true)
        assertNull(sinLugar.latitud)
        assertNull(sinLugar.longitud)
        assertNull(sinLugar.precision)
        assertNull(sinLugar.lugar)
        assertFalse(sinLugar.tieneUbicacion)
    }

    @Test
    fun copiarSinArgumentosConservaTodo() {
        val m = movimiento().copiar(tarjeta = "Visa BBVA ···4821", categoriaPropia = "p1")
        val copia = m.copiar()
        assertEquals("Visa BBVA ···4821", copia.tarjeta)
        assertEquals("p1", copia.categoriaPropia)
        assertEquals(m.category, copia.category)
    }

    @Test
    fun elegirUnaCategoriaDeSiempreQuitaLaPropiaYElegirUnaPropiaLaPone() {
        val conPropia = movimiento().copiar(categoriaPropia = "mascotas")
        assertEquals("mascotas", conPropia.categoriaPropia)
        // Cambiar otro campo no toca la propia.
        assertEquals("mascotas", conPropia.copiar(merchant = "Otro").categoriaPropia)
        // Elegir una categoría de siempre la quita.
        assertNull(conPropia.copiar(category = Categoria.Otros).categoriaPropia)
    }

    @Test
    fun laCategoriaSeGuardaEnUnaSolaColumna() {
        assertEquals("otros", categoriaAlmacenada(Categoria.Otros, null))
        assertEquals("otros|mascotas", categoriaAlmacenada(Categoria.Otros, "mascotas"))
        assertEquals("transferenciaRecibida", categoriaAlmacenada(Categoria.TransferenciaRecibida, null))
    }

    @Test
    fun leerLaCategoriaGuardadaEsLaInversaDeGuardarla() {
        for (categoria in Categoria.entries) {
            assertEquals(
                CategoriaLeida(categoria, null),
                leerCategoriaAlmacenada(categoriaAlmacenada(categoria, null), Categoria.Otros),
            )
        }
        assertEquals(
            CategoriaLeida(Categoria.Otros, "mascotas"),
            leerCategoriaAlmacenada("otros|mascotas", Categoria.Compras),
        )
    }

    @Test
    fun leerUnaCategoriaVaciaODesconocidaUsaLaDeRespaldo() {
        assertEquals(CategoriaLeida(Categoria.Compras, null), leerCategoriaAlmacenada(null, Categoria.Compras))
        assertEquals(CategoriaLeida(Categoria.Compras, null), leerCategoriaAlmacenada("", Categoria.Compras))
        assertEquals(CategoriaLeida(Categoria.Compras, null), leerCategoriaAlmacenada("noExiste", Categoria.Compras))
        assertEquals(CategoriaLeida(Categoria.Compras, "x"), leerCategoriaAlmacenada("noExiste|x", Categoria.Compras))
    }

    @Test
    fun unaBarraAlFinalNoDejaCategoriaPropia() {
        assertEquals(CategoriaLeida(Categoria.Otros, null), leerCategoriaAlmacenada("otros|", Categoria.Compras))
    }

    @Test
    fun lasClavesGuardadasDeLosEnumsSonLasMismasQueLasDeFlutter() {
        assertEquals(
            listOf(
                "alimentacion", "transporte", "compras", "servicios", "entretenimiento", "otros",
                "sueldo", "transferenciaRecibida", "venta", "otrosIngresos",
            ),
            Categoria.entries.map { it.clave },
        )
        assertEquals(
            listOf("yape", "plin", "googlePay", "manual", "tarjeta", "efectivo", "otro"),
            PaymentSource.entries.map { it.clave },
        )
        assertEquals(listOf("gasto", "ingreso"), TipoMovimiento.entries.map { it.clave })
        assertEquals(listOf("pendiente", "confirmado"), EstadoMovimiento.entries.map { it.clave })
        assertEquals(
            listOf("notificacion", "wallet", "captura", "manual"),
            CanalMovimiento.entries.map { it.clave },
        )
    }

    @Test
    fun unEnumSeLeePorSuClaveYSiNoExisteDevuelveNull() {
        assertEquals(PaymentSource.GooglePay, PaymentSource.desdeClave("googlePay"))
        assertNull(PaymentSource.desdeClave("paypal"))
        assertEquals(Categoria.OtrosIngresos, Categoria.desdeClave("otrosIngresos"))
    }
}
