<!-- Prompt maestro de Sack, sin cambios. Restaurado el 2026-10-24 desde el texto que pegó en la sesión: el archivo original se borró por error junto con el proyecto Expo descartado. Las decisiones que cambian el stack y el alcance están en ARQUITECTURA.md. -->

LEVELUP — PROMPT MAESTRO DE DESARROLLO
1. Rol y misión
Actúa como un equipo senior multidisciplinario formado por:

* Arquitecto de software mobile.
* Desarrollador experto en React Native, Expo y TypeScript.
* Diseñador UI/UX especializado en accesibilidad cognitiva.
* Especialista en gamificación, hábitos y experiencia de usuario.
* Ingeniero de backend, bases de datos y sincronización offline-first.
* Especialista en seguridad, privacidad y autenticación.
* QA Engineer y especialista en automatización de pruebas.
* Especialista en publicación y distribución para Google Play y Apple App Store.

Construye una aplicación móvil multiplataforma llamada LevelUp, destinada a ayudar a las personas a desarrollar hábitos positivos mediante retos personalizados, progresión gamificada, seguimiento de objetivos y acompañamiento social.
No te limites a crear una demostración visual. Implementa una aplicación funcional, con persistencia real, reglas de negocio verificables, navegación completa, autenticación, sincronización, gestión de errores, pruebas y documentación.
Trabaja directamente sobre el repositorio disponible. Si ya contiene código, inspecciónalo antes de modificarlo. Conserva lo que funcione y evita reescribir innecesariamente componentes útiles.
No declares terminada una funcionalidad hasta que su comportamiento esté implementado y probado.
2. Visión del producto
LevelUp transforma el desarrollo personal en una experiencia progresiva similar a un RPG, sin convertir la productividad en una fuente de ansiedad.
Ejemplos de retos:

* Social: saluda a tres personas hoy.
* Físico: camina dos kilómetros.
* Mental: dedica diez minutos a aprender algo nuevo.
* Productividad: completa una tarea que llevas posponiendo.
* Bienestar: dedica cinco minutos a relajarte.
* Creatividad: dibuja, escribe o crea algo durante quince minutos.
* Hogar: organiza un espacio pequeño.
* Finanzas: registra tus gastos del día.
* Hábitos digitales: dedica un periodo a una actividad sin distracciones.

Los objetivos son orientativos. El sistema debe adaptar la dificultad a las preferencias, capacidades, tiempo disponible y contexto del usuario.
Principios fundamentales:

1. Progreso por encima de perfección.
2. Accesibilidad sin sacrificar funcionalidad.
3. Privacidad por defecto.
4. Funcionamiento offline real.
5. Personalización sin complejidad innecesaria.
6. Gamificación que incentive, no que castigue.
7. Sin mecanismos manipulativos de retención.
8. Funciones básicas gratuitas y arquitectura preparada para un modelo freemium.
9. Ninguna función premium debe ser necesaria para progresar personalmente.
10. La aplicación debe resultar comprensible para personas neurodivergentes y neurotípicas.

3. Stack tecnológico
Utiliza como arquitectura inicial:

* React Native.
* Expo con TypeScript en modo estricto.
* Expo Router para navegación.
* SQLite mediante una solución compatible con Expo para persistencia local.
* Zustand para estado de interfaz y estado de aplicación que realmente lo necesite.
* TanStack Query para sincronización y operaciones remotas, sin convertir la nube en requisito para usar la app.
* Supabase para autenticación, PostgreSQL, API y sincronización social.
* Supabase Row Level Security (RLS) para autorización de datos.
* React Native Reanimated para animaciones.
* React Native Gesture Handler cuando sea necesario.
* React Hook Form y Zod para formularios y validación.
* Expo Notifications para recordatorios locales.
* Expo SecureStore para secretos y credenciales locales que corresponda proteger.
* EAS Build y EAS Submit para la automatización de compilación y distribución.

Verifica las versiones compatibles y estables de todas las dependencias antes de instalarlas. No combines versiones incompatibles ni uses APIs obsoletas.
La aplicación debe compilar para Android e iOS desde una base de código compartida. No simules funciones nativas que requieran configuración adicional: implementa los plugins, permisos, archivos de configuración y adaptaciones de plataforma correspondientes.
Mantén la lógica de negocio independiente de los componentes visuales.
4. Arquitectura local-first obligatoria
El dispositivo es la fuente inmediata de lectura y escritura para las funciones personales.
Implementa las siguientes reglas:

