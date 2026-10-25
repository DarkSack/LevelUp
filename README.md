# LevelUp

App de hábitos con retos pequeños y progreso tipo RPG: experiencia, niveles, títulos, logros, misiones y
marcos para el avatar. Funciona sin internet y sin cuenta; con cuenta (opcional) sincroniza teléfonos y
añade amigos, retos en grupo y clasificación. Kotlin Multiplatform + Compose Multiplatform, primero en Android.

## Compilar y probar

Requiere JDK 21 y el SDK de Android (ruta en `local.properties`, `sdk.dir=...`). Archivos locales fuera de
git, cada uno con su `.example`:

- `supabase.properties`: URL y clave publicable de Supabase. Sin él la app compila sin cuenta.
- `play.properties`: clave de licencia de Play para comprobar las compras de Plus.
- `keystore.properties`: llave de subida a Play. Sin ella el release sale sin firmar.

```bash
./gradlew :shared:testAndroidHostTest     # dominio, datos, sincronización e interfaz (Robolectric)
./gradlew :androidApp:assembleDebug       # APK de depuración
./gradlew :androidApp:bundleRelease       # AAB para Play
```

Backend (necesita Docker y la CLI de Supabase):

```bash
supabase start      # Supabase local
supabase db reset   # aplica supabase/migrations
supabase test db    # pruebas pgTAP del esquema, RLS y funciones
```

La app contra ese Supabase local (registro, sincronización y lo social de punta a punta):

```bash
./gradlew :shared:testAndroidHostTest --tests "*CloudIntegrationTest" -PsupabaseLocalKey=<PUBLISHABLE_KEY de supabase status>
```

## Estructura

- `shared/`: dominio, catálogo de retos, base de datos (Room), sincronización, cliente de Supabase, interfaz (Compose) y pruebas.
- `androidApp/`: actividad, recordatorios y sincronización con WorkManager, compra de Plus, manifiesto e iconos.
- `supabase/`: migraciones, pruebas y plantillas de correo del backend.
- `docs/`: [especificación original](docs/PROMPT_MAESTRO.md), [arquitectura](docs/ARQUITECTURA.md), [reglas del juego](docs/REGLAS.md),
  [pendientes del propietario](docs/PENDIENTES.md), [cómo añadir iOS](docs/IOS.md) y [material para Play](docs/play/).
