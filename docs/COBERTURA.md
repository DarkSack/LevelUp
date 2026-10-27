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
| 14 | Datos | Parcial | Base local v4 (favoritos, metas, notas, momentos del día, versión mínima, etiquetas, aviso por hábito) con migraciones 1→2→3→4 probadas, ids deterministas. No hay tablas de dispositivos ni de compras en el servidor (la compra se comprueba en el teléfono: decisión «opción b»). |
| 15 | Backend y cuentas | Parcial | Desplegado el 2026-10-25 con RLS y 40 pruebas pgTAP. **La segunda migración (extras, 14 pruebas pgTAP más) solo está aplicada en local.** **Registro bloqueado en producción hasta poner SMTP propio.** No hay configuración separada de desarrollo y producción: las compilaciones usan siempre `supabase.properties`; el desarrollo usa el Supabase local. |
| 16 | Sincronización | Hecho | Probada con dos teléfonos simulados y de punta a punta contra el Supabase local. |
| 17 | Notificaciones | Hecho | Recordatorio diario a la hora elegida, de recuperación, avisos de amigos con horas de silencio, aviso propio por hábito (hora en el editor del hábito) y aviso de logro cuando se consigue desde el widget o el reloj; el permiso se pide al activarlos. |
| 18 | Privacidad y seguridad | Parcial | RLS, funciones que validan y limitan el uso, exportar, borrar datos y cuenta, política de privacidad. **Faltan** las condiciones de servicio, alojar la política en una URL pública y una auditoría de dependencias. |
| 19 | Monetización | Hecho | LevelUp Plus (compra única) con restauración y firma comprobada. Sin suscripciones ni IA (decisión). Falta crear el producto en Play Console. |
| 20 | Google Play | Parcial | Paquete, versión, icono, AAB firmado, llave fuera de git, ficha en español, contenido y seguridad de datos. Icono 512 × 512, gráfico 1024 × 500, capturas reales del teléfono y notas de versión en `docs/play/`. La 0.1.0 sale sin cuenta (`cuenta=no`). **Faltan**: splash propia (solo la del sistema en Android 12+), inglés (app y ficha), capturas del reloj, datos de acceso para revisores, guía de pruebas internas y cerradas (y el requisito de prueba cerrada si la cuenta de Play es personal). EAS no aplica: firma Gradle. |
| 21 | App Store | Decisión | Fuera de esta versión; solo [IOS.md](IOS.md). |
| 22 | Recursos comerciales y legales | Parcial | Hay descripciones corta y larga y política de privacidad. **Faltan** condiciones de servicio, textos de capturas, plan de localización, notas de revisión y de versión, y lista de verificación de publicación. |
| 23 | CI | Parcial | GitHub Actions corre las pruebas y compila los APK de depuración del teléfono y del reloj. **Faltan** lint, pruebas pgTAP, auditoría de dependencias, compilar el AAB de release y la subida a Play con aprobación manual. |
| 24 | Pruebas | Parcial | Unitarias: todas las reglas pedidas. Integración: Room, migración, sincronización, conflictos, RLS, amistades, bloqueos, borrado de cuenta. Interfaz (Robolectric): bienvenida, omitirla, onboarding interrumpido, completar un reto y su trofeo, accesibilidad, Plus. **Faltan** de interfaz: registro, sin conexión y reconexión, amigos y borrar cuenta; y todas las de accesibilidad y rendimiento en teléfono. |
| 25 | Criterios de aceptación | Parcial | Se cumplen 1–8, 10–14, 17–20. El 9 (registro con backend) funciona en local, no en producción hasta el SMTP. El 15 solo cubre Android. El 16 está a medias (faltan inglés y capturas del reloj). |
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
| Widget | Hecho, sin probar en dispositivo | Reto principal, avance y botón «Hecho» |
| Accesos directos | Hecho, sin probar en dispositivo | Reto del día, Concentración y Buscar |
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
| Compartir tarjeta de logro como imagen | Hecho, sin probar en dispositivo | Botón «Compartir» en el detalle de un logro |
| Perfil con avatar, título y trofeos | Hecho | Avatar emoji; título y hasta 3 trofeos que el servidor valida |
| Ocultar nivel o actividad | Hecho | Opciones de privacidad del perfil social |
| Denunciar y bloquear | Hecho | En la app y validado en el servidor |
| Abandonar grupos y retos | Hecho | «Salir» en el grupo; pausar o archivar retos propios |
| Historial de participación | Hecho | Grupos → Historial (terminados o dejados) |
| Importación validada | Hecho | Versión, duplicados, rangos, notas y metas |
| Protección de capturas opcional | Hecho, sin probar en dispositivo | Perfil → Privacidad → Proteger mis pantallas |
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
| Health Connect | Hecho, sin probar en dispositivo | Laboratorio; requiere la declaración de Play (ver PENDIENTES) |
| Sincronización teléfono y tablet | Hecho | Con la cuenta, igual que entre teléfonos |
| Wear OS y complicaciones | Hecho, sin probar en dispositivo | Módulo `wearApp`; necesita su propio canal en Play |
| Automatizaciones | Hecho | Enlaces `levelup://` que solo abren pantallas; no hay API que devuelva datos (decisión de privacidad) |
| Revisión de objetivos cada semana | Hecho | En la revisión semanal: «¿Siguen siendo importantes?» y prioridades |
| Suspender y volver sin lista atrasada | Hecho | Pausa con fecha; al volver eliges qué hábitos retomar |
| Portabilidad del progreso | Hecho | Respaldo, CSV, cuenta y guía de cambio de teléfono |
| Laboratorio de funciones | Hecho | Perfil → Laboratorio; apagarlo no toca el progreso |

## Lo que falta, en orden

1. **SMTP propio** (del propietario) y subir las plantillas: sin esto nadie puede registrarse (la 0.1.0 sale sin cuenta).
2. **Play Console** (del propietario): crear la app, el producto `levelup_plus`, la clave de licencia y la URL de la política.
3. **Probar en un teléfono**: instalar, TalkBack, texto grande, recordatorios, compra de prueba, registro real.
4. Recursos de la tienda: notas para revisores, lista de verificación y capturas del reloj (icono 512, gráfico
   1024 × 500, capturas del teléfono y notas de versión ya están).
6. Condiciones de servicio (lo social las justifica) con los datos del propietario.
7. Estado de sincronización en Inicio.
8. CI: lint, pgTAP, auditoría de dependencias y AAB de release.
9. Inglés: textos de la app (hoy están en el código) y ficha.
10. Splash propia y pruebas de interfaz que faltan.
11. Probar en un dispositivo lo nuevo de Android: widget, atajos, compartir, capturas protegidas, Health Connect y el reloj.