* Los retos, las preferencias, el progreso, el inventario de trofeos y las estadísticas personales deben poder consultarse sin conexión.
* Completar un reto debe registrarse inmediatamente en SQLite.
* La concesión de XP debe realizarse de forma transaccional.
* Las pantallas principales no deben esperar una petición de red para mostrar información disponible localmente.
* Las operaciones pendientes deben guardarse en una cola persistente.
* Cuando vuelva la conexión, la aplicación sincronizará los cambios pendientes.
* Los errores de red no deben borrar cambios locales.
* La sincronización debe ser idempotente.
* Deben existir estrategias explícitas para conflictos entre dispositivos.
* El usuario debe poder saber si sus datos están sincronizados.
* Las funciones sociales pueden necesitar conexión, pero nunca deben bloquear el uso individual.
* La aplicación debe sobrevivir a cierres inesperados durante una sincronización.

Diseña identificadores UUID, marcas de tiempo, versiones de registros y estados de sincronización adecuados.
No dependas exclusivamente de una caché de consultas como mecanismo de almacenamiento offline.
Persistencia y migraciones
Crea un sistema de migraciones versionadas para SQLite y PostgreSQL.
Las migraciones deben poder ejecutarse de forma segura, registrar la versión del esquema y preservar los datos existentes.
Implementa una estrategia documentada para copias de seguridad, recuperación, exportación e importación de los datos personales.
El usuario debe poder exportar sus datos en un formato legible y recuperar su progreso en un dispositivo nuevo.
5. Primera apertura y animación de bienvenida
La primera experiencia debe ser memorable, profesional y accesible.
Pantalla de bienvenida animada
Diseña una pantalla inicial de LevelUp con:

* Fondo visual coherente con la identidad de la marca.
* Logotipo y símbolo de LevelUp.
* Animación progresiva de aparición.
* Efecto de crecimiento o evolución que represente subir de nivel.
* Transiciones suaves y de baja intensidad.
* Texto principal: «Cada pequeño paso cuenta».
* Texto secundario: «Construye hábitos, descubre tu potencial y avanza a tu ritmo».
* Botón principal: «Comenzar».
* Acceso secundario para omitir la animación.

La animación debe ejecutarse localmente y no depender de una API.
Evita una introducción excesivamente larga. El usuario debe poder continuar inmediatamente y la duración de la animación debe poder reducirse o saltarse.
Respeta la preferencia del sistema operativo de reducir movimiento. Evita flashes, parpadeos, zoom agresivo y estímulos sensoriales innecesarios.
Comportamiento de la primera apertura
La secuencia inicial será:

1. Bienvenida animada.
2. Presentación breve de la propuesta de valor.
3. Selección de objetivos personales.
4. Configuración de preferencias y accesibilidad.
5. Selección de dificultad y disponibilidad.
6. Configuración opcional de recordatorios.
7. Generación de la primera selección de retos.
8. Presentación del funcionamiento de XP y niveles.
9. Acceso al modo invitado o registro.
10. Entrada al panel principal.

No obligues al usuario a completar todos los pasos.
Permite omitir, retroceder y modificar decisiones. Guarda el progreso del onboarding localmente para que una interrupción no obligue a empezar de cero.
Al completar el onboarding, no vuelvas a mostrar automáticamente la introducción en cada apertura.
Incluye una opción en Ajustes para volver a reproducir la bienvenida o repetir el tutorial.
La bienvenida animada, el onboarding y la pantalla de carga deben ser componentes diferentes con responsabilidades separadas.
Inicio de sesión posterior
Si el usuario ya ha completado el onboarding, abre directamente la aplicación.
Si tiene una sesión válida, restaura su cuenta y sincroniza en segundo plano.
Si está en modo invitado, carga su perfil local sin obligarlo a registrarse.
6. Onboarding y personalización
Permite seleccionar una o varias categorías:

* Físico.
* Social.
* Mental y aprendizaje.
* Productividad y organización.
* Bienestar y autocuidado.
* Creatividad.
* Finanzas personales.
* Hábitos digitales.
* Hogar y entorno.
* Categorías personalizadas.

Solicita únicamente la información necesaria para personalizar los retos.
Preferencias configurables:

* Objetivos principales.
* Dificultad inicial.
* Tiempo disponible por día.
* Frecuencia de los retos.
* Horarios preferidos.
* Cantidad de retos diarios.
* Duración estimada de las actividades.
* Preferencias de accesibilidad.
* Intensidad de las notificaciones.
* Categorías prioritarias.
* Preferencias de privacidad.
* Estilo de progresión y gamificación.

Permite cambiar estas preferencias posteriormente.
No solicites diagnósticos médicos ni hagas inferencias clínicas sobre el usuario.
7. Motor de retos
Implementa un motor de selección de retos independiente de la interfaz.
Cada reto debe incluir, como mínimo:

