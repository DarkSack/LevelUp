# Añadir iOS

v1 se publica solo en Android porque iOS exige un Mac con Xcode para compilar, firmar y probar. El código
de `shared/` (dominio, contenido, datos e interfaz) ya usa solo librerías multiplataforma, así que añadir
iOS es trabajo de plataforma, no reescribir la app. Nada de esto se ha probado todavía.

## 1. Objetivos de iOS en `shared/build.gradle.kts`

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "Shared"; isStatic = true }
    }
    sourceSets {
        iosMain.dependencies { implementation("androidx.sqlite:sqlite-bundled:<versión de androidx.sqlite>") }
    }
}

dependencies {
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
```

## 2. Lo propio de iOS (`shared/src/iosMain/kotlin/app/levelup/`)

| Pieza | Qué hacer |
|---|---|
| Base de datos | `Room.databaseBuilder<AppDatabase>("<Documents>/levelup.db").setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()` con la ruta de `NSDocumentDirectory` |
| `Platform` | `appVersion` de `NSBundle`, `systemReducedMotion` = `UIAccessibilityIsReduceMotionEnabled()`, exportar/importar con `UIDocumentPickerViewController`, permiso con `UNUserNotificationCenter.requestAuthorization` |
| Recordatorios | iOS no ejecuta código a una hora exacta. Al abrir la app y al cambiar preferencias, programar con `UNCalendarNotificationTrigger` los avisos de los próximos 7 días usando `Reminders.decide` (sin el aviso de regreso, que depende de días sin abrir la app; ese puede ir como aviso único a 3 días vista que se reprograma cada vez que se abre) |
| Punto de entrada | `fun MainViewController() = ComposeUIViewController { App(repo, IosPlatform()) }`, con el repositorio creado una sola vez |

## 3. Proyecto Xcode (`iosApp/`)

Crear una app SwiftUI que muestre `MainViewControllerKt.MainViewController()` dentro de un
`UIViewControllerRepresentable`, y una fase de compilación que ejecute
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`. Identificador de paquete, equipo de firma y nombre
de la app los decide el propietario (ver [PENDIENTES.md](PENDIENTES.md)).

## 4. Pruebas

`./gradlew :shared:iosSimulatorArm64Test` ejecuta en el simulador las mismas pruebas de `commonTest`
(dominio, catálogo, respaldo). Las de `androidHostTest` (repositorio y UI con Robolectric) son solo de Android.

## 5. App Store

- Privacidad (etiquetas de App Store Connect): "Datos no recopilados", igual que en Play.
- Copia de seguridad: los datos de la app entran en la copia de iCloud del dispositivo si el usuario la tiene activada; exportar/importar funciona igual que en Android.
- Probablemente el nombre "LevelUp" ya esté ocupado en la App Store: hace falta otro nombre visible.
