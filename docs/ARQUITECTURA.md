# LevelUp — arquitectura

App de hábitos con retos y progreso tipo RPG. Kotlin Multiplatform + Compose Multiplatform,
publicada primero en Android. Todo funciona sin conexión y sin cuenta.

## Decisiones de alcance (v1, acordadas con el propietario el 2026-10-24)

| Tema | v1 | Por qué |
|---|---|---|
| Plataformas | Android. El código compartido solo usa librerías multiplataforma | iOS no se puede compilar ni probar en Windows. Pasos para añadirlo: [IOS.md](IOS.md) |
| Backend, cuentas, sincronización, social | **No**. Todo vive en el teléfono | Es la parte más grande y solo hace falta para lo social o para usar varios dispositivos. Queda para la v2 |
| Copia de seguridad y teléfono nuevo | Exportar/importar archivo JSON + copia automática de Android | Cubre la recuperación sin servidor |
| Asistente de IA | No | Sin proveedor configurado no aporta nada |
| Misiones, cosméticos, rangos, títulos, logros/trofeos | Sí | |
| Compras | Pendiente de decisión del propietario | Ver docs/PENDIENTES.md |

## Módulos

```
shared/      Kotlin Multiplatform (hoy solo el objetivo Android)
  commonMain/kotlin/app/levelup/
    domain/    reglas puras: XP y niveles, motor de retos, dificultad adaptativa, rachas,
               logros, misiones, recordatorios. Sin Compose; los modelos llevan anotaciones de Room
               (solo anotaciones, para no duplicar clases) y se prueban con kotlin.test
    content/   catálogo local de retos, logros, misiones y cosméticos (código Kotlin, versionado)
    data/      Room (entidades, DAOs, base de datos), repositorio y respaldo JSON
    ui/        Compose: tema, componentes, pantallas, navegación y un único ViewModel
    App.kt     punto de entrada de la interfaz; recibe `Platform` con lo que es propio de cada sistema
  androidMain/ constructor de la base de datos para Android
androidApp/  Activity, Application, recordatorios con WorkManager, manifiesto, iconos
```

Sin framework de inyección de dependencias: `LevelUpApplication` crea la base de datos, el repositorio y
`AndroidPlatform` una sola vez, y el ViewModel los recibe por constructor. `AndroidPlatform` vive en la
Application (no en la actividad) para que el ViewModel no retenga una actividad muerta al girar la pantalla.

## Datos (Room, local primero)

La base de datos del teléfono es la única fuente de verdad. Las pantallas leen `Flow`s de Room, nunca
esperan a la red (no hay red).

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

- **Definición vs. instancia:** un reto del catálogo o propio es la definición; `challenge_instances`
  es lo que hay que hacer un día concreto. `UNIQUE(challenge_id, date)` evita duplicados del mismo día.
- **Transacciones:** completar un reto marca la instancia, inserta el XP y desbloquea logros/misiones
  en una sola transacción de Room. Si la app se cierra a medias, no queda nada a medio escribir.
- **Derivado, no guardado:** nivel, nivel por categoría, rachas, estadísticas y cosméticos desbloqueados se
  calculan desde las tablas; no se duplican.
- **IDs:** UUID (`kotlin.uuid.Uuid`) para todo lo que crea el usuario; los retos del catálogo tienen ID de texto estable.
- **Migraciones:** Room con esquemas exportados en `shared/schemas/`. Cada cambio de esquema sube
  `version`, añade una `Migration` y una prueba que la ejecuta. Nunca `fallbackToDestructiveMigration`.
- **Borrado:** "Borrar todos mis datos" vacía todas las tablas. Borrar un reto propio no borra el
  historial de lo ya completado.

## Respaldo y recuperación

1. **Copia automática de Android** (`allowBackup="true"`, reglas por defecto: incluye la base de datos):
   Android la guarda en la cuenta de Google del usuario y la restaura al instalar en un teléfono nuevo.
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

## v2 (fuera de esta versión)

Supabase (cuentas, sincronización con cola persistente e idempotente, RLS), social (amigos, bloqueos, retos
en grupo, clasificaciones voluntarias) e iOS. El diseño actual ya lo permite: IDs UUID, ledger de XP
idempotente y copia de datos por instancia.