* Identificador.
* Título.
* Descripción.
* Categoría.
* Objetivo medible o condición de finalización.
* Dificultad.
* Duración estimada.
* XP base.
* Requisitos previos, si existen.
* Frecuencia permitida.
* Fecha de disponibilidad.
* Etiquetas.
* Pasos opcionales.
* Criterios de finalización.
* Estado del reto.
* Procedencia: catálogo, personalizado o generado.
* Indicaciones de seguridad pertinentes.
* Versión del contenido.

Selección inteligente
El motor debe:

1. Priorizar las categorías seleccionadas por el usuario.
2. Respetar su dificultad preferida.
3. Considerar el tiempo disponible.
4. Evitar repeticiones excesivas.
5. Proponer variedad sin perder consistencia.
6. Adaptarse al historial de retos completados y omitidos.
7. Ofrecer alternativas más pequeñas cuando un reto resulte difícil.
8. Permitir descartar o sustituir retos.
9. No aumentar la dificultad automáticamente de forma agresiva.
10. Mantener disponibles los retos adecuados incluso sin conexión.

Catálogo local
Incluye un catálogo inicial suficientemente amplio para que la aplicación sea útil desde el primer lanzamiento sin internet.
Organiza el contenido por categorías, dificultades y duraciones.
Incluye pruebas que comprueben que los retos tienen instrucciones claras, objetivos alcanzables y criterios de finalización comprensibles.
Retos personalizados
Permite crear retos con título, descripción, categoría, dificultad, frecuencia y condiciones de finalización.
Permite convertir retos recurrentes en hábitos.
El usuario debe poder pausar, editar, archivar y eliminar sus retos personalizados.
Inteligencia artificial opcional
Diseña una interfaz desacoplada para incorporar proveedores de IA.
La aplicación debe funcionar plenamente con el catálogo local cuando no exista conexión o proveedor configurado.
No incluyas claves de API en el código cliente.
Si se habilita IA, las solicitudes deben pasar por un servicio seguro que aplique límites, validación y controles de costes.
Los retos generados deben validarse antes de guardarse. La IA no debe tener autoridad para conceder XP, modificar el inventario, alterar cuentas ni ejecutar acciones arbitrarias.
No prometas adaptación mediante IA si el proveedor no está configurado.
8. Dificultad adaptativa y flexibilidad
Implementa una progresión que tenga en cuenta:

* Porcentaje de finalización.
* Preferencias explícitas.
* Tiempo disponible.
* Valoraciones de dificultad.
* Retos sustituidos.
* Descansos y pausas.
* Cambios de objetivos.

Permite elegir entre niveles de dificultad manuales, sugerencias automáticas y adaptación controlada por el usuario.
Incluye una opción para simplificar un reto sin penalización.
Por ejemplo, un reto de caminar dos kilómetros puede convertirse en una versión más corta cuando el usuario tenga poco tiempo.
No reduzcas automáticamente la dificultad por una sola tarea omitida ni interpretes la falta de actividad como fracaso personal.
No utilices inferencias médicas ni recomendaciones de ejercicio potencialmente peligrosas.
9. Gamificación RPG
Experiencia y niveles
Implementa:

* XP por completar retos.
* Nivel global.
* Progreso por categoría.
* Rangos y títulos.
* Logros.
* Trofeos.
* Colecciones.
* Misiones especiales.
* Recompensas cosméticas.
* Historial de progresión.

Define una fórmula de XP y progresión explícita, configurable y testeable.
La misma operación no debe otorgar XP dos veces.
El servidor debe validar las operaciones remotas para evitar manipulación del progreso social.
En modo offline, los eventos locales deben conservar identificadores únicos y sincronizarse posteriormente de forma idempotente.
Atributos por categoría
Cada categoría puede tener su propio nivel de dominio.
Ejemplos:

* Social.
* Físico.
* Mental.
* Productividad.
* Bienestar.
* Creatividad.

El nivel global representa el progreso agregado; los niveles de categoría representan la dedicación específica.
Evita presentar los atributos como medidas científicas de la capacidad o valor personal.
Logros y trofeos
Incluye logros por:

* Primer reto completado.
* Constancia.
* Exploración de nuevas categorías.
* Metas personales.
* Hitos de progresión.
* Participación en desafíos.
* Recuperación después de una pausa.
* Objetivos definidos por el usuario.

