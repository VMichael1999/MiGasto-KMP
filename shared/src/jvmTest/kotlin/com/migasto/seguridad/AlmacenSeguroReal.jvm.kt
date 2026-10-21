package com.migasto.seguridad

/** En la JVM no hay Keychain ni Keystore: se usa el almacén en memoria. */
actual fun almacenSeguroReal(): AlmacenSeguro = AlmacenEnMemoria()
