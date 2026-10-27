# Pendientes del propietario

Lo que el código no puede resolver solo: cuentas, datos legales, despliegue y pruebas en un teléfono.

## Decisiones tomadas (2026-10-24)

| Tema | Decisión |
|---|---|
| Identificador del paquete | `sack.levelup` (no se puede cambiar tras la primera subida) |
| Nombre en la tienda | LevelUp |
| Compras | LevelUp Plus, compra única en Play, firma comprobada en el teléfono (opción b) |
| Público objetivo | General, 13+, con enfoque en personas neurodivergentes |
| Desarrollador y contacto | Johan Jafet Del Valle Santiago · donsackito@protonmail.com |
| Login | Solo correo y contraseña |
| Llave de subida | Generada: `C:\Users\Sack\.android-keys\levelup-upload.jks`, alias `upload`, contraseña en `keystore.properties` (fuera de git). **Respáldala fuera del equipo**: sin ella no se pueden publicar actualizaciones (salvo pedir a Google un cambio de llave) |

## Backend (Supabase)

El proyecto real `prcfmlvrzygpipifzmju` está en la cuenta de Supabase que también tiene **RPGRoll-Shop**. Desde el
2026-10-26 la CLI de este equipo tiene sesión en esa cuenta y el repo está enlazado (`supabase db push` sube las
migraciones nuevas sin pedir contraseña). `supabase.properties` (fuera de git) apunta
a él, así que las compilaciones salen conectadas a producción.

**Desplegado el 2026-10-25 por Composio:**

- Migración aplicada (`supabase/migrations/20261025142826_inicial.sql`; el archivo se renombró a la versión que
  registró Supabase, así `supabase db push` no la repite). Comprobado: 10 tablas con RLS, 16 políticas, el rol
  anónimo sin acceso a tablas ni funciones (probado desde fuera: «permiso denegado»), tarea de limpieza de
  `pg_cron` programada.
- Login: código de 6 cifras y contraseña de 8 con letras y números.
- Avisos de seguridad de Supabase: ninguno grave. Avisa de que las funciones `security definer` se pueden
  llamar con sesión (es a propósito: son la API de la app y cada una valida a quién afecta) y de que `reports`
  no tiene políticas (a propósito: solo se lee desde el panel).

**Bloqueante: correo propio (SMTP).** En el plan gratis, con el remitente de serie, Supabase **no deja cambiar
las plantillas** (lo rechazó al intentarlo) y solo envía 2 correos por hora. Las plantillas de serie mandan un
enlace en inglés, no el código de 6 cifras que pide la app, así que hoy en producción **no se puede crear cuenta
ni cambiar la contraseña desde la app**. Mientras tanto la 0.1.0 sale sin cuenta (`cuenta=no` en
`supabase.properties`, ver [PLAY_STORE.md](play/PLAY_STORE.md) §9 para volver a activarla). Para arreglarlo:

1. Crear una cuenta en un proveedor de correo (Resend, Brevo, Amazon SES…) y verificar un remitente. Con un
   dominio propio (por ejemplo `no-reply@tu-dominio`) los correos llegan mejor que con una dirección de Gmail o
   Proton, que suelen acabar en spam o rechazados cuando los envía otro servidor.
2. En Supabase → Authentication → Emails → SMTP Settings: poner los datos del proveedor (la contraseña SMTP no
   debe pasar por el chat) y subir el límite de correos por hora (Authentication → Rate Limits).
3. Subir solo las plantillas de `supabase/templates/` y sus asuntos (pedírmelo: van por la API de gestión de
   Supabase, campo a campo). **Nunca `supabase config push`**: sube todo `config.toml`, que es la configuración
   local, y pisaría el auth de producción (URLs, límites, proveedores).