Cada logro debe incluir nombre, descripción, icono, condición, fecha de desbloqueo y recompensa.
Incluye estados bloqueado, disponible y desbloqueado.
Los logros deben desbloquearse mediante eventos de dominio, no por condiciones visuales improvisadas.
Evita logros que incentiven conductas inseguras, agotamiento o interacción social no deseada.
Rachas y descansos
Incluye rachas opcionales y objetivos semanales flexibles.
No elimines XP ni niveles por perder una racha.
Permite configurar días de descanso, pausar objetivos y reanudar hábitos.
El sistema debe reconocer la continuidad y la recuperación, no solamente la perfección.
Monetización justa
Prepara una arquitectura freemium sin implementar compras falsas.
No vendas XP, resultados sociales, ventajas competitivas abusivas ni la posibilidad de evitar penalizaciones artificiales.
Los cosméticos premium no deben interferir con la accesibilidad.
Si se habilitan compras, utiliza las APIs de compras integradas correspondientes a cada tienda y valida las transacciones en el servidor.
10. Apartado social
Implementa:

* Búsqueda de usuarios.
* Solicitudes de amistad.
* Aceptación y rechazo de solicitudes.
* Lista de amigos.
* Eliminación y bloqueo de usuarios.
* Perfiles configurables.
* Estado de actividad opcional.
* Progreso compartido mediante permisos.
* Felicitaciones y reacciones.
* Retos cooperativos.
* Desafíos grupales.
* Clasificaciones voluntarias.
* Notificaciones sociales configurables.

El sistema debe respetar la privacidad individual y permitir utilizar LevelUp sin amigos.
No publiques automáticamente categorías personales, historial detallado, rutinas ni información sensible.
Las clasificaciones deben ser opcionales y permitir ocultar el perfil.
Seguridad social
Implementa controles contra spam, solicitudes abusivas y manipulación de resultados.
Los bloqueos deben impedir interacciones no deseadas de forma efectiva en el servidor.
Si se incorporan comentarios, publicaciones o contenido generado por usuarios, añade moderación, mecanismos de denuncia, políticas de contenido y procedimientos de actuación antes de habilitar esas funciones.
No implementes chat público como requisito para la primera versión.
11. Accesibilidad cognitiva y sensorial
La accesibilidad debe formar parte de la arquitectura y del sistema de diseño.
Diseña para personas con distintas preferencias cognitivas, sensoriales y motoras, incluidas personas con TDAH, autismo, dislexia u otras necesidades, sin asumir que todas tienen las mismas dificultades.
Principios de interfaz

* Una acción principal clara por pantalla.
* Jerarquía visual consistente.
* Lenguaje directo y concreto.
* Navegación predecible.
* Etiquetas comprensibles.
* Evitar pantallas saturadas.
* Evitar notificaciones coercitivas.
* Evitar cronómetros innecesarios.
* Reducir decisiones superfluas.
* Ofrecer pasos pequeños y opcionales.
* Mantener controles en posiciones consistentes.
* Mostrar estados de carga, error, éxito y contenido vacío.

Preferencias de accesibilidad
Incluye:

* Modo sencillo con menor densidad visual.
* Tamaños de texto ajustables.
* Compatibilidad con lectores de pantalla.
* VoiceOver y TalkBack.
* Etiquetas accesibles para controles.
* Contraste adecuado.
* Uso de texto o iconos además del color.
* Compatibilidad con texto ampliado del sistema.
* Reducción de movimiento.
* Control de animaciones.
* Control de sonidos y vibraciones.
* Ajustes de notificaciones.
* Temporizadores de enfoque opcionales.
* División de tareas en subtareas.
* Recordatorios para retomar una actividad, si se desean.
* Flexibilidad para días difíciles.

No presentes la accesibilidad como un modo exclusivo que segregue usuarios. Las preferencias deben ser combinables y estar disponibles en cualquier momento.
Verifica los componentes con herramientas de accesibilidad de Android e iOS.
12. Sistema visual e identidad
Crea una identidad visual propia para LevelUp.
Debe sentirse como una aplicación de crecimiento personal moderna con elementos RPG sutiles, no como una copia de un videojuego existente.
Diseña:

* Logotipo.
* Icono de aplicación.
* Paleta de colores.
* Tipografía.
* Escala de espaciado.
* Radios de bordes.
* Elevaciones.
* Iconografía.
* Componentes.
* Estados interactivos.
* Animaciones.
* Ilustraciones y trofeos.
* Tema claro y oscuro.
* Tokens semánticos.

Utiliza un lenguaje visual coherente, con buena legibilidad y una jerarquía de información clara.
Evita depender de degradados, sombras o animaciones para comunicar información importante.
La aplicación debe verse correctamente en diferentes tamaños de pantalla, densidades, orientaciones compatibles y escalas de texto.
13. Navegación y pantallas
Implementa como mínimo las siguientes secciones.
Inicio

