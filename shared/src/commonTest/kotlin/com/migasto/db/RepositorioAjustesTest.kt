package com.migasto.db

import com.migasto.domain.Categoria
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Portado de test/category_learning_test.dart, de «alias en la app» (alias_test.dart) y de la parte
// de ajustes de ExpenseRepositoryImpl (presupuesto, proveedores). Ya no hay SharedPreferences: todo
// va a la tabla `ajustes` de la misma base cifrada.
class RepositorioAjustesTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-ajustes.db"

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    // Categorías aprendidas

    @Test
    fun aprendeCategoriasYLasEncuentra() {
        abrir().use { base ->
            val ajustes = base.ajustes
            assertNull(ajustes.categoriaAprendida("Tambo Miraflores"))
            ajustes.guardarAprendida("Tambo Miraflores", Categoria.Compras)
            assertEquals(Categoria.Compras, ajustes.categoriaAprendida("Tambo Miraflores"))
        }
    }

    @Test
    fun aprendeSinImportarMayusculasNiEspaciosDeLosBordes() {
        abrir().use { base ->
            base.ajustes.guardarAprendida("Tambo Miraflores", Categoria.Compras)
            assertEquals(Categoria.Compras, base.ajustes.categoriaAprendida("tambo miraflores"))
            assertEquals(Categoria.Compras, base.ajustes.categoriaAprendida("TAMBO MIRAFLORES"))
            assertEquals(Categoria.Compras, base.ajustes.categoriaAprendida("  Tambo Miraflores   "))
        }
    }

    @Test
    fun olvidarUnaOTodasLasAprendidas() {
        abrir().use { base ->
            base.ajustes.guardarAprendida("Tambo", Categoria.Compras)
            base.ajustes.guardarAprendida("Uber", Categoria.Transporte)
            assertEquals(mapOf("tambo" to "compras", "uber" to "transporte"), base.ajustes.aprendidas())
            base.ajustes.olvidarAprendida(" TAMBO ")
            assertEquals(mapOf("uber" to "transporte"), base.ajustes.aprendidas())
            base.ajustes.olvidarTodasLasAprendidas()
            assertEquals(emptyMap(), base.ajustes.aprendidas())
        }
    }

    @Test
    fun unaCategoriaGuardadaQueYaNoSeConoceCaeEnOtros() {
        abrir().use { base ->
            base.ajustes.guardarCrudo("migasto_category_overrides_v2", """{"tambo":"marte"}""")
            assertEquals(Categoria.Otros, base.ajustes.categoriaAprendida("Tambo"))
        }
    }

    // Alias

    @Test
    fun guardarCambiarYQuitarUnAlias() {
        abrir().use { base ->
            val a = base.ajustes
            a.establecerAlias("MICHAEL ANTHONY VALDIVIEZO MAZA", "Michael")
            assertEquals(mapOf("michael anthony valdiviezo maza" to "Michael"), a.aliases())

            a.establecerAlias("michael anthony valdiviezo maza", "Mi yo")
            assertEquals("Mi yo", a.aliases().values.single())

            // Un nombre vacío, o igual al original, lo quita.
            a.establecerAlias("MICHAEL ANTHONY VALDIVIEZO MAZA", "   ")
            assertEquals(emptyMap(), a.aliases())
            a.establecerAlias("Tambo", "Tambo")
            assertEquals(emptyMap(), a.aliases())
        }
    }

    @Test
    fun mezclarLosAliasDeUnRespaldoNoPisaLosTuyos() {
        abrir().use { base ->
            base.ajustes.establecerAlias("Juan Perez", "Juan")
            base.ajustes.mezclarAliases(mapOf("juan perez" to "Juanito", "maria lopez" to "Mari"))
            assertEquals(mapOf("juan perez" to "Juan", "maria lopez" to "Mari"), base.ajustes.aliases())
        }
    }

    // Presupuesto, proveedores y preferencias

    @Test
    fun sinPresupuestoHastaQueElUsuarioLoDefine() {
        abrir().use { base ->
            assertEquals(0.0, base.ajustes.presupuesto())
            base.ajustes.guardarPresupuesto(1500.0)
            assertEquals(1500.0, base.ajustes.presupuesto())
            base.ajustes.guardarPresupuesto(0.0)
            assertEquals(0.0, base.ajustes.presupuesto())
        }
    }

    @Test
    fun losProveedoresEstanActivosPorDefectoYSePuedenApagar() {
        abrir().use { base ->
            assertEquals(mapOf("yape" to true, "plin" to true, "googlePay" to true), base.ajustes.proveedoresActivos())
            base.ajustes.guardarProveedoresActivos(mapOf("yape" to true, "plin" to false, "googlePay" to true))
            assertFalse(base.ajustes.proveedoresActivos().getValue("plin"))
            assertTrue(base.ajustes.proveedorActivo("yape"))
            assertFalse(base.ajustes.proveedorActivo("plin"))
            assertFalse(base.ajustes.proveedorActivo("noExiste"))
        }
    }

    @Test
    fun guardarLosIngresosAutomaticamenteEstaApagadoPorDefecto() {
        abrir().use { base ->
            assertFalse(base.ajustes.guardarIngresosAutomaticamente())
            base.ajustes.establecerGuardarIngresosAutomaticamente(true)
            assertTrue(base.ajustes.guardarIngresosAutomaticamente())
        }
    }

    @Test
    fun elMontoEnPantallaBloqueadaEstaApagadoPorDefecto() {
        abrir().use { base ->
            assertFalse(base.ajustes.mostrarMontoEnPantallaBloqueada())
            base.ajustes.establecerMostrarMontoEnPantallaBloqueada(true)
            assertTrue(base.ajustes.mostrarMontoEnPantallaBloqueada())
        }
    }

    @Test
    fun losAjustesSobrevivenAlReabrir() {
        abrir().use {
            it.ajustes.guardarPresupuesto(900.0)
            it.ajustes.establecerAlias("Tambo Surco", "Tambo")
        }
        abrir().use {
            assertEquals(900.0, it.ajustes.presupuesto())
            assertEquals("Tambo", it.ajustes.aliases().values.single())
        }
    }

    @Test
    fun unValorDanadoSeIgnoraYSeUsaElPorDefecto() {
        abrir().use { base ->
            base.ajustes.guardarCrudo("migasto_monthly_budget_v2", "no es un número")
            base.ajustes.guardarCrudo("migasto_aliases_v1", "{roto")
            base.ajustes.guardarCrudo("migasto_providers_enabled_v2", "[]")
            assertEquals(0.0, base.ajustes.presupuesto())
            assertEquals(emptyMap(), base.ajustes.aliases())
            assertEquals(3, base.ajustes.proveedoresActivos().size)
        }
    }
}
