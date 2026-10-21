package com.migasto.db

import com.migasto.seguridad.AlmacenSeguroIos
import com.migasto.seguridad.ClaveDeLaBase

/**
 * Abre la base cifrada de la app en iPhone: la clave se genera la primera vez y vive en el Keychain.
 * Es lo único que Swift necesita para arrancar (Swift no ve los valores por defecto de Kotlin).
 */
fun abrirLaBaseDeLaApp(): BaseMiGasto =
    BaseMiGasto.abrir(FabricaDeDriverIos(), "migasto.db", ClaveDeLaBase(AlmacenSeguroIos()).obtenerOCrear())
