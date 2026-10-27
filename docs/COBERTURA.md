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
| 13 | Pantallas | Hecho | Están las seis secciones. Inicio muestra el estado de la sincronización con cuenta. «Cuenta y sesiones» lista los dispositivos con la sesión iniciada (modelo y versión de Android) y cierra uno o todos los demás. |
| 14 | Datos | Parcial | Base local v4 (favoritos, metas, notas, momentos del día, versión mínima, etiquetas, aviso por hábito) con migraciones 1→2→3→4 probadas, ids deterministas. No hay tablas de dispositivos ni de compras en el servidor (la compra se comprueba en el teléfono: decisión «opción b»). |
| 15 | Backend y cuentas | Parcial | Desplegado con RLS: las tres migraciones (inicial, extras y sesiones) aplicadas en producción y 61 pruebas pgTAP en local. Cuenta con correo o nombre de usuario y contraseña, sin verificación ni recuperación por correo (decisión del propietario, 2026-10-26; «Confirm email» apagado en producción ese día). Las compilaciones usan siempre producción (`supabase.properties`); el Supabase local solo sirve para las pruebas (decisión del propietario, 2026-10-26). |
| 16 | Sincronización | Hecho | Probada con dos teléfonos simulados y de punta a punta contra el Supabase local. |
| 17 | Notificaciones | Hecho | Recordatorio diario a la hora elegida, de recuperación, avisos de amigos con horas de silencio, aviso propio por hábito (hora en el editor del hábito) y aviso de logro cuando se consigue desde el widget o el reloj; el permiso se pide al activarlos. |
| 18 | Privacidad y seguridad | Parcial | RLS, funciones que validan y limitan el uso, exportar, borrar datos y cuenta, política de privacidad publicada, condiciones de uso publicadas (shop.sackito.online/condiciones/levelup) y alertas de dependencias (Dependabot). |
| 19 | Monetización | Hecho | LevelUp Plus (compra única) con restauración y firma comprobada. Sin suscripciones ni IA (decisión). Falta crear el producto en Play Console. |
| 20 | Google Play | Parcial | Paquete, versión, icono, AAB firmado, llave fuera de git, ficha en español, contenido y seguridad de datos. Icono 512 × 512, gráfico 1024 × 500, capturas reales del teléfono y notas de versión en `docs/play/`. La 1.0.0 sale con la cuenta opcional. Splash propia en Android 12+ (fondo de la marca y el logo). Capturas del reloj, notas para la revisión y lista antes de publicar (con la prueba interna y la cerrada) en `PLAY_STORE.md`. App, reloj, ficha y notas también en inglés y portugués de Brasil (las capturas siguen en español). EAS no aplica: firma Gradle. |
| 21 | App Store | Decisión | Fuera de esta versión; solo [IOS.md](IOS.md). |
| 22 | Recursos comerciales y legales | Parcial | Descripciones corta y larga, notas de la versión, notas para la revisión, lista de verificación, política de privacidad y condiciones de uso ([condiciones.md](play/condiciones.md), publicadas en shop.sackito.online/condiciones/levelup). Las capturas no llevan textos encima (decisión: se ve la app tal cual). |
| 23 | CI | Parcial | GitHub Actions corre las pruebas, lint, los APK de depuración y los AAB de release sin firmar (como artefacto), las pruebas pgTAP en un Supabase local, y envía el grafo de dependencias para las alertas de Dependabot (más `dependabot.yml` semanal). **Falta** la subida a Play con aprobación manual: necesita la app creada en Play Console y una cuenta de servicio. |
| 24 | Pruebas | Parcial | Unitarias: todas las reglas pedidas. Integración: Room, migración, sincronización, conflictos, RLS, amistades, bloqueos, borrado de cuenta. Interfaz (Robolectric): bienvenida, omitirla, onboarding interrumpido, completar un reto y su trofeo, logros secretos, accesibilidad, Plus y, contra el Supabase local (`AccountUiTest`), registro con código, cambios sin conexión que suben al reconectar, amistad y borrar la cuenta. **Faltan** las de accesibilidad y rendimiento en un teléfono. |
| 25 | Criterios de aceptación | Parcial | Se cumplen 1–8, 10–14, 17–20. El 9 (registro con backend) se cumple sin verificación por correo («Confirm email» apagado en producción). El 15 solo cubre Android. El 16 se cumple (inglés y portugués; capturas del reloj). |
| 26 | Forma de trabajo | Hecho | Fases con pruebas; pendientes en [PENDIENTES.md](PENDIENTES.md). |

