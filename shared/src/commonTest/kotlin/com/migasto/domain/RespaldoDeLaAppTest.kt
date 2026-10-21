package com.migasto.domain

import com.migasto.db.BaseMiGasto
import com.migasto.db.fabricaDePrueba
import com.migasto.domain.reglas.ReglasIncluidas
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Portado de «restaurar en la app» (backup_test.dart) y de «el respaldo lleva los alias» (alias_test.dart).
class RespaldoDeLaAppTest {
    private val fabrica = fabricaDePrueba()
    private val nombre = "prueba-respaldo-app.db"
    private val ahora = fecha(dia = 27, hora = 12, minuto = 41)

    @AfterTest
    fun limpiar() = fabrica.borrar(nombre)

    private fun abrir() = BaseMiGasto.abrir(fabrica, nombre, "una-clave-de-prueba")

    private fun BaseMiGasto.respaldo() = RespaldoDeLaApp(movimientos, ajustes, categoriasPropias) { ahora }

    private fun BaseMiGasto.registro() = RegistroDeMovimientos(
        movimientos, ajustes, ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias), { ahora },
    )

    private fun mov(id: String, comercio: String) =
        Movimiento(id, 5.0, comercio, Categoria.Alimentacion, PaymentSource.Yape, ahora, estado = EstadoMovimiento.Confirmado)

    @Test
    fun agregaLoQueFaltaNoDuplicaYRecuperaPresupuestoYCategorias() {
        abrir().use { base ->
            base.registro().agregarManual(TipoMovimiento.Gasto, 10.0, "Ya estaba", Categoria.Otros)
            val yaEstaba = base.movimientos.todos().single()

            val contenido = ContenidoDeRespaldo(
                creado = fecha(dia = 1, hora = 0, minuto = 0, mes = 10),
                presupuesto = 900.0,
                aprendidas = mapOf("tambo" to "alimentacion", "raro" to "noExiste"),
                movimientos = listOf(yaEstaba, mov("a1", "Tambo Surco"), mov("b2", "Otro")),
            )
            val respaldo = base.respaldo()
            assertEquals(2, respaldo.cuantosSonNuevos(contenido))

            assertEquals(2, respaldo.restaurar(contenido))
            assertEquals(3, base.movimientos.todos().size)
            assertEquals(900.0, base.ajustes.presupuesto())
            assertEquals(mapOf("tambo" to "alimentacion"), base.ajustes.aprendidas()) // «raro» no existe: se descarta

            // Restaurar dos veces el mismo respaldo no duplica nada.
            assertEquals(0, respaldo.restaurar(contenido))
            assertEquals(3, base.movimientos.todos().size)

            // El respaldo que se crea desde la app trae todo.
            val de = respaldo.contenido()
            assertEquals(3, de.movimientos.size)
            assertEquals(900.0, de.presupuesto)
            assertEquals("alimentacion", de.aprendidas["tambo"])
            assertEquals(ahora, de.creado)
        }
    }

    @Test
    fun elRespaldoLlevaLosAliasYRestaurarNoPisaLosTuyos() {
        abrir().use { base ->
            base.ajustes.establecerAlias("Juan Perez", "Juan")
            val respaldo = base.respaldo()
            assertEquals(mapOf("juan perez" to "Juan"), respaldo.contenido().alias)

            val contenido = ContenidoDeRespaldo(
                creado = ahora, presupuesto = 1000.0, aprendidas = emptyMap(), movimientos = emptyList(),
                alias = mapOf("juan perez" to "Juanito", "maria lopez" to "Mari"),
            )
            respaldo.restaurar(contenido)
            assertEquals(mapOf("juan perez" to "Juan", "maria lopez" to "Mari"), base.ajustes.aliases())
        }
    }

    @Test
    fun elRespaldoLlevaYMezclaLasCategoriasPropias() {
        abrir().use { base ->
            val mia = base.categoriasPropias.agregar("Gym", "gimnasio", TipoMovimiento.Gasto)
            val barberia = CategoriaPropia("c1", "Barbería", "spa", TipoMovimiento.Gasto)
            val respaldo = base.respaldo()
            assertEquals(listOf(mia), respaldo.contenido().categoriasPropias)

            respaldo.restaurar(
                ContenidoDeRespaldo(
                    creado = ahora, presupuesto = 0.0, aprendidas = emptyMap(), movimientos = emptyList(),
                    categoriasPropias = listOf(mia.copy(nombre = "Otro"), barberia),
                ),
            )
            assertEquals(listOf("Gym", "Barbería"), base.categoriasPropias.todas().map { it.nombre })
        }
    }

    @Test
    fun elAliasNoCambiaElMovimientoGuardado() {
        abrir().use { base ->
            base.registro().agregarManual(TipoMovimiento.Gasto, 5.0, "TAMBO SURCO", Categoria.Alimentacion)
            base.ajustes.establecerAlias("TAMBO SURCO", "Tambo")
            assertEquals("TAMBO SURCO", base.movimientos.todos().single().merchant)
            assertEquals("Tambo", nombreVisible("TAMBO SURCO", base.ajustes.aliases()))
        }
    }
}
