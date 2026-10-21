import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.androidKmpLibrary)
}

// SQLCipher como librería estática: el SQLite de Apple (libsqlite3.dylib) gana a un framework
// dinámico, así que se compila el sqlite3.c del pod y se enlaza con -force_load.
val sqlcipherFlags = listOf(
    "-DSQLITE_HAS_CODEC", "-DSQLITE_TEMP_STORE=2", "-DSQLITE_SOUNDEX", "-DSQLITE_THREADSAFE",
    "-DSQLITE_ENABLE_RTREE", "-DSQLITE_ENABLE_STAT3", "-DSQLITE_ENABLE_STAT4",
    "-DSQLITE_ENABLE_COLUMN_METADATA", "-DSQLITE_ENABLE_MEMORY_MANAGEMENT",
    "-DSQLITE_ENABLE_LOAD_EXTENSION", "-DSQLITE_ENABLE_FTS4", "-DSQLITE_ENABLE_FTS4_UNICODE61",
    "-DSQLITE_ENABLE_FTS3_PARENTHESIS", "-DSQLITE_ENABLE_UNLOCK_NOTIFY", "-DSQLITE_ENABLE_JSON1",
    "-DSQLITE_ENABLE_FTS5", "-DSQLCIPHER_CRYPTO_CC", "-DHAVE_USLEEP=1",
    "-DSQLITE_MAX_VARIABLE_NUMBER=99999", "-DSQLITE_EXTRA_INIT=sqlcipher_extra_init",
    "-DSQLITE_EXTRA_SHUTDOWN=sqlcipher_extra_shutdown",
)

fun sqlcipherTask(taskName: String, dir: String, sdk: String, minFlag: String) =
    tasks.register<Exec>(taskName) {
        group = "sqlcipher"
        description = "Compila SQLCipher como librería estática para $dir"
        dependsOn("podInstallSyntheticIos")
        val fuente = layout.buildDirectory.file("cocoapods/synthetic/ios/Pods/SQLCipher/sqlite3.c")
        val salida = layout.buildDirectory.dir("sqlcipher/$dir")
        inputs.file(fuente)
        inputs.property("flags", sqlcipherFlags)
        outputs.dir(salida)
        doFirst { salida.get().asFile.mkdirs() }
        val obj = salida.get().asFile.resolve("sqlite3.o")
        val lib = salida.get().asFile.resolve("libsqlcipher.a")
        val cflags = sqlcipherFlags.joinToString(" ")
        commandLine(
            "sh", "-c",
            "xcrun --sdk $sdk clang -c -O2 -arch arm64 $minFlag $cflags -w " +
                "'${fuente.get().asFile}' -o '$obj' && rm -f '$lib' && xcrun ar rcs '$lib' '$obj'",
        )
    }

val sqlcipherDevice = sqlcipherTask("buildSqlcipherIosArm64", "iosArm64", "iphoneos", "-miphoneos-version-min=17.0")
val sqlcipherSim = sqlcipherTask("buildSqlcipherIosSimulatorArm64", "iosSimulatorArm64", "iphonesimulator", "-mios-simulator-version-min=17.0")

fun KotlinNativeTarget.enlazarSqlcipher(dir: String, tarea: TaskProvider<Exec>) {
    val lib = layout.buildDirectory.file("sqlcipher/$dir/libsqlcipher.a").get().asFile.path
    binaries.getTest("DEBUG").linkerOpts("-force_load", lib)
    binaries.all { linkTaskProvider.configure { dependsOn(tarea) } }
}

// Las reglas (shared/reglas/*.json) son las mismas que usa la app de Flutter. Se incrustan
// como texto en una clase generada para leerlas igual en Android, iOS y los tests, sin
// cargar archivos de cada plataforma.
val generarReglas = tasks.register("generarReglas") {
    group = "reglas"
    description = "Genera ReglasIncluidas.kt a partir de shared/reglas/*.json"
    val entrada = layout.projectDirectory.dir("reglas")
    val salida = layout.buildDirectory.dir("generated/reglas")
    inputs.dir(entrada)
    outputs.dir(salida)
    doLast {
        fun texto(nombre: String): String {
            val t = entrada.file(nombre).asFile.readText()
            check("\"\"\"" !in t && '$' !in t) { "$nombre no puede llevar \"\"\" ni \$" }
            return t
        }
        val archivo = salida.get().asFile.resolve("com/migasto/domain/reglas/ReglasIncluidas.kt")
        archivo.parentFile.mkdirs()
        archivo.writeText(
            "package com.migasto.domain.reglas\n\n" +
                "// Generado por la tarea generarReglas. No editar: cambia shared/reglas/*.json.\n" +
                "object ReglasIncluidas {\n" +
                "    val lector: String = \"\"\"${texto("reader_rules.json")}\"\"\"\n\n" +
                "    val categorias: String = \"\"\"${texto("category_rules.json")}\"\"\"\n" +
                "}\n",
        )
    }
}

kotlin {
    androidLibrary {
        namespace = "com.migasto.shared"
        compileSdk = 36
        minSdk = 24

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    iosArm64 { enlazarSqlcipher("iosArm64", sqlcipherDevice) }
    iosSimulatorArm64 { enlazarSqlcipher("iosSimulatorArm64", sqlcipherSim) }

    cocoapods {
        summary = "Lógica compartida de MiGasto"
        homepage = "https://github.com/VMichael1999/MiGasto"
        version = "0.1.0"
        ios.deploymentTarget = "17.0"

        framework {
            baseName = "Shared"
            isStatic = true
        }

        pod("SQLCipher") {
            version = "~> 4.6"
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generarReglas)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.sqlite)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("MiGastoDb") {
            packageName.set("com.migasto.db")
            // Foto de cada versión del esquema: sirve para comprobar las migraciones (.sqm).
            schemaOutputDirectory.set(file("schemas"))
            verifyMigrations.set(true)
        }
    }
}