* Saludo contextual.
* Reto principal.
* Retos del día.
* Progreso diario.
* XP y nivel.
* Resumen de categorías.
* Acceso rápido a la siguiente acción.
* Estado de sincronización cuando sea pertinente.

Retos

* Retos disponibles.
* Filtros por categoría.
* Dificultad y duración.
* Detalles del reto.
* Inicio, pausa y finalización.
* Sustitución de retos.
* Historial.
* Creación de retos personalizados.

Progreso

* Nivel global.
* Niveles por categoría.
* Evolución temporal.
* Estadísticas personales.
* Hábitos recurrentes.
* Historial de actividad.
* Comparaciones con objetivos propios.

No utilices estadísticas que impliquen precisión inexistente ni castigues días sin actividad.
Logros y trofeos

* Colección de trofeos.
* Logros desbloqueados.
* Próximos hitos.
* Recompensas.
* Detalle de condiciones.
* Animación accesible de desbloqueo.

Social

* Amigos.
* Solicitudes.
* Perfiles.
* Retos cooperativos.
* Clasificaciones opcionales.
* Controles de privacidad.

Perfil y ajustes

* Datos personales mínimos.
* Preferencias.
* Objetivos.
* Accesibilidad.
* Notificaciones.
* Cuenta y sesiones.
* Sincronización.
* Exportación e importación.
* Privacidad.
* Eliminación de cuenta.
* Eliminación de datos.
* Información legal.
* Versión de la aplicación.
* Reproducción de la bienvenida.

Usa navegación inferior cuando sea apropiado, con etiquetas visibles y un número reducido de destinos principales.
No conviertas cada función en una pestaña independiente.
14. Diseño de datos
Diseña entidades normalizadas y relaciones explícitas para:

* Perfil de usuario.
* Preferencias.
* Categorías.
* Catálogo de retos.
* Retos personalizados.
* Instancias de retos.
* Hábitos recurrentes.
* Eventos de progreso.
* Transacciones de XP.
* Niveles.
* Logros.
* Definiciones de trofeos.
* Trofeos desbloqueados.
* Rachas.
* Misiones.
* Amistades.
* Solicitudes de amistad.
* Bloqueos.
* Retos grupales.
* Participantes.
* Reacciones.
* Notificaciones.
* Cola de sincronización.
* Dispositivos y sesiones.
* Suscripciones y compras, si se habilitan.

Incluye claves primarias, claves foráneas, índices, restricciones, fechas, estados y políticas de borrado.
Distingue claramente entre la definición de un reto y cada instancia que el usuario debe completar.
Define una política de retención y eliminación de datos.
Evita duplicar información derivada que pueda calcularse de forma fiable.
15. Backend, autenticación y cuentas
Utiliza Supabase como backend inicial.
Implementa:

* Registro con correo electrónico.
* Inicio y cierre de sesión.
* Recuperación de contraseña.
* Verificación de correo cuando corresponda.
* Gestión segura de sesiones.
* Cierre de sesiones remotas.
* Vinculación de cuenta invitada, si está soportada de forma segura.
* Sincronización de perfil y progreso.
* Amistades y solicitudes.
* Retos grupales.
* Preferencias de privacidad.
* Eliminación de cuenta.

Ofrece modo invitado sin conexión desde el primer uso.
No obligues a crear una cuenta para completar retos personales.
Al convertir una cuenta invitada en cuenta registrada, fusiona el progreso de forma segura y evita duplicar recompensas.
Protege las tablas con Row Level Security. Define políticas por operación y rol; no confíes exclusivamente en ocultar botones desde el cliente.
Las claves privilegiadas y los secretos de backend nunca deben incluirse en la aplicación distribuida.
Incluye un archivo de variables de entorno de ejemplo sin secretos reales.
16. Modelo de sincronización
Implementa un patrón de sincronización con:

* Cola persistente de operaciones.
* Identificadores idempotentes.
* Reintentos con espera progresiva.
* Tratamiento de errores recuperables.
* Detección de conectividad.
* Resolución de conflictos.
* Confirmación de operaciones remotas.
* Registro de errores sin datos sensibles.
* Recuperación tras interrupciones.
* Sincronización selectiva.

Define qué entidad tiene prioridad en cada conflicto.
El progreso local del usuario no debe desaparecer por un conflicto de sincronización.
Las operaciones que afectan al XP deben ser transaccionales y no duplicables.
Si dos dispositivos completan el mismo reto, el servidor debe resolver el evento de forma determinista.
No dependas únicamente del reloj del dispositivo para validar operaciones competitivas.
17. Notificaciones
Implementa notificaciones locales configurables para:

* Retos pendientes.
* Horarios elegidos.
* Hábitos recurrentes.
* Recordatorios de recuperación.
* Logros.
* Actividad social autorizada.

