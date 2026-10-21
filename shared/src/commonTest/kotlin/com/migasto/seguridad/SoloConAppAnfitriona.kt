package com.migasto.seguridad

/**
 * Marca los tests que necesitan correr dentro de una app (con su Keychain y su identificador). En iOS
 * el ejecutable de pruebas de Kotlin/Native no es una app y el sistema responde «no hay llavero»
 * (errSecNotAvailable, -25291), así que allí se omiten hasta que la app anfitriona de `iosApp` los
 * ejecute. En Android no hace nada: el Keystore funciona en las pruebas instrumentadas.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class SoloConAppAnfitriona()