## Funciones pedidas el 2026-10-25

Lista del propietario, revisada contra el código el mismo día. Todo tiene pruebas automáticas salvo lo marcado
«sin probar en dispositivo», que depende de Android, del reloj o de Health Connect.

| Función | Estado | Dónde / notas |
|---|---|---|
| Misiones sorpresa opcionales | Hecho | Laboratorio → Retos sorpresa: tarjeta en Inicio con un reto nunca hecho; no cambia el plan |
| Retos contextuales (casa, exterior, trabajo, tiempo libre) | Hecho | Etiquetas en todo el catálogo; filtro en Explorar; «¿Dónde estás?» al pedir un reto extra |
| Retos sin materiales | Hecho | Etiqueta y filtro «Sin materiales» |
| Retos personalizados | Hecho | Objetivo, frecuencia, categoría, criterio, pasos, lugar, momento y versión mínima |
| Retos de recuperación | Hecho | Tras 7 días sin actividad: como mucho 2 retos, muy fáciles y de 10 min en total, y un aviso amable en Inicio |
| Misiones semanales | Hecho | Misiones opcionales sin castigo (ya estaban) |
| Retos de exploración | Hecho | «Probar algo nuevo» en Explorar: un reto nunca hecho, mejor de una categoría que no sueles elegir |
| Lista de favoritos | Hecho | Corazón en el detalle, filtro «Solo favoritos»; se sincroniza y va en el respaldo |
| Pausa de hábitos | Hecho | Pausar un hábito, o pausar todo con fecha de fin |
| Recordatorios locales | Hecho | Hora, días de aviso, silencio e intensidad |
| Widget | Hecho | Reto principal, avance y botón «Hecho». Probado en dispositivo el 2026-10-25. |
| Accesos directos | Hecho | Reto del día, Concentración y Buscar. Probado en dispositivo el 2026-10-25. |
| Exportación JSON o CSV | Hecho | CSV con protección contra fórmulas |
| Importación y restauración | Hecho | Respaldo v2 validado antes de reemplazar nada |
| Búsqueda global | Hecho | Retos, hábitos, logros, metas y notas, sin acentos |
| Filtros y etiquetas | Hecho | Duración, dificultad, categoría, lugar, sin materiales y favoritos |
| Modo concentración | Hecho | Un solo reto en pantalla con cronómetro |
| Resumen semanal | Hecho | Revisión semanal en Inicio y en Perfil |
| Objetivos personales | Hecho | Perfil → Mis metas |
| Modo sin gamificación | Hecho | Estilo de juego «Sin juego»: sin XP, niveles, rachas, misiones ni animaciones |
| XP por recuperación | Hecho | 20 XP con el primer reto tras 14 días o más sin actividad; deshacer la quita |
| Objetivos colectivos | Hecho | Retos en grupo «Juntos» con meta común |
| Calendario de actividades grupales | Hecho | Mes con los días de retos en grupo, en Grupos |
| Reacciones rápidas | Hecho | Cinco reacciones predefinidas |
| Mensajes de ánimo prediseñados | Hecho | Cinco mensajes; el servidor solo acepta los predefinidos |
| Invitaciones para un reto juntos | Hecho | Invitar amigos a un reto en grupo |
| Compartir tarjeta de logro como imagen | Hecho | Botón «Compartir» en el detalle de un logro. Probado en dispositivo el 2026-10-25. |
| Perfil con avatar, título y trofeos | Hecho | Avatar emoji; título y hasta 3 trofeos que el servidor valida |
| Ocultar nivel o actividad | Hecho | Opciones de privacidad del perfil social |
| Denunciar y bloquear | Hecho | En la app y validado en el servidor |
| Abandonar grupos y retos | Hecho | «Salir» en el grupo; pausar o archivar retos propios |
| Historial de participación | Hecho | Grupos → Historial (terminados o dejados) |
| Importación validada | Hecho | Versión, duplicados, rangos, notas y metas |
| Protección de capturas opcional | Hecho | Perfil → Privacidad → Proteger mis pantallas. Probado en dispositivo el 2026-10-25. |
| Bloqueo y denuncia en backend | Hecho | Funciones con RLS y límites de uso |
| Modo sin analítica | Hecho (no aplica) | La app no tiene analítica ni rastreo de ningún tipo |
| Copia de seguridad transparente | Hecho | JSON legible y explicación de la copia de Android |
| Tiempo flexible | Hecho | Nada caduca por pasarse del tiempo estimado; el cronómetro cuenta hacia arriba |
| Pausas voluntarias | Hecho | Pausa con o sin fecha, sin perder nada |
| Recordatorios adaptativos (con autorización) | Hecho | La revisión semanal propone hora y días; solo cambian si pulsas |
| Planificación por bloques | Hecho | Mañana, tarde y noche en Inicio, el detalle y los retos propios |
| Reinicio visual del día | Hecho | Cada día solo muestra los retos de hoy; lo no hecho no se acumula |
| Objetivos mínimos | Hecho | «Versión mínima» en retos propios y botón «Hice la versión mínima» (XP de dificultad 1) |
| Modo de baja estimulación | Hecho | Accesibilidad → Baja estimulación |
| Navegación consistente | Hecho | Cuatro pestañas fijas y «Volver» en cada pantalla |
| Modo oscuro del sistema | Hecho | Tema «Como el teléfono» |
| Vibración háptica opcional | Hecho | Accesibilidad → Vibración |
| Respaldo y transferencia | Hecho | Tus datos → Cambiar de teléfono |
| Health Connect | Hecho | Laboratorio; requiere la declaración de Play (ver PENDIENTES). Probado en dispositivo el 2026-10-25. |
| Sincronización teléfono y tablet | Hecho | Con la cuenta, igual que entre teléfonos |
| Wear OS y complicaciones | Hecho, sin probar en dispositivo | Módulo `wearApp`; necesita su propio canal en Play |
| Automatizaciones | Hecho | Enlaces `levelup://` que solo abren pantallas; no hay API que devuelva datos (decisión de privacidad) |
| Revisión de objetivos cada semana | Hecho | En la revisión semanal: «¿Siguen siendo importantes?» y prioridades |
| Suspender y volver sin lista atrasada | Hecho | Pausa con fecha; al volver eliges qué hábitos retomar |
| Portabilidad del progreso | Hecho | Respaldo, CSV, cuenta y guía de cambio de teléfono |
| Laboratorio de funciones | Hecho | Perfil → Laboratorio; apagarlo no toca el progreso |