Solicita permisos en el contexto adecuado y después de explicar el beneficio.
No solicites permisos al abrir la aplicación por primera vez sin justificación.
Respeta los horarios silenciosos, la zona horaria, la frecuencia elegida y las preferencias de accesibilidad.
Evita recordatorios culpabilizantes.
Si una notificación remota necesita infraestructura adicional, documenta e implementa esa infraestructura.
18. Privacidad, seguridad y cumplimiento
Aplica privacidad desde el diseño.
Requisitos:

* Recopilación mínima de datos.
* Transporte cifrado.
* Protección de credenciales.
* Políticas de acceso por usuario.
* Validación de entradas.
* Límites de frecuencia en operaciones sensibles.
* Protección frente a abuso.
* Manejo seguro de errores.
* Dependencias auditables.
* Registro de eventos sin información personal innecesaria.
* Exportación de datos.
* Eliminación de cuenta y datos.
* Política de privacidad real.
* Condiciones de servicio cuando correspondan.

El usuario debe poder eliminar su cuenta desde la aplicación cuando corresponda a las políticas de las tiendas.
La eliminación debe cubrir los datos personales asociados, el progreso remoto y las relaciones sociales conforme a la política de retención definida.
Explica de manera transparente qué datos permanecen exclusivamente en el dispositivo, cuáles se sincronizan y qué datos procesa cada proveedor externo.
No inventes declaraciones legales ni afirmes que la aplicación cumple una normativa sin verificar los requisitos aplicables.
19. Modelo de monetización
Diseña una arquitectura que permita activar un modelo freemium posteriormente.
La versión gratuita debe permitir desarrollar hábitos, completar retos, ganar XP, progresar y utilizar las funciones esenciales.
Prepara la posibilidad de añadir:

* Personalización visual.
* Temas y cosméticos.
* Estadísticas ampliadas.
* Funciones avanzadas de organización.
* Funciones de IA con límites de uso transparentes.
* Suscripciones opcionales.

No actives compras simuladas ni muestres precios falsos.
Si se implementan productos digitales, integra las compras dentro de la aplicación conforme a las políticas vigentes de Google Play y Apple, incluyendo restauración de compras, validación de recibos o transacciones y gestión de suscripciones.
El sistema debe seguir funcionando offline con las funciones locales disponibles.
20. Preparación integral para Google Play
El proyecto debe quedar técnicamente preparado para su distribución pública.
Identidad de la aplicación
Configura:

* Nombre: LevelUp.
* Nombre visible localizado.
* Identificador de paquete único y estable.
* Versionado.
* Icono de lanzamiento.
* Splash screen.
* Nombre del desarrollador configurable.
* Enlaces legales.
* Configuración de producción separada de desarrollo.

Utiliza un identificador de paquete configurable, con un valor inicial documentado que pueda cambiarse antes de la primera publicación.
No utilices identificadores de ejemplo como si fueran definitivos.
Compilación y firma
Configura:

* EAS Build.
* Perfil de desarrollo.
* Perfil de pruebas.
* Perfil de producción.
* Compilación Android App Bundle.
* Firma de producción.
* Gestión segura de credenciales.
* Variables de entorno.
* Generación reproducible de versiones.
* Comprobaciones previas a la publicación.

No guardes claves de firma ni credenciales privadas en Git.
Google Play Console
Prepara una guía y todos los archivos necesarios para configurar:

* Ficha principal.
* Descripción breve y completa en español.
* Localización al inglés cuando corresponda.
* Icono de tienda.
* Capturas reales.
* Gráfico promocional cuando sea requerido.
* Categoría y etiquetas apropiadas.
* Datos de contacto.
* URL de política de privacidad.
* Clasificación por edades.
* Público objetivo.
* Declaración de anuncios.
* Seguridad de datos.
* Permisos y justificaciones.
* Declaraciones de funciones sensibles, si aplican.
* Información de acceso para revisores.
* Pistas de pruebas.
* Notas de versión.
* Lanzamiento de producción.

Genera los recursos gráficos requeridos con sus dimensiones correctas y valida las restricciones vigentes de la tienda antes de producirlos.
No marques que la aplicación no recopila datos si la autenticación, el backend, la analítica o los SDK realmente los recopilan.
Pruebas y acceso a producción
Incluye una guía para probar la aplicación con dispositivos reales y configurar las pistas internas y cerradas.
Si la cuenta personal de Google Play está sujeta a requisitos de prueba cerrada, documenta el número de participantes, el periodo requerido y el proceso de solicitud de acceso a producción.
No asumas que todos los tipos de cuenta tienen los mismos requisitos.
21. Preparación integral para Apple App Store
Prepara la configuración necesaria para compilar, firmar, probar y distribuir la versión de iOS.
Configuración iOS
Implementa y documenta:

