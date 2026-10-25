import java.util.Properties

// Llave de subida a Play: keystore.properties (fuera de git) apunta al .jks. Sin ese archivo el
// release sale sin firmar y Play Console lo rechaza.
val uploadKey: Properties? = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

fun localProperties(name: String): Properties =
    Properties().apply { rootProject.file(name).takeIf { it.exists() }?.inputStream()?.use(::load) }

// Supabase (supabase.properties) y clave de licencia de Play (play.properties), fuera de git; ver los .example.
// Sin ellos la app compila igual: sin cuenta y sin comprobar la firma de las compras.
val supabase = localProperties("supabase.properties")
val play = localProperties("play.properties")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.levelup"
    // AndroidX actual exige compilar contra 37; targetSdk 36 hasta probar los cambios de Android 17.
    compileSdk = 37

    defaultConfig {
        // Identificador en Play, elegido por el propietario. No se puede cambiar tras la primera subida.
        applicationId = "sack.levelup"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${supabase.getProperty("url", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supabase.getProperty("publishableKey", "")}\"")
        buildConfigField("String", "PLAY_LICENSE_KEY", "\"${play.getProperty("licenseKey", "")}\"")
    }

    signingConfigs {
        if (uploadKey != null) {
            create("upload") {
                storeFile = file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.play.billing)
    // Billing trae fragment 1.1, que rompe los selectores de archivos (ActivityResult necesita 1.3 o más).
    implementation(libs.androidx.fragment)
    // Solo lectura de pasos y ejercicio de hoy, y solo si se activa en Laboratorio.
    implementation(libs.health.connect)
    // Reloj: el teléfono le manda los retos de hoy y recibe los que se completan desde la muñeca.
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}
