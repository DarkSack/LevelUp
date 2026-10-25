# Publicar LevelUp en Google Play

Lo que pide Play Console, listo para copiar. Lo que solo puede hacer el dueño de la cuenta está
marcado con **Tú**. Antes de subir nada, revisar [PENDIENTES.md](../PENDIENTES.md) (backend, producto de Plus, clave de licencia).

## 1. El paquete

| | |
|---|---|
| Generarlo | `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab` |
| Paquete y versión | `sack.levelup`, versionName 0.1.0, versionCode 1 |
| SDK | mínimo 26 (Android 8), objetivo 36 |
| Firma | Llave de subida ya creada (`keystore.properties`, fuera de git). **Tú**: respaldar el `.jks` y su contraseña fuera del equipo |
| Configuración | `supabase.properties` y `play.properties` en la raíz (fuera de git) antes de compilar el release |

## 2. Ficha (español)

**Nombre** (30 máx.): `LevelUp`

**Descripción breve** (80 máx.):

> Retos pequeños para crear hábitos. Sube de nivel a tu ritmo, con o sin cuenta.

**Descripción completa:**

> Cada pequeño paso cuenta. LevelUp te propone cada día unos pocos retos concretos, elegidos según tus
> objetivos y el tiempo que tienes, y convierte tu constancia en progreso: experiencia, niveles, títulos,
> logros y misiones.
>
> RETOS A TU MEDIDA
> • Más de 130 retos en nueve áreas: físico, mental, social, productividad, bienestar, creatividad,
>   finanzas, vida digital y hogar.
> • Tú eliges cuántos retos al día y cuánto tiempo; la dificultad se ajusta según cómo te van.
> • Crea tus propios retos y hábitos con días fijos.
>
> SIN CASTIGOS
> • ¿Un día difícil? Simplifica el reto, cámbialo o déjalo para otro día.
> • Días de descanso y pausas que no rompen tu racha. La experiencia ganada nunca se pierde.
>
> PROGRESO QUE SE VE
> • Niveles generales y por categoría, títulos, 41 logros con trofeos, misiones de varios días y marcos para tu avatar.
> • Estilo tranquilo para quien prefiere menos celebraciones.
>
> PENSADA PARA CADA CABEZA
> • Pasos visibles, temporizador opcional, modo sencillo y avisos amables, útiles si te cuesta empezar o
>   mantener la atención.
> • Texto más grande, alto contraste, menos movimiento y compatibilidad con TalkBack.
>
> CON AMIGOS, SI QUIERES
> • Cuenta opcional para sincronizar tus teléfonos.
> • Amigos, felicitaciones, retos en grupo y una clasificación semanal a la que solo entras si quieres.
>
> PRIVADO
> • Sin anuncios y sin rastreo. Sin cuenta funciona sin internet y tus datos se quedan en tu teléfono.
> • Exporta un respaldo cuando quieras.
>
> LEVELUP PLUS (OPCIONAL)
> • Compra única: marcos exclusivos, colores para la app y estadísticas ampliadas. Nada de Plus es
>   necesario para progresar.

**Categoría:** Salud y bienestar o Productividad (**Tú** eliges). **Correo de contacto:** donsackito@protonmail.com.
**Desarrollador:** Johan Jafet Del Valle Santiago.

## 3. Contenido de la app

| Sección | Respuesta |
|---|---|
| Política de privacidad | URL pública de [privacidad.md](privacidad.md) (**Tú**: alojarla) |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todo disponible sin cuenta. Lo social necesita cuenta, pero cualquiera puede crearla en la app; si Play pide credenciales de revisión, **Tú** crea una cuenta de prueba |
| Clasificación de contenido | Cuestionario IARC: sin violencia ni ubicación. **Sí** a compras digitales. **Sí** a interacción entre usuarios, limitada: solo reacciones predefinidas, sin chat ni texto libre, con bloqueo y denuncia |
| Público objetivo | 13 años o más. No dirigida a menores de 13 |
| Borrado de cuenta | En la app (Cuenta y sincronización → Borrar mi cuenta) y una URL pública con cómo pedirlo (**Tú**, ver PENDIENTES.md) |
| Apps de salud | Declarar si Play lo pregunta: retos de bienestar general, sin funciones médicas ni datos de salud |

## 4. Seguridad de los datos

Sin cuenta nada sale del teléfono. Con cuenta (opcional) los datos van a Supabase, cifrados en tránsito
(HTTPS). Nada se comparte con terceros (Supabase es un proveedor que trata los datos por cuenta del
desarrollador, lo que Play no cuenta como «compartir»).

| Pregunta | Respuesta propuesta |
|---|---|
| ¿Recopila o comparte datos? | Recopila: **Sí**. Comparte: **No** |
| ¿Cifrados en tránsito? | Sí |
| ¿El usuario puede pedir que se borren? | Sí (en la app y por correo) |
| ¿Recopilación opcional? | Sí en todos: solo si crea una cuenta |

| Tipo de dato (Play) | Para qué |
|---|---|
| Información personal → Dirección de correo | Gestión de la cuenta |
| Información personal → Nombre (nombre visible del perfil social) | Funciones de la app (social) |
| Información personal → ID de usuario | Gestión de la cuenta, funciones de la app |
| Actividad en la app → Otras acciones (retos, XP, logros, rachas) | Funciones de la app (sincronización, social) |
| Actividad en la app → Otro contenido generado por el usuario (retos propios, nombre de usuario) | Funciones de la app |

No se recopilan ubicación, contactos, datos financieros (el pago lo gestiona Google Play), salud,
identificadores del dispositivo ni datos de diagnóstico.

**Tú**: repasa la lista con la ayuda vigente de Play Console al rellenarla; las categorías cambian a veces.

## 5. Gráficos

Pendientes: icono 512 × 512, gráfico destacado 1024 × 500 y capturas. Se pueden sacar de la interfaz real
con Robolectric, como en Moneytor (`StoreScreenshots`).