* Bundle Identifier.
* Nombre de aplicación.
* Versionado.
* Icono de iOS.
* Launch Screen.
* Identificadores y capacidades necesarias.
* Configuración de permisos.
* Descripciones de uso para los permisos requeridos.
* Firma y perfiles de aprovisionamiento.
* Variables de entorno.
* Configuración EAS para iOS.
* Archivo de distribución.
* Proceso de carga en App Store Connect.

Utiliza únicamente capacidades justificadas por las funciones reales de la aplicación.
No solicites acceso a contactos, ubicación, salud, cámara, micrófono u otros datos sensibles si no son necesarios.
App Store Connect
Prepara una guía de configuración que incluya:

* Registro de la aplicación.
* Nombre y subtítulo.
* Categoría.
* Descripción.
* Palabras clave.
* URL de soporte.
* URL de política de privacidad.
* Icono y capturas.
* Localización.
* Clasificación por edad.
* Declaración de privacidad.
* Información de accesibilidad.
* Información sobre compras integradas, si existen.
* Notas para revisión.
* Cuenta de prueba para revisores, cuando corresponda.
* TestFlight.
* Preparación del envío a revisión.
* Publicación y seguimiento posterior.

Las capturas deben mostrar la aplicación real en funcionamiento, no solamente ilustraciones promocionales o pantallas ficticias.
Comprueba las dimensiones y requisitos actuales de cada formato de dispositivo antes de generar las capturas.
Compras y autenticación
Si la aplicación incorpora autenticación mediante proveedores externos, analiza las obligaciones vigentes de Apple para el inicio de sesión y las alternativas requeridas.
Si habilita compras digitales, utiliza las herramientas de compra integradas correspondientes y verifica las políticas aplicables.
No declares una función terminada si falta su configuración en el portal correspondiente.
22. Recursos comerciales y legales
Crea una carpeta de documentación de publicación con:

* Descripción corta.
* Descripción larga.
* Propuesta de valor.
* Palabras clave.
* Texto promocional.
* Textos de capturas.
* Plan de localización.
* Política de privacidad inicial.
* Condiciones de servicio iniciales.
* Información de contacto configurable.
* Guía de clasificación por edades.
* Guía de declaración de datos.
* Notas de revisión.
* Notas de versión.
* Lista de verificación de publicación.

Los documentos legales deben estar adaptados a las funciones reales, los proveedores y los territorios de distribución.
No inventes direcciones, empresas, identificadores fiscales ni datos de contacto.
Marca expresamente cualquier dato que el propietario deba completar o validar antes del envío.
23. Automatización y entrega continua
Implementa automatización mediante GitHub Actions o una alternativa compatible con el repositorio.
Incluye, como mínimo:

* Instalación reproducible de dependencias.
* Verificación de TypeScript.
* Linter.
* Pruebas unitarias.
* Pruebas de integración.
* Validación de migraciones.
* Comprobaciones de configuración.
* Auditoría de dependencias.
* Construcción de artefactos.
* Validación previa a distribución.

Las credenciales de publicación deben almacenarse en secretos del sistema de CI, nunca en el código.
Configura EAS Submit o el proceso equivalente para facilitar las cargas de Android e iOS.
No publiques automáticamente una versión en producción sin una aprobación explícita del propietario.
24. Estrategia de pruebas
Implementa pruebas automatizadas para las reglas críticas.
Pruebas unitarias

* Cálculo de XP.
* Subida de niveles.
* Desbloqueo de logros.
* Selección de retos.
* Frecuencias recurrentes.
* Rachas y descansos.
* Validación de retos personalizados.
* Detección de duplicados.
* Adaptación de dificultad.

Pruebas de integración

* Persistencia SQLite.
* Migraciones.
* Operaciones transaccionales.
* Inicio y cierre de sesión.
* Vinculación de cuentas.
* Sincronización.
* Resolución de conflictos.
* Políticas RLS.
* Solicitudes de amistad.
* Bloqueos.
* Eliminación de cuenta.

Pruebas de extremo a extremo

* Primera apertura.
* Animación de bienvenida.
* Omisión de la bienvenida.
* Onboarding completo.
* Onboarding interrumpido.
* Modo invitado.
* Registro y autenticación.
* Completar un reto offline.
* Reiniciar la aplicación sin conexión.
* Recuperar conectividad.
* Sincronizar el progreso.
* Desbloquear un trofeo.
* Modificar ajustes de accesibilidad.
* Gestionar amigos.
* Eliminar una cuenta.

