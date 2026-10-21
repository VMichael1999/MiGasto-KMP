# Compatibilidad del respaldo `.mgb` con la app de Flutter

El formato es el mismo en las dos apps: `"MGB1"` · vueltas (4 bytes, big endian) · sal (16) · nonce (12) ·
texto cifrado · etiqueta (16). Clave con PBKDF2-HMAC-SHA256 (210 000 vueltas) y AES-256-GCM; la cabecera
queda autenticada.

## Flutter → KMP

`RespaldoTest.abreUnRespaldoHechoPorFlutter` abre un archivo real hecho por el `BackupService` de Flutter
(copiado sin cambios a un paquete Dart aparte, 10 000 vueltas). El archivo va incrustado en
`ArchivoDeFlutter.kt`. Si el formato cambia, hay que volver a generarlo con Flutter.

## KMP → Flutter

Verificado a mano el 2026-10-21: un archivo hecho por `ServicioDeRespaldo` (iOS, 10 000 vueltas) se abrió con
el `BackupService.decrypt` de Flutter, con presupuesto, alias, categorías propias y un movimiento con
`ÑAÑO 日本`, ubicación y fecha intactos. Falta automatizarlo (hoy no hay forma de correr Dart desde los tests).

## Fechas

Flutter guarda las fechas con `DateTime.toIso8601String()`: hora local sin zona o UTC con `Z`. `FechaIso.kt`
lee las dos y escribe igual (movimientos en hora local, `creado` en UTC).
