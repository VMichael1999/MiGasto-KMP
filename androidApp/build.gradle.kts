import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ktlint)
}

// La clave de Google Maps va en local.properties (GOOGLE_MAPS_API_KEY=...) o en la variable de entorno del mismo
// nombre; no se guarda en el repositorio. Sin clave la app funciona y, donde iría el mapa, lo explica.
val claveDeMapas: String = Properties().also { p -> rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(p::load) }
    .getProperty("GOOGLE_MAPS_API_KEY") ?: System.getenv("GOOGLE_MAPS_API_KEY") ?: ""

android {
    namespace = "com.migasto.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.migasto"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["claveDeMapas"] = claveDeMapas
    }

    // Maps Compose está compilado para Java 17: la app tiene que usar el mismo destino para poder inlinar su código.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.maps.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}
