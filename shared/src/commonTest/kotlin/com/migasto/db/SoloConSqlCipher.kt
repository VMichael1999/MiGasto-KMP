package com.migasto.db

/**
 * Marca los tests que necesitan una base cifrada de verdad. En la JVM las pruebas usan SQLite sin
 * cifrar y se omiten; en Android e iOS corren con SQLCipher.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class SoloConSqlCipher()
