package com.migasto.android

import android.app.Application
import com.migasto.db.BaseMiGasto
import com.migasto.db.FabricaDeDriverAndroid
import com.migasto.domain.ClasificadorCategorias
import com.migasto.domain.RegistroDeMovimientos
import com.migasto.domain.reglas.ReglasIncluidas
import com.migasto.seguridad.AlmacenSeguroAndroid
import com.migasto.seguridad.ClaveDeLaBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow

class MiGastoApp : Application() {
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * La base cifrada, abierta en segundo plano con la clave que vive en el Keystore: abrirla y leer la
     * clave tarda, y no debe frenar el arranque. Quien la use hace `await()`.
     */
    lateinit var base: Deferred<BaseMiGasto>
        private set

    /** Los casos de uso de registrar, confirmar y eliminar movimientos, sobre la misma base. */
    lateinit var registro: Deferred<RegistroDeMovimientos>
        private set

    /** Si el usuario ocultó los montos (el ojo del Resumen). Vale para todas las pantallas. */
    val montosOcultos = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        base = ambito.async {
            val clave = ClaveDeLaBase(AlmacenSeguroAndroid(this@MiGastoApp)).obtenerOCrear()
            BaseMiGasto.abrir(FabricaDeDriverAndroid(this@MiGastoApp), "migasto.db", clave).also(SemillaDeEjemplo::aplicar)
        }
        registro = ambito.async {
            val b = base.await()
            RegistroDeMovimientos(b.movimientos, b.ajustes, ClasificadorCategorias.desdeJson(ReglasIncluidas.categorias))
        }
    }
}
