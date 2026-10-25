# Cobertura del PROMPT MAESTRO

Revisado el 2026-10-25 contra el código. **Hecho**: implementado y con pruebas automáticas. **Parcial**: falta una
parte (se dice cuál). **Falta**: no está. **Decisión**: el propietario lo cambió o lo dejó fuera.

Nada se ha probado en un teléfono real ni contra el Supabase de producción con cuentas reales (ver
[PENDIENTES.md](PENDIENTES.md)).

| § | Tema | Estado | Notas |
|---|---|---|---|
| 1–2 | Misión y principios | Hecho | Sin castigos por días sin actividad; todo lo esencial es gratis y funciona sin conexión. |
| 3 | Stack | Decisión | Kotlin Multiplatform + Compose en vez de Expo/TypeScript; Supabase como backend. |
| 4 | Local primero | Hecho | Room; funciona sin cuenta; la sincronización usa una cola de salida. |
| 5 | Bienvenida | Hecho | Animada, se puede omitir, respeta «reducir movimiento» y se repite desde Perfil → Acerca de. El onboarding explica que se usa sin cuenta; la cuenta se crea luego desde Perfil (no hay botón de registro dentro del onboarding). |
| 6 | Onboarding y personalización | Hecho | 6 pasos, conserva el progreso si se interrumpe; todo se cambia luego en Perfil. |
| 7 | Motor de retos | Hecho | Catálogo local, categorías, dificultad, tiempo, variedad, cambiar, posponer, versión más fácil, retos propios. |
| 8 | Dificultad adaptativa | Hecho | La valoración (fácil, justo, difícil) ajusta sin saltos bruscos. |
| 9 | Gamificación RPG | Hecho | XP, nivel global y por categoría, trofeos, rachas con días de descanso. |
| 10 | Social | Hecho | Amigos, solicitudes, bloqueos, reacciones predefinidas, retos en grupo, clasificación voluntaria, privacidad, denuncias. La moderación no tiene panel: se hace en Supabase. |
| 11 | Accesibilidad | Parcial | Tamaño de texto, alto contraste, menos movimiento, modo sencillo, vibración y áreas de 48 dp en el código. Falta probarlo con TalkBack y texto al 200 % en un teléfono. |
| 12 | Identidad visual | Hecho | Tema propio, claro y oscuro; colores y marcos de Plus. |
| 13 | Pantallas | Parcial | Están las seis secciones. **Falta en Inicio el estado de sincronización** (solo se ve en Perfil → Cuenta). «Cuenta y sesiones» permite cerrar la sesión en los demás dispositivos, pero no lista los dispositivos. |
| 14 | Datos | Parcial | Base local v2 con migración probada, ids deterministas. No hay tablas de dispositivos ni de compras en el servidor (la compra se comprueba en el teléfono: decisión «opción b»). |
| 15 | Backend y cuentas | Parcial | Desplegado el 2026-10-25 con RLS y 40 pruebas pgTAP. **Registro bloqueado en producción hasta poner SMTP propio.** No hay configuración separada de desarrollo y producción: las compilaciones usan siempre `supabase.properties`; el desarrollo usa el Supabase local. |
| 16 | Sincronización | Hecho | Probada con dos teléfonos simulados y de punta a punta contra el Supabase local. |
| 17 | Notificaciones | Parcial | Recordatorio diario a la hora elegida, de recuperación y avisos de amigos con horas de silencio; el permiso se pide al activarlos. **Faltan** avisos por logro y recordatorios distintos por hábito u hora. |
| 18 | Privacidad y seguridad | Parcial | RLS, funciones que validan y limitan el uso, exportar, borrar datos y cuenta, política de privacidad. **Faltan** las condiciones de servicio, alojar la política en una URL pública y una auditoría de dependencias. |
| 19 | Monetización | Hecho | LevelUp Plus (compra única) con restauración y firma comprobada. Sin suscripciones ni IA (decisión). Falta crear el producto en Play Console. |
| 20 | Google Play | Parcial | Paquete, versión, icono, AAB firmado, llave fuera de git, ficha en español, contenido y seguridad de datos. **Faltan**: splash propia (solo la del sistema en Android 12+), inglés (app y ficha), icono 512 × 512, gráfico 1024 × 500, capturas reales, notas de versión, datos de acceso para revisores, guía de pruebas internas y cerradas (y el requisito de prueba cerrada si la cuenta de Play es personal). EAS no aplica: firma Gradle. |
| 21 | App Store | Decisión | Fuera de esta versión; solo [IOS.md](IOS.md). |
| 22 | Recursos comerciales y legales | Parcial | Hay descripciones corta y larga y política de privacidad. **Faltan** condiciones de servicio, textos de capturas, plan de localización, notas de revisión y de versión, y lista de verificación de publicación. |
| 23 | CI | Parcial | GitHub Actions corre las pruebas y compila el APK de depuración. **Faltan** lint, pruebas pgTAP, auditoría de dependencias, compilar el AAB de release y la subida a Play con aprobación manual. |
| 24 | Pruebas | Parcial | Unitarias: todas las reglas pedidas. Integración: Room, migración, sincronización, conflictos, RLS, amistades, bloqueos, borrado de cuenta. Interfaz (Robolectric): bienvenida, omitirla, onboarding interrumpido, completar un reto y su trofeo, accesibilidad, Plus. **Faltan** de interfaz: registro, sin conexión y reconexión, amigos y borrar cuenta; y todas las de accesibilidad y rendimiento en teléfono. |
| 25 | Criterios de aceptación | Parcial | Se cumplen 1–8, 10–14, 17–20. El 9 (registro con backend) funciona en local, no en producción hasta el SMTP. El 15 solo cubre Android. El 16 está a medias (faltan gráficos, capturas e inglés). |
| 26 | Forma de trabajo | Hecho | Fases con pruebas; pendientes en [PENDIENTES.md](PENDIENTES.md). |

## Lo que falta, en orden

1. **SMTP propio** (del propietario) y subir las plantillas: sin esto nadie puede registrarse.
2. **Play Console** (del propietario): crear la app, el producto `levelup_plus`, la clave de licencia y la URL de la política.
3. **Probar en un teléfono**: instalar, TalkBack, texto grande, recordatorios, compra de prueba, registro real.
4. Recursos de la tienda: icono 512, gráfico 1024 × 500 y capturas reales (se pueden sacar con Robolectric como
   en Moneytor), notas de versión, notas para revisores y lista de verificación.
5. Condiciones de servicio (lo social las justifica) con los datos del propietario.
6. Estado de sincronización en Inicio.
7. CI: lint, pgTAP, auditoría de dependencias y AAB de release.
8. Inglés: textos de la app (hoy están en el código) y ficha.
9. Splash propia, avisos por logro y pruebas de interfaz que faltan.
