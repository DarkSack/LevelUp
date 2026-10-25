# LevelUp

App de hábitos con retos pequeños y progreso tipo RPG: experiencia, niveles, títulos, logros, misiones y
marcos para el avatar. Funciona sin internet y sin cuenta. Con una cuenta (opcional) sincroniza varios
teléfonos y añade amigos, retos en grupo y clasificación. Hecha con Kotlin Multiplatform y Compose
Multiplatform; por ahora solo para Android.

Pensada para el público general (13+), y en especial para personas neurodivergentes: retos cortos, sin
castigos por los días sin actividad, sin presión para volver y con opciones de accesibilidad desde el primer
momento.

## Estado

Versión `0.1.0` (`versionCode 1`), **sin publicar**. El código está completo para una primera versión y probado
con pruebas automáticas. Nunca se ha instalado en un teléfono real.

- **Backend en producción:** desplegado en Supabase. Crear una cuenta todavía no funciona porque falta
  configurar un servidor de correo propio (SMTP). Detalles en [docs/PENDIENTES.md](docs/PENDIENTES.md).
- **Qué falta respecto a la especificación:** [docs/COBERTURA.md](docs/COBERTURA.md).
- **Fuera de esta versión:** iOS y el asistente de IA.

## Qué hace

- **Retos del día:** más de 120 retos en 9 categorías (físico, social, mente y aprendizaje, productividad,
  bienestar, creatividad, finanzas, digital y hogar). Los elige según tus objetivos, la dificultad y el tiempo
  que tienes. Puedes cambiar un reto por otro, dejarlo para otro día o pedir una versión más fácil.
- **Retos propios:** creas los tuyos, con pasos y días de repetición; se pueden pausar y archivar.
- **Dificultad que se adapta:** al terminar un reto dices si fue fácil, justo o difícil, y la app ajusta la
  dificultad poco a poco.
- **Progreso RPG:** XP y nivel global, nivel por categoría, rachas con días de descanso, objetivo semanal,
  logros, misiones, títulos y marcos. Lo ganado nunca se pierde.
- **Accesibilidad:** tamaño de texto, alto contraste, menos movimiento, modo sencillo, vibración, pasos visibles,
  cronómetro opcional y nivel de gamificación ajustable.
- **Recordatorios:** opcionales, a la hora que elijas, con horas de silencio y sin mensajes de culpa.
- **Tus datos:** exportar e importar un respaldo JSON, borrar todo, y la copia automática de Android.
- **Cuenta (opcional):** registro por correo con código de 6 cifras. Sincroniza entre teléfonos y permite cerrar
  la sesión en los demás dispositivos o borrar la cuenta desde la app.
- **Social (con cuenta):** amigos, solicitudes, bloqueos, reacciones predefinidas (sin chat), retos en grupo,
  clasificación semanal voluntaria, avisos y denuncias. Cada opción de privacidad se puede apagar.
- **LevelUp Plus:** compra única en Google Play con marcos, colores y estadísticas ampliadas. Es solo cosmético:
  nada de Plus hace falta para progresar.

## Privacidad

Sin cuenta, nada sale del teléfono. Con cuenta, el progreso se guarda también en Supabase, protegido por
reglas que solo dejan a cada usuario leer lo suyo. Lo social solo comparte lo que el usuario activa. La app no
tiene anuncios ni analítica. La política completa está en [docs/play/privacidad.md](docs/play/privacidad.md).

## Tecnología

| Parte | Qué usa |
|---|---|
| Lenguaje e interfaz | Kotlin 2.4.21, Compose Multiplatform 1.12.2 (Material 3) |
| Base de datos local | Room 2.8.5 (KMP), con migraciones probadas |
| Backend | Supabase (Postgres con RLS, Auth, `pg_cron`) con supabase-kt 3.8.0 |
| Tareas en segundo plano | WorkManager (recordatorios y sincronización) |
| Compras | Google Play Billing 9.1.0, con la firma comprobada en el teléfono |
| Pruebas | kotlin.test, JUnit 4, Robolectric 4.17, pgTAP |
| Compilación | Gradle 9.7.1, AGP 9.4.1, JDK 21; `minSdk 26`, `targetSdk 36`, `compileSdk 37` |

Paquete: `sack.levelup`. No se puede cambiar después de la primera subida a Play.

## Puesta en marcha

Requisitos: JDK 21 y el SDK de Android. La ruta del SDK va en `local.properties` (`sdk.dir=...`).

Tres archivos locales quedan fuera de git. Cada uno tiene su `.example`, salvo `keystore.properties`:

