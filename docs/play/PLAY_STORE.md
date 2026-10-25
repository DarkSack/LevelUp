# Publicar LevelUp en Google Play

Lo que pide Play Console, listo para copiar. Lo que solo puede hacer el dueño de la cuenta está
marcado con **Tú**. Antes de subir nada, resolver [PENDIENTES.md](../PENDIENTES.md) (identificador, nombre, llave).

## 1. El paquete

| | |
|---|---|
| Generarlo | `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab` |
| Paquete y versión | `app.levelup` (provisional), versionName 0.1.0, versionCode 1 |
| SDK | mínimo 26 (Android 8), objetivo 36 |
| Firma | `keystore.properties` en la raíz (fuera de git). **Tú**: crear la llave de subida y respaldarla fuera del equipo |

## 2. Ficha (español)

**Nombre** (30 máx., provisional): `LevelUp: hábitos y retos`

**Descripción breve** (80 máx.):

> Retos pequeños para crear hábitos. Sube de nivel a tu ritmo, sin cuenta.

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
> ACCESIBLE
> • Texto más grande, modo sencillo, alto contraste, menos movimiento y compatibilidad con TalkBack.
>
> PRIVADO
> • Sin cuenta, sin anuncios y sin rastreo. Funciona sin internet y tus datos se quedan en tu teléfono.
> • Exporta un respaldo cuando quieras.

**Categoría:** Salud y bienestar o Productividad (**Tú** eliges). **Correo de contacto:** **Tú**.

## 3. Contenido de la app

| Sección | Respuesta |
|---|---|
| Política de privacidad | URL pública de [privacidad.md](privacidad.md) (**Tú**: completar los datos pendientes y alojarla) |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todo disponible sin restricciones (no hay cuenta) |
| Clasificación de contenido | Cuestionario IARC: todo «No» (sin violencia, compras, interacción entre usuarios ni ubicación) |
| Público objetivo | **Tú** (recomendado 13+; ver PENDIENTES.md) |
| Apps de salud | Declarar si Play lo pregunta: retos de bienestar general, sin funciones médicas ni datos de salud |

## 4. Seguridad de los datos

La app no declara el permiso de internet: nada sale del teléfono salvo la copia de seguridad de Android
(que hace el sistema y el usuario controla) y los archivos que el usuario exporta.

| Pregunta | Respuesta propuesta |
|---|---|
| ¿Tu app recopila o comparte datos del usuario? | **No** |

**Tú**: confirma en la ayuda de Play Console vigente que la copia de seguridad de Android no obliga a
declarar nada; no se ha verificado.

## 5. Gráficos

Pendientes: icono 512 × 512, gráfico destacado 1024 × 500 y capturas. Se pueden sacar de la interfaz real
con Robolectric, como en Moneytor (`StoreScreenshots`).
