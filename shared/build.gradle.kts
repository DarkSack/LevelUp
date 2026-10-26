import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    // Room genera un `actual object` para el constructor de la base de datos.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    android {
        namespace = "app.levelup.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest { isIncludeAndroidResources = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    // iOS: todo lo de commonMain usa librerías multiplataforma. Para añadirlo, ver docs/IOS.md.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
        }
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.framework)
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}

room {
    schemaDirectory("$projectDir/schemas")
}


apply(from = rootProject.file("gradle/robolectric-offline.gradle.kts"))

// Prueba contra el Supabase local (CloudIntegrationTest): -PsupabaseLocalKey=<clave publicable de `supabase status`>.
// Capturas para la ficha de Play (StoreScreenshots): solo corren con -Pcapturas=<carpeta>.
tasks.withType<Test>().configureEach {
    providers.gradleProperty("supabaseLocalKey").orNull?.let { systemProperty("supabaseLocalKey", it) }
    providers.gradleProperty("capturas").orNull?.let { systemProperty("levelup.capturas", it) }
}
