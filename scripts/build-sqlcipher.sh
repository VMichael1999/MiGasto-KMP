#!/bin/zsh
# Compila SQLCipher (amalgamation del pod) como librería estática para el simulador de iOS.
# Requisito: haber corrido antes `./gradlew :shared:podBuildSQLCipherIosSimulator` (o cualquier tarea que baje el pod).
# Se necesita porque el SQLite de Apple (libsqlite3.dylib) gana a un framework dinámico de SQLCipher.
set -euo pipefail
cd "$(dirname "$0")/.."
SRC=shared/build/cocoapods/synthetic/ios/Pods/SQLCipher
OUT=shared/build/sqlcipher/iosSimulatorArm64
mkdir -p "$OUT"
FLAGS=$(grep '^OTHER_CFLAGS' shared/build/cocoapods/buildSettings/build-settings-iosSimulator-SQLCipher.properties | cut -d= -f2-)
SDK=$(xcrun --sdk iphonesimulator --show-sdk-path)
xcrun clang -c -O2 -arch arm64 -isysroot "$SDK" -mios-simulator-version-min=17.0 ${=FLAGS} -w "$SRC/sqlite3.c" -o "$OUT/sqlite3.o"
xcrun ar rcs "$OUT/libsqlcipher.a" "$OUT/sqlite3.o"
echo "Listo: $OUT/libsqlcipher.a"
