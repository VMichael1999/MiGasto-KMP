package com.migasto.android.pagos

/** Las apps de las que se leen notificaciones: Yape, Google Wallet y los bancos con Plin. */
internal object PaquetesDePago {
    private val reales = setOf(
        "com.bcp.innovacxion.yapeapp",
        "com.google.android.apps.walletnfcrel",
        "com.bbva.bbvacontigo",
        "pe.com.interbank.mobilebanking",
        "pe.com.scotiabank.blpm.android.client",
        "com.banbif.plin",
    )

    val leidos: Set<String> = reales + PaquetesDeDepuracion.extra
}
