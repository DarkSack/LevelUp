import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Mascotas animadas dibujadas con código (Canvas de Compose): sin imágenes ni dependencias de la app.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.levelup.pets"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
