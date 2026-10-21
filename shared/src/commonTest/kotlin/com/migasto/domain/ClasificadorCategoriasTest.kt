package com.migasto.domain

import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.Test
import kotlin.test.assertEquals

// Portado de test/nlp_classifier_test.dart.
class ClasificadorCategoriasTest {
    private val clasificador = ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias)

    private fun gasto(texto: String, nombre: String) = clasificador.clasificarGasto(texto, nombre)

    @Test
    fun alimentacion() {
        assertEquals(Categoria.Alimentacion, gasto("Yapeaste S/ 15.50 a Rappi", "Rappi Peru"))
        assertEquals(
            Categoria.Alimentacion,
            gasto("Plin: Enviaste S/ 35.00 a Restaurante Rustica", "Rustica"),
        )
    }

    @Test
    fun unServicioPagadoConYapeCaeEnServicios() {
        assertEquals(
            Categoria.Servicios,
            gasto("¡Yapeaste el servicio! S/ 134.85 BanBif Servicio: Pago de Cuotas Prestamos", "BanBif"),
        )
    }

    @Test
    fun compras() {
        assertEquals(Categoria.Compras, gasto("Compra Google Pay S/ 120.90 en Metro", "Metro S.A."))
        assertEquals(Categoria.Compras, gasto("Yapeaste S/ 18.00 a Tambo", "Tambo Miraflores"))
        assertEquals(Categoria.Compras, gasto("Compra de medicina en Inkafarma", "Inkafarma"))
    }

    @Test
    fun transporte() {
        assertEquals(Categoria.Transporte, gasto("Plin: Pago de S/ 25.00 a Taxi Uber", "Uber"))
        assertEquals(Categoria.Transporte, gasto("Compra S/ 85.00 en Repsol", "Grifo Repsol"))
    }

    @Test
    fun serviciosYEntretenimiento() {
        assertEquals(Categoria.Entretenimiento, gasto("Cargo S/ 44.90 Netflix", "Netflix Peru"))
        assertEquals(Categoria.Servicios, gasto("Pago Enel Luz", "Enel Distribucion"))
    }

    @Test
    fun noConfundeNombresDePersonasNiLugaresParecidos() {
        assertEquals(Categoria.Otros, gasto("Yapeaste S/ 20.00 a Luz María", "Luz María"))
        assertEquals(Categoria.Otros, gasto("Yapeaste S/ 5.00 a Metropolitano", "Metropolitano"))
    }

    @Test
    fun compraPorSoloNoDecideLaCategoria() {
        assertEquals(Categoria.Otros, gasto("Compra por S/ 30.00", "Bazar Lucía"))
    }

    @Test
    fun sueldoVentaYTransferencia() {
        assertEquals(
            Categoria.Sueldo,
            clasificador.clasificarIngreso("Abono de sueldo S/ 3,200.00", "Mi Empresa"),
        )
        assertEquals(Categoria.Venta, clasificador.clasificarIngreso("Pago por venta S/ 180.00", "Ana"))
        assertEquals(
            Categoria.TransferenciaRecibida,
            clasificador.clasificarIngreso("Juan te yapeó S/ 15.00", "Juan Pérez"),
        )
    }

    // casos nuevos

    @Test
    fun noDistingueMayusculasNiAcentos() {
        assertEquals(Categoria.Alimentacion, gasto("Pago en POLLERÍA", "Pollería X"))
        assertEquals(Categoria.Entretenimiento, gasto("Cargo", "NETFLIX"))
    }

    @Test
    fun unGastoNoSeClasificaConLasReglasDeIngreso() {
        assertEquals(Categoria.Otros, gasto("Pago de sueldo", "Ana"))
    }
}
