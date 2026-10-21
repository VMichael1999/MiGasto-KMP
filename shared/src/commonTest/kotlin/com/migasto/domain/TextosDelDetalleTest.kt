package com.migasto.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

// Los textos fijos del detalle y de sus diálogos (antes escritos a mano en la pantalla de Android), y el nombre
// con el que se abre «Cambiar nombre».
class TextosDelDetalleTest {
    private fun detalle(alias: Map<String, String> = emptyMap()) = construirDetalle(
        Movimiento("m", 25.0, "BARBERÍA EL PATRÓN", Categoria.Otros, PaymentSource.Efectivo, fecha(dia = 1, mes = 10)),
        alias, emptyList(), TimeZone.UTC,
    )

    @Test
    fun losTextosFijos() {
        assertEquals("Editar", TextosDelDetalle.EDITAR)
        assertEquals("Cancelar", TextosDelDetalle.CANCELAR)
        assertEquals("Guardar", TextosDelDetalle.GUARDAR)
        assertEquals("Eliminar", TextosDelDetalle.ELIMINAR)
        assertEquals("Fecha", TextosDelDetalle.FECHA)
        assertEquals("Categoría", TextosDelDetalle.CATEGORIA)
        assertEquals("Registrado", TextosDelDetalle.REGISTRADO)
        assertEquals("Nota", TextosDelDetalle.NOTA)
        assertEquals("Texto original", TextosDelDetalle.TEXTO_ORIGINAL)
        assertEquals("Por confirmar", TextosDelDetalle.POR_CONFIRMAR)
        assertEquals("Cambiar nombre", TextosDelDetalle.CAMBIAR_NOMBRE)
        assertEquals("Nombre", TextosDelDetalle.NOMBRE)
        assertEquals("Quitar nombre", TextosDelDetalle.QUITAR_NOMBRE)
        assertEquals("Monto", TextosDelDetalle.MONTO)
        assertEquals("Nota (opcional)", TextosDelDetalle.NOTA_OPCIONAL)
        assertEquals("Guardar cambios", TextosDelDetalle.GUARDAR_CAMBIOS)
        assertEquals("Pon un monto mayor que cero y un nombre.", TextosDelDetalle.ERROR_DE_EDICION)
    }

    @Test
    fun losTextosQueDependenDelMovimiento() {
        assertEquals("Editar gasto", TextosDelDetalle.tituloDeEdicion("Gasto"))
        assertEquals("Editar ingreso", TextosDelDetalle.tituloDeEdicion("Ingreso"))
        assertEquals("Comercio", TextosDelDetalle.etiquetaDelComercio(false))
        assertEquals("De quién", TextosDelDetalle.etiquetaDelComercio(true))
        assertEquals("Aplicar a todos los pagos a Wong", TextosDelDetalle.aplicarATodos("Wong"))
        assertEquals("Quitar la ubicación de este gasto", TextosDelDetalle.quitarUbicacion("Gasto"))
        assertEquals(
            "Así verás a \"Wong\" en toda la app. Es solo cómo se ve: el pago no cambia.",
            TextosDelDetalle.explicacionDelNombre("Wong"),
        )
    }

    @Test
    fun cambiarNombreSeAbreConLaSugerenciaSiNoHayNombrePuesto() {
        val d = detalle()
        assertEquals(false, d.tieneNombrePropio)
        assertEquals(sugerenciaDeAlias("BARBERÍA EL PATRÓN"), d.nombreParaEditar)
    }

    @Test
    fun cambiarNombreSeAbreConElNombreQueYaSePuso() {
        val d = detalle(mapOf(claveDeAlias("BARBERÍA EL PATRÓN") to "La barbería"))
        assertEquals(true, d.tieneNombrePropio)
        assertEquals("La barbería", d.nombreParaEditar)
    }
}
