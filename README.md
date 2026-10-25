# LevelUp

App de hábitos con retos pequeños y progreso tipo RPG: experiencia, niveles, títulos, logros, misiones y
marcos para el avatar. Funciona sin internet y sin cuenta. Kotlin Multiplatform + Compose Multiplatform;
v1 se publica en Android.

## Compilar y probar

Requiere JDK 21 y el SDK de Android (ruta en `local.properties`, `sdk.dir=...`).

```bash
./gradlew :shared:testAndroidHostTest     # dominio, datos e interfaz (Robolectric)
./gradlew :androidApp:assembleDebug       # APK de depuración
./gradlew :androidApp:bundleRelease       # AAB para Play (necesita keystore.properties)
```

## Estructura

- `shared/`: dominio, catálogo de retos, base de datos (Room), interfaz (Compose) y pruebas.
- `androidApp/`: actividad, recordatorios con WorkManager, manifiesto e iconos.
- `docs/`: [especificación original](docs/PROMPT_MAESTRO.md), [arquitectura](docs/ARQUITECTURA.md), [reglas del juego](docs/REGLAS.md),
  [pendientes del propietario](docs/PENDIENTES.md), [cómo añadir iOS](docs/IOS.md) y [material para Play](docs/play/).