**Migración `extras` aplicada en producción el 2026-10-25 por Composio** (`supabase/migrations/20261025161831_extras.sql`,
renombrada a la versión que registró Supabase): favoritos y metas en la sincronización, XP de regreso, mensajes de
ánimo, avatar/título/trofeos del perfil e historial de grupos. Comprobado en producción: las tres restricciones
nuevas, las columnas del perfil, `touch_profile` de 3 argumentos y `my_group_history` solo para `authenticated`, y
`profile_card` sin acceso directo. Avisos de seguridad: los mismos de siempre (funciones `security definer` que
validan a quien llama).

**Migración `sesiones` aplicada en producción el 2026-10-26 con la CLI** (`supabase db push`): `my_sessions` y
`end_session` para la lista de dispositivos. Comprobado desde fuera: existen y el rol anónimo recibe «permiso
denegado». Las tres migraciones locales coinciden con las de producción (`supabase migration list --linked`).

Otros:

- Plan gratis: esa organización ya tiene 2 proyectos activos (RPGRoll-Shop y LevelUp), el máximo gratis, y un
  proyecto gratis se pausa tras 7 días sin uso.
- CLI: `supabase migration list --linked` para comparar y `supabase db push --dry-run` antes de `supabase db push`.
  **No usar `supabase config push`**: subiría la configuración local de `config.toml` (URL del sitio en localhost,
  etc.) encima de la de producción.

## Google Play Console

- Crear la app con el paquete `sack.levelup`.
- **Producto integrado** con id `levelup_plus` (Monetizar → Productos → Productos integrados), compra única,
  y su precio (lo decides tú; la app muestra el que dé Play).
- **Clave de licencia** (Monetizar → Configuración de monetización): copiarla en `play.properties`
  (`licenseKey=...`, ver `play.properties.example`). Sin ella las compras no se comprueban.
- Cuenta de pagos de comerciante (necesaria para vender Plus).
- Probar la compra con una cuenta de prueba de licencias antes de publicar.
- **Declaración de Health Connect** (Contenido de la app): la app declara `READ_STEPS` y `READ_EXERCISE`. Si no
  quieres pasar esa revisión en la primera versión, se pueden quitar del manifiesto y la opción desaparece sola
  del Laboratorio (dímelo y lo hago).
- **Reloj**: subir el AAB de `wearApp` al canal de Wear OS de la misma ficha, con capturas del reloj
  ([PLAY_STORE.md](play/PLAY_STORE.md) §5).

## Datos legales y enlaces

- ~~Alojar la política de privacidad~~: hecho, https://shop.sackito.online/privacidad/levelup (2026-10-26). Sirve
  también como **URL para solicitar el borrado de la cuenta**: su sección «Tus derechos» explica cómo pedirlo por correo.

## Moderación

Las denuncias quedan en la tabla `reports` (estado `abierta`). No hay panel: se revisan en el editor de
tablas de Supabase. Para actuar: cambiar el nombre visible ofensivo o borrar la cuenta desde
Authentication → Users (borra todo en cascada), y marcar la denuncia como `revisada` o `descartada`.
Conviene revisarlas al menos una vez por semana.

## Pruebas que faltan

- En un teléfono o emulador real: no se ha instalado nunca. La interfaz se ha probado con Robolectric
  (pruebas de Compose en la JVM), no en un dispositivo.
- La app contra el Supabase real (registro, sincronizar entre dos teléfonos, amigos y grupos): necesita antes el
  correo propio. Contra el Supabase local ya está probada de punta a punta (`CloudIntegrationTest`).
- La compra de Plus con una cuenta de prueba de Play.
- TalkBack, texto al 200 % y «Quitar animaciones» del sistema en un teléfono real.
- Que los recordatorios y la sincronización en segundo plano lleguen (WorkManager puede retrasarlos con ahorro de batería).
- Restaurar la copia automática de Android en un teléfono nuevo.
- Lo nuevo del 2026-10-25 que solo se ve en un dispositivo: el widget y su botón, los atajos del icono, los enlaces
  `levelup://`, compartir un logro como imagen, «Proteger mis pantallas», Health Connect, y la app del reloj con su
  complicación (no hay reloj ni emulador de Wear OS en este equipo).

## Fuera de esta versión

Asistente de IA e iOS ([IOS.md](IOS.md)).
