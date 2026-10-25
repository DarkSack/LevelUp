# Pendientes del propietario

Lo que el código no puede resolver solo: decisiones, cuentas, datos legales y pruebas en un teléfono.

## Decisiones

| Tema | Estado | Opciones |
|---|---|---|
| Identificador del paquete | Provisional: `app.levelup` (en `androidApp/build.gradle.kts`) | Elegir uno propio antes de la primera subida a Play; después no se puede cambiar |
| Nombre en la tienda | "LevelUp" | Es muy probable que esté ocupado en Play y en la App Store; conviene un nombre distintivo |
| Compras / freemium | No hay compras en v1 | El prompt pide validar compras en servidor y v1 no tiene servidor. Opciones: (a) publicar gratis y añadir compras en v2 con backend; (b) Play Billing con verificación local de la firma, como se hizo en Narra, sabiendo que es más fácil de saltar |
| Público objetivo | Sin decidir | Para Play: si se dirige a menores de 13 años aplican las reglas de Familias. Recomendado: 13+ |

## Cuentas y datos legales (no se inventan)

- Nombre del desarrollador y correo de contacto para la ficha de Play y la política de privacidad.
- URL pública donde alojar la política de privacidad (borrador en `docs/play/privacidad.md`).
- Cuenta de Google Play Console.
- Llave de subida: generarla y guardarla fuera del repositorio. `keystore.properties` (ignorado por git) en la
  raíz del proyecto con `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Sin ella el release sale sin firmar.

## Pruebas que faltan

- En un teléfono o emulador real: no se ha instalado nunca. La interfaz se ha probado con Robolectric
  (pruebas de Compose en la JVM), no en un dispositivo.
- TalkBack, texto al 200 % y "Quitar animaciones" del sistema en un teléfono real.
- Que el recordatorio llegue a la hora (WorkManager puede retrasarlo unos minutos con ahorro de batería).
- Restaurar la copia automática de Android en un teléfono nuevo.

## Fuera de v1 (acordado)

Backend y cuentas (Supabase), sincronización, social, asistente de IA e iOS ([IOS.md](IOS.md)).