| Archivo | Para qué | Si no está |
|---|---|---|
| `supabase.properties` | URL y clave **publicable** de Supabase (nunca la secreta ni la `service_role`) | La app compila sin cuenta ni funciones sociales |
| `play.properties` | Clave de licencia de Play para comprobar las compras de Plus | Las compras no se comprueban |
| `keystore.properties` | Ruta y contraseñas de la llave de subida a Play (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) | El AAB de release sale sin firmar |

```bash
cp supabase.properties.example supabase.properties
cp play.properties.example play.properties
```

Para probar contra el Supabase local desde un emulador, usa `url=http://10.0.2.2:54321` en
`supabase.properties`.

## Compilar

```bash
./gradlew :androidApp:assembleDebug       # APK de depuración
./gradlew :androidApp:bundleRelease       # AAB para Play (firmado si existe keystore.properties)
```

## Pruebas

```bash
./gradlew :shared:testAndroidHostTest     # dominio, datos, sincronización e interfaz (Robolectric)
```

Cubren las reglas del juego (XP, niveles, selección de retos, dificultad, rachas, logros, recordatorios), la
base de datos y su migración, la sincronización entre dos teléfonos simulados, la firma de las compras y las
pantallas principales.

Backend (necesita Docker y la [CLI de Supabase](https://supabase.com/docs/guides/local-development)):

```bash
supabase start      # Supabase local
supabase db reset   # aplica supabase/migrations
supabase test db    # pruebas pgTAP del esquema, RLS y funciones
```

La app contra ese Supabase local, de punta a punta (registro con el código que llega a Mailpit, sincronización
entre dos teléfonos, amigos, grupos y borrado de cuenta). Sin la clave, esta prueba se salta:

```bash
./gradlew :shared:testAndroidHostTest --tests "*CloudIntegrationTest" -PsupabaseLocalKey=<PUBLISHABLE_KEY de supabase status>
```

## Integración continua

[GitHub Actions](.github/workflows) corre las pruebas y compila el APK de depuración en cada push a `main` y en
cada pull request. No publica nada en Play.

## Backend

El esquema vive en [supabase/migrations](supabase/migrations). Las pruebas están en [supabase/tests](supabase/tests)
y las plantillas de correo en [supabase/templates](supabase/templates). Hay dos tipos de datos:

- `user_records`: copia por usuario de la base local. Gana el último que escribe y se descarga en orden por
  número de secuencia.
- **Tablas sociales:** perfiles, amistades, bloqueos, reacciones, grupos, clasificación, avisos y denuncias.
  Todo lo que toca a otra persona pasa por funciones del servidor que validan y limitan el uso.

Para aplicar migraciones al proyecto real: `supabase login`, `supabase link --project-ref <ref>` y
`supabase db push`. Los pasos que faltan del lado del propietario (correo propio, moderación, plan gratis)
están en [docs/PENDIENTES.md](docs/PENDIENTES.md).

## Publicar en Google Play

[docs/play/PLAY_STORE.md](docs/play/PLAY_STORE.md) tiene la ficha en español, la clasificación de contenido,
la seguridad de los datos y los gráficos necesarios. Lo que hay que hacer en Play Console está en
[docs/PENDIENTES.md](docs/PENDIENTES.md).

## Estructura

```
shared/       código común: dominio, catálogo, Room, sincronización, cliente de Supabase, interfaz y pruebas
androidApp/   actividad, recordatorios y sincronización con WorkManager, compra de Plus, manifiesto e iconos
supabase/     migraciones, pruebas pgTAP, plantillas de correo y config.toml
docs/         especificación, arquitectura, reglas, pendientes y material para Play
```

## Documentación

| Documento | Contenido |
|---|---|
| [PROMPT_MAESTRO.md](docs/PROMPT_MAESTRO.md) | Especificación original del producto |
| [COBERTURA.md](docs/COBERTURA.md) | Qué está hecho y qué falta de la especificación |
| [ARQUITECTURA.md](docs/ARQUITECTURA.md) | Módulos, datos, sincronización, social y Plus |
| [REGLAS.md](docs/REGLAS.md) | Fórmulas de XP, selección de retos, dificultad, rachas y logros |
| [PENDIENTES.md](docs/PENDIENTES.md) | Decisiones tomadas y tareas del propietario |
| [IOS.md](docs/IOS.md) | Cómo añadir la versión de iOS |
| [play/](docs/play/) | Ficha de Play y política de privacidad |

## Contacto

Johan Jafet Del Valle Santiago · donsackito@protonmail.com
