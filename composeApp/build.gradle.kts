plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.20"
}

kotlin {
    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework { baseName = "CinemaxKit"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.3.0")
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("androidx.media3:media3-exoplayer:1.8.0")
            implementation("androidx.media3:media3-ui:1.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.0")
        }
    }
}

android {
    namespace = "com.cinemax.app"
    compileSdk = 36
    defaultConfig { applicationId = "com.cinemax.app"; minSdk = 26; targetSdk = 36 }
}
