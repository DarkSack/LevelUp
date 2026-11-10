// Versiones corregidas de librerías con alertas de Dependabot (noviembre de 2026). Casi todas llegan
// por los plugins (AGP, Room, KSP) y por lint, solo para compilar; Guava además va dentro de la app.
// El classpath de los plugins va aquí y el de cada módulo, abajo en allprojects.
buildscript {
    dependencies {
        constraints {
            classpath("com.google.guava:guava:33.7.2-jre")
            classpath("org.bouncycastle:bcprov-jdk18on:1.85")
            classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
            classpath("org.bouncycastle:bcutil-jdk18on:1.85")
            classpath("org.bitbucket.b_c:jose4j:0.9.6")
            classpath("org.jdom:jdom2:2.0.6.1")
            classpath("org.apache.commons:commons-lang3:3.21.0")
            classpath("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

val parches = mapOf(
    "com.google.guava:guava" to "33.7.2-jre",
    "org.bouncycastle:bcprov-jdk18on" to "1.85",
    "org.bouncycastle:bcpkix-jdk18on" to "1.85",
    "org.bouncycastle:bcutil-jdk18on" to "1.85",
    "org.bitbucket.b_c:jose4j" to "0.9.6",
    "org.jdom:jdom2" to "2.0.6.1",
    "org.apache.commons:commons-lang3" to "3.21.0",
    "org.apache.httpcomponents:httpclient" to "4.5.14",
)

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            parches["${requested.group}:${requested.name}"]?.let { useVersion(it); because("alerta de Dependabot") }
        }
    }
}