## Idiomas

Español, inglés y portugués de Brasil, según el idioma del teléfono (en Android 13+ también solo para LevelUp).
El texto en español del código es la clave: `Text` de `ui/components` traduce lo que pinta y `tr("…{0}…", args)`
lo demás (avisos, TalkBack, notificaciones, widget). Las traducciones están en `i18n/TextosUi.kt` y
`i18n/TextosCatalogo.kt`; `I18nTest` falla si un texto nuevo del código o del catálogo no tiene traducción o si
pierde un `{0}`. El reloj usa `values-en`/`values-pt`. El CSV exportado sigue en español.

## Lo que falta, en orden

1. **Play Console** (del propietario): crear la app, el producto `levelup_plus`, la clave de licencia y la URL de la política.
2. **Probar en un teléfono**: TalkBack, texto grande, recordatorios, compra de prueba y registro real. La 1.0.0 de
   release (con R8) ya se recorrió sin cuenta en la tableta el 2026-10-26: bienvenida, Hoy, logro, Progreso, Retos,
   Perfil, Cuenta, Plus, Amigos, Revisión, Metas, Concentración, Buscar, enlaces y compartir, sin cierres.
3. CI: subida a Play con aprobación manual (tras crear la app en Play Console y una cuenta de servicio).
4. Probar el reloj (Wear OS) en un dispositivo.
