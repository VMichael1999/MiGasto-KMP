plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.sqldelight)
}

kotlin {
    iosArm64()
    iosSimulatorArm64 {
        binaries.getTest("DEBUG").linkerOpts(
            "-force_load",
            layout.buildDirectory.file("sqlcipher/iosSimulatorArm64/libsqlcipher.a").get().asFile.path,
        )
    }

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
        commonTest.dependencies {
            implementation(libs.kotlin.test)
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
        }
    }
}
