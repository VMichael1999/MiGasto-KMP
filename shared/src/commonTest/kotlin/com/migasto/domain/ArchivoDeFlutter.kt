package com.migasto.domain

import kotlin.io.encoding.Base64

// Respaldo .mgb hecho por la app de Flutter (el BackupService real, 10 000 vueltas) con la
// contraseña de CONTRASENA_DE_FLUTTER. Si el formato cambia, se vuelve a generar con Flutter.
const val CONTRASENA_DE_FLUTTER = "contraseña de prueba 123"

private const val BASE64_DEL_ARCHIVO =
        "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" +
        "INAK8toTkF8WVq4eQfYPyatqYzZ68Poliw=="

val respaldoHechoPorFlutter: ByteArray = Base64.decode(BASE64_DEL_ARCHIVO)
