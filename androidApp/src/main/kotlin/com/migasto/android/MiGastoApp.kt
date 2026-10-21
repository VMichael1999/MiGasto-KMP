package com.migasto.android

import android.app.Application
import com.migasto.db.BaseMiGasto
import com.migasto.db.FabricaDeDriverAndroid
import com.migasto.seguridad.AlmacenSeguroAndroid
import com.migasto.seguridad.ClaveDeLaBase

class MiGastoApp : Application() {
    /** La base cifrada, abierta con la clave que vive en el Keystore. */
    lateinit var base: BaseMiGasto
        private set

    override fun onCreate() {
        super.onCreate()
        val clave = ClaveDeLaBase(AlmacenSeguroAndroid(this)).obtenerOCrear()
        base = BaseMiGasto.abrir(FabricaDeDriverAndroid(this), "migasto.db", clave)
        SemillaDeEjemplo.aplicar(base)
    }
}
