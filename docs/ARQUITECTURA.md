# LevelUp — arquitectura

App de hábitos con retos y progreso tipo RPG. Kotlin Multiplatform + Compose Multiplatform,
publicada primero en Android. Todo funciona sin conexión y sin cuenta; la cuenta (Supabase) es opcional y
añade sincronización entre teléfonos y lo social.

## Decisiones de alcance (acordadas con el propietario el 2026-10-24)

| Tema | v1 | Por qué |
|---|---|---|
| Plataformas | Android. El código compartido solo usa librerías multiplataforma | iOS no se puede compilar ni probar en Windows. Pasos para añadirlo: [IOS.md](IOS.md) |
| Backend, cuentas, sincronización, social | **Sí, opcional**: Supabase con login por correo | El teléfono sigue siendo la fuente de verdad; sin cuenta nada sale de él |
| Copia de seguridad y teléfono nuevo | Exportar/importar archivo JSON + copia automática de Android | Cubre la recuperación sin servidor |
| Asistente de IA | No | Sin proveedor configurado no aporta nada |
| Misiones, cosméticos, rangos, títulos, logros/trofeos | Sí | |
| Compras | LevelUp Plus, compra única en Google Play con la firma comprobada en el teléfono | Elegido por el propietario (opción b). Plus es solo cosmético y estadísticas: nada necesario para progresar |
| Público | General, 13+, con enfoque en personas neurodivergentes | |

## Módulos

```
shared/      Kotlin Multiplatform (hoy solo el objetivo Android)
  commonMain/kotlin/app/levelup/
    domain/    reglas puras: XP y niveles, motor de retos, dificultad adaptativa, rachas,
               logros, misiones, recordatorios. Sin Compose; los modelos llevan anotaciones de Room
               (solo anotaciones, para no duplicar clases) y se prueban con kotlin.test
    content/   catálogo local de retos, logros, misiones y cosméticos (código Kotlin, versionado)
    data/      Room (entidades, DAOs, base de datos), repositorio, respaldo JSON y motor de sincronización (Sync.kt)
    cloud/     cliente de Supabase (Cloud.kt) y servicio que une la cuenta con la base local (CloudService.kt)
    ui/        Compose: tema, componentes, pantallas, navegación; LevelUpViewModel y CloudViewModel (cuenta y social)
    App.kt     punto de entrada de la interfaz; recibe `Platform` con lo que es propio de cada sistema
  androidMain/ base de datos para Android, sesión de la cuenta en preferencias, firma de Play
androidApp/  Activity, Application, recordatorios y sincronización con WorkManager, compra de Plus, manifiesto
supabase/    migraciones SQL, pruebas pgTAP, plantillas de correo y config.toml (CLI de Supabase)
```

Sin framework de inyección de dependencias: `LevelUpApplication` crea la base de datos, el repositorio,
`AndroidPlatform` y (si la compilación tiene `supabase.properties`) el `CloudService` una sola vez, y el ViewModel los recibe por constructor. `AndroidPlatform` vive en la
Application (no en la actividad) para que el ViewModel no retenga una actividad muerta al girar la pantalla.

## Datos (Room, local primero)

La base de datos del teléfono es la fuente de verdad. Las pantallas leen `Flow`s de Room y nunca esperan a
la red; lo único que se pide al servidor al abrir una pantalla son los datos sociales.

| Tabla | Qué guarda |
|---|---|
| `preferences` | una fila: objetivos, ritmo, accesibilidad, recordatorios, descanso, estado del onboarding |
| `custom_categories` | categorías creadas por el usuario |
| `custom_challenges` | retos propios y hábitos (reto con días de la semana) |
| `challenge_instances` | cada reto concreto de un día, con copia del título, XP y categoría para que el historial no cambie si el catálogo cambia |
| `xp_ledger` | movimientos de XP. `UNIQUE(source_type, source_id)`: la misma acción nunca suma dos veces |
| `unlocked_achievements` | logros desbloqueados y su fecha |
| `mission_runs` | misiones empezadas, completadas o vencidas |
| `pauses` | periodos de pausa (no rompen la racha) |
| `sync_outbox` | cambios locales pendientes de subir (una fila por registro, la llenan triggers de SQLite) |
| `sync_state` | una fila: cuenta enlazada, cursor de descarga, último error |

- **Definición vs. instancia:** un reto del catálogo o propio es la definición; `challenge_instances`
  es lo que hay que hacer un día concreto. `UNIQUE(challenge_id, date)` evita duplicados del mismo día.
- **Transacciones:** completar un reto marca la instancia, inserta el XP y desbloquea logros/misiones
  en una sola transacción de Room. Si la app se cierra a medias, no queda nada a medio escribir.
- **Derivado, no guardado:** nivel, nivel por categoría, rachas, estadísticas y cosméticos desbloqueados se
  calculan desde las tablas; no se duplican.
- **IDs:** UUID (`kotlin.uuid.Uuid`) para lo que crea el usuario; los retos del catálogo tienen ID de texto
  estable. Lo que dos teléfonos pueden crear a la vez lleva ID determinista para que se junte y no se
  duplique: instancia `fecha|reto`, XP `tipo:origen`, misión `misión|inicio`, pausa = fecha de inicio.