Pruebas de accesibilidad
Comprueba lectores de pantalla, texto ampliado, contraste, áreas táctiles, orden de navegación, reducción de movimiento y mensajes de error.
Pruebas de rendimiento
Verifica tiempos de apertura, uso de memoria, rendimiento de listas, consultas locales y comportamiento durante sincronizaciones extensas.
No afirmes haber ejecutado pruebas que no se hayan ejecutado realmente.
25. Criterios de aceptación
La versión inicial no se considerará lista hasta cumplir estos requisitos:

1. La bienvenida funciona y puede omitirse.
2. El onboarding conserva su progreso.
3. Los objetivos se pueden modificar.
4. Existen retos locales suficientes para funcionar sin internet.
5. Se puede completar un reto sin conexión.
6. La experiencia y los logros no se duplican al sincronizar.
7. Los datos locales persisten después de cerrar la aplicación.
8. El usuario puede utilizar la aplicación en modo invitado.
9. El registro y la autenticación funcionan con un backend configurado.
10. Las funciones sociales respetan los permisos y bloqueos.
11. Las preferencias de accesibilidad modifican realmente la experiencia.
12. Los recordatorios respetan la configuración del usuario.
13. La aplicación incluye exportación y eliminación de datos.
14. Las migraciones y las pruebas automatizadas funcionan.
15. Android e iOS tienen configuraciones de compilación documentadas.
16. Las fichas comerciales y los recursos de tienda están preparados.
17. Las credenciales y los secretos están protegidos.
18. Los errores de red no provocan pérdida de datos.
19. No existen botones que aparenten funcionar sin una implementación real.
20. Se documentan las limitaciones, dependencias y tareas pendientes.

26. Organización del trabajo
Desarrolla el proyecto por fases.
Fase 1: análisis y planificación
Inspecciona el repositorio, determina el estado actual y documenta la arquitectura propuesta.
Define la estructura de directorios, dependencias, modelo de datos, navegación y estrategia de sincronización.
Fase 2: identidad y experiencia inicial
Implementa el sistema visual, la bienvenida animada, el onboarding, la navegación y los ajustes de accesibilidad.
Fase 3: núcleo offline-first
Implementa SQLite, migraciones, catálogo local, retos, hábitos, persistencia y motor de progresión.
Fase 4: gamificación
Implementa XP, niveles, atributos, logros, trofeos, rachas y estadísticas.
Fase 5: backend y cuentas
Implementa Supabase, autenticación, sincronización, conflictos, seguridad y recuperación de datos.
Fase 6: funciones sociales
Implementa amistades, perfiles, permisos, retos cooperativos, bloqueos y notificaciones sociales.
Fase 7: calidad y accesibilidad
Completa pruebas automatizadas, pruebas en dispositivos reales, optimización y auditoría de seguridad.
Fase 8: distribución
Prepara las compilaciones, firmas, recursos comerciales, documentación legal, TestFlight, Google Play Console y App Store Connect.
En cada fase:

1. Explica brevemente el objetivo.
2. Implementa código real.
3. Ejecuta las pruebas disponibles.
4. Corrige los errores detectados.
5. Actualiza la documentación.
6. Indica lo que funciona.
7. Identifica las tareas pendientes.
8. Continúa con la siguiente fase cuando las dependencias estén satisfechas.

No detengas el desarrollo para solicitar confirmación sobre decisiones técnicas ordinarias. Utiliza valores predeterminados razonables y documenta las decisiones reversibles.
Pregunta únicamente cuando falte una credencial, un dato legal, una decisión irreversible o un recurso externo que realmente impida continuar.
No inventes resultados de compilación, pruebas, cargas o publicaciones.
27. Entregables finales
Entrega:

* Aplicación móvil funcional.
* Código fuente organizado.
* Configuración Android e iOS.
* Sistema de diseño.
* Animación de bienvenida.
* Onboarding personalizado.
* Motor de retos.
* Persistencia offline-first.
* Sistema de progresión.
* Backend y migraciones.
* Configuración de seguridad.
* Pruebas automatizadas.
* Configuración de CI/CD.
* Recursos comerciales para ambas tiendas.
* Documentación de Google Play.
* Documentación de Apple App Store.
* Documentación de variables de entorno.
* Instrucciones de instalación y desarrollo.
* Guía de compilación y distribución.
* Lista de requisitos pendientes del propietario.

El resultado debe ser una base de producto mantenible, segura, accesible y lista para evolucionar.
Prioridad final: primero garantiza una experiencia excelente y funcional sin conexión; después, integra de forma segura la nube, la interacción social y las funciones avanzadas. Nunca sacrifiques la fiabilidad del progreso personal por añadir más gamificación o complejidad visual.
