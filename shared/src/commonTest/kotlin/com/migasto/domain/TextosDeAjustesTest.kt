package com.migasto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

// Port de los textos y validaciones de lib/presentation/settings/settings_screen.dart que no dependen
// de la pantalla: contraseña del respaldo, resumen al restaurar, presupuesto, categorías aprendidas.
class TextosDeAjustesTest {
    private val utc = TimeZone.UTC
    private val ahora = fecha(dia = 21, hora = 10, minuto = 14, mes = 10)

    // Contraseña del respaldo

    @Test
    fun alCrearPideAlMenosOchoCaracteres() {
        assertEquals("Usa al menos 8 caracteres.", validarContrasenaDeRespaldo("corta", "corta", creando = true))
        assertEquals("Usa al menos 8 caracteres.", validarContrasenaDeRespaldo("1234567", "1234567", creando = true))
    }

    @Test
    fun alCrearLasDosDebenCoincidir() {
        assertEquals("Las contraseñas no coinciden.", validarContrasenaDeRespaldo("una contraseña", "otra contraseña", creando = true))
        assertNull(validarContrasenaDeRespaldo("una contraseña", "una contraseña", creando = true))
    }

    @Test
    fun alAbrirSoloPideQueEscriba() {
        assertEquals("Escribe la contraseña.", validarContrasenaDeRespaldo("", "", creando = false))
        assertNull(validarContrasenaDeRespaldo("x", "", creando = false))
    }

    // Respaldo

    @Test
    fun elSubtituloDelRespaldo() {
        assertEquals(
            "Aún no has hecho uno. Si pierdes el teléfono, pierdes tus datos.",
            subtituloDelRespaldo(null, utc),
        )
        assertEquals("Último: 21 oct 2026, 10:14", subtituloDelRespaldo(ahora, utc))
        assertEquals("Último: 21 oct 2026, 05:14", subtituloDelRespaldo(ahora, TimeZone.of("America/Lima")))
    }

    @Test
    fun elNombreDelArchivoDeRespaldo() {
        assertEquals("MiGasto-respaldo-2026-10-21_1014.mgb", nombreDelArchivoDeRespaldo(ahora, utc))
        assertEquals("MiGasto-respaldo-2026-01-05_0907.mgb", nombreDelArchivoDeRespaldo(fecha(dia = 5, hora = 9, minuto = 7, mes = 1), utc))
    }

    @Test
    fun elResumenAntesDeRestaurar() {
        val contenido = ContenidoDeRespaldo(
            movimientos = listOf(Movimiento("a", 1.0, "x", Categoria.Otros, PaymentSource.Yape, ahora)),
            presupuesto = 900.0, aprendidas = emptyMap(), creado = fecha(dia = 1, hora = 7, minuto = 0, mes = 10),
        )
        assertEquals(
            "Creado el 1 oct 2026, 07:00.\n\n" +
                "• 1 movimientos en el respaldo, 1 nuevos para este teléfono.\n" +
                "• Presupuesto: S/ 900.00.\n\n" +
                "No se borra nada de lo que ya tienes.",
            textoDeRestauracion(contenido, nuevos = 1, zona = utc),
        )
    }

    @Test
    fun elMensajeDespuesDeRestaurar() {
        assertEquals("No había nada nuevo: ya tenías todos esos movimientos.", mensajeDeRestauracion(0))
        assertEquals("Se restauraron 3 movimientos.", mensajeDeRestauracion(3))
        assertEquals("Se restauraron 1 movimientos.", mensajeDeRestauracion(1))
    }

    // Presupuesto

    @Test
    fun elPresupuestoDebeSerMayorQueCero() {
        assertEquals(1500.0, parsearPresupuesto("1500"))
        assertEquals(1500.5, parsearPresupuesto("1500,5"))
        assertNull(parsearPresupuesto("0"))
        assertNull(parsearPresupuesto("-3"))
        assertNull(parsearPresupuesto("abc"))
        assertNull(parsearPresupuesto(""))
    }

    @Test
    fun elPresupuestoComoSeEdita() {
        assertEquals("1500", presupuestoParaEditar(1500.0))
        assertEquals("1501", presupuestoParaEditar(1500.5))
        assertEquals("", presupuestoParaEditar(0.0))
    }

    @Test
    fun elTextoDelPresupuestoEnAjustes() {
        assertEquals("S/ 1,500.00", textoDelPresupuestoEnAjustes(1500.0))
        assertEquals("Sin definir", textoDelPresupuestoEnAjustes(0.0))
    }

    // Categorías aprendidas

    @Test
    fun elNombreDeUnaCategoriaAprendida() {
        assertEquals("Alimentación", nombreDeCategoriaAprendida("alimentacion"))
        assertEquals("Transferencia recibida", nombreDeCategoriaAprendida("transferenciaRecibida"))
        assertEquals("noExiste", nombreDeCategoriaAprendida("noExiste"))
    }

    @Test
    fun lasAprendidasSalenOrdenadasPorComercio() {
        val lista = aprendidasOrdenadas(mapOf("uber" to "transporte", "tambo" to "compras", "kfc" to "alimentacion"))
        assertEquals(listOf("kfc", "tambo", "uber"), lista.map { it.comercio })
        assertEquals(listOf("Alimentación", "Compras", "Transporte"), lista.map { it.categoria })
    }

    @Test
    fun losAliasSalenOrdenados() {
        val lista = aliasOrdenados(mapOf("zeta" to "Z", "ana lopez" to "Ana"))
        assertEquals(listOf("ana lopez", "zeta"), lista.map { it.clave })
        assertEquals(listOf("Ana", "Z"), lista.map { it.nombre })
    }

    @Test
    fun elUltimoRespaldoSeGuardaEnLosAjustes() {
        val ultimo: Instant = ahora
        assertEquals("2026-10-21T10:14:00.000Z", fechaIsoUtc(ultimo))
    }
}