- **Migraciones:** Room con esquemas exportados en `shared/schemas/`. Cada cambio de esquema sube
  `version`, añade una `Migration` y una prueba que la ejecuta. Nunca `fallbackToDestructiveMigration`.
- **Borrado:** "Borrar todos mis datos" vacía todas las tablas. Borrar un reto propio no borra el
  historial de lo ya completado.

## Respaldo y recuperación

1. **Copia automática de Android** (`allowBackup="true"`, incluye la base de datos): Android la guarda en la
   cuenta de Google del usuario y la restaura en un teléfono nuevo. `res/xml/` excluye la sesión de la cuenta
   (`levelup_session.xml`) y dos cachés que se rehacen solas.
2. **Exportar/importar**: archivo JSON legible con todas las tablas y `schemaVersion`. Importar valida el
   archivo completo antes de tocar nada y reemplaza los datos en una sola transacción.

## Reglas de juego

Todas en `domain/`, con pruebas. Detalle y números en [REGLAS.md](REGLAS.md).

## Recordatorios

Un solo trabajo de WorkManager (`ReminderWork`) programado a la hora elegida. Al ejecutarse pregunta al
repositorio (`reminderNow`) qué decir según `Reminders.decide` —que calla en horas de silencio, descansos y
pausas— y programa el del día siguiente. Cambiar las preferencias lo reprograma.

## Accesibilidad

Preferencias combinables y disponibles siempre (Perfil → Accesibilidad): tamaño de texto, modo sencillo,
reducir movimiento (sigue la opción del sistema por defecto), alto contraste, tema, vibración, pasos
visibles y temporizador opcional. Todo control tiene etiqueta para TalkBack, área táctil mínima de 48 dp,
y ningún estado se comunica solo con color.

## Cuenta y sincronización (Supabase)

- **Login** solo por correo y contraseña (mín. 8, letras y números). Confirmación y cambio de contraseña con
  un código de 6 dígitos por correo (plantillas en `supabase/templates/`). La sesión vive en
  `SharedPreferences` propias (`PrefsSessionManager`) y no entra en la copia de Android.
- **Servidor:** una tabla genérica `user_records (user_id, entity, id, data jsonb, client_updated_at, deleted, seq)`
  con RLS por usuario. *shortcut:* el progreso viaja como JSON por entidad en vez de tablas normalizadas; si
  algún día el servidor tiene que calcular sobre ese progreso, normalizar esas entidades.
- **Subida:** triggers de SQLite apuntan cada cambio en `sync_outbox` (solo con cuenta enlazada). El RPC
  `push_records` aplica *el último que escribe gana* por la marca de tiempo del cliente, con `>` estricto
  (empate: se queda lo guardado), y numera cada cambio con `seq` bajo un bloqueo por usuario.
- **Bajada:** `user_records` con `seq > cursor`, en páginas de 500. Lo que tiene cambios locales sin subir no
  se pisa.
- **Al entrar:** lo local se marca pendiente con sus marcas de tiempo reales (0 en preferencias y pausas),
  así lo de invitado nunca pisa algo más nuevo de la cuenta. Si el teléfono estaba enlazado a otra cuenta,
  primero se borra lo local. **Al salir** se borra lo local (sigue en la cuenta). **Al borrar la cuenta** lo
  local se queda como invitado. Importar un respaldo con sesión reemplaza también lo de la cuenta.
- **Cuándo:** `SyncWork` (WorkManager) sube 5 s después de cada cambio, con red y reintentos, y cada 6 horas
  mientras haya sesión; también al abrir la app.

## Social

Tablas normalizadas con RLS; todo lo que escribe pasa por funciones `security definer` con límites de uso:
perfiles (nombre de usuario único, opciones de visibilidad), solicitudes, amistades, bloqueos (el servidor
los aplica en todas las funciones), reacciones predefinidas (sin texto libre), retos en grupo (cooperativo o
competitivo, hasta 60 días; el progreso lo cuenta el servidor desde `user_records`), clasificación semanal
de amigos voluntaria (tope de 600 XP por día), avisos (se borran a los 90 días con `pg_cron`) y denuncias.
`delete_account()` borra el usuario de `auth.users` y todo cae en cascada.

Pruebas: `supabase test db` (pgTAP, 40 casos: sincronización, RLS, amistades, bloqueos, grupos, borrado) y
`CloudIntegrationTest`, que pone la app de verdad (Room, `SyncEngine`, `Cloud`) contra el Supabase local: registro
con el código del correo (lo lee de Mailpit), dos teléfonos, amistad, avisos, grupo, clasificación y borrado.
Solo corre con `-PsupabaseLocalKey=<clave publicable de supabase status>`.

## LevelUp Plus

Compra única `levelup_plus` (Play Billing en `PlusBilling`). Desbloquea 4 marcos, 6 colores de la app y
estadísticas ampliadas (`ExtendedStats`). La compra se confirma (acknowledge) y su firma se comprueba con la
clave de licencia de `play.properties`. *shortcut:* la comprobación es en el teléfono; si el pirateo importa,
verificar el token con la Google Play Developer API desde el servidor. Sin Plus (o tras un reembolso) se
vuelve al color de siempre sin tocar la preferencia guardada.

## iOS

No se compila ni se prueba en Windows. Pasos en [IOS.md](IOS.md); la parte de Supabase y la base de datos
ya es multiplataforma, faltan la sesión, las compras (StoreKit) y el trabajo en segundo plano.
