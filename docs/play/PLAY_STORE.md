# Publicar LevelUp en Google Play

Lo que pide Play Console, listo para copiar. Lo que solo puede hacer el dueño de la cuenta está
marcado con **Tú**. Antes de subir nada, revisar [PENDIENTES.md](../PENDIENTES.md) (backend, producto de Plus, clave de licencia).

## 1. El paquete

| | |
|---|---|
| Archivo para Play | `E:\Dev\levelup\androidApp\build\outputs\bundle\release\androidApp-release.aab` (se sube este, no un APK) |
| Reloj (Wear OS) | `E:\Dev\levelup\wearApp\build\outputs\bundle\release\wearApp-release.aab` (canal de Wear OS, ver la sección 5) |
| Generarlo | `./gradlew :androidApp:bundleRelease :wearApp:bundleRelease` |
| Paquete y versión | `sack.levelup`, versionName 0.1.0, versionCode 1 |
| SDK | mínimo 26 (Android 8), objetivo 36 |
| Firma | Llave de subida ya creada (`keystore.properties`, fuera de git). **Tú**: respaldar el `.jks` y su contraseña fuera del equipo |
| Sin cuenta | La 0.1.0 sale **sin cuenta**: `cuenta=no` en `supabase.properties` compila sin la nube (no aparecen cuenta, amigos ni grupos) mientras el registro no funcione en producción (falta el SMTP propio). Al quitarlo, cambiar lo marcado en la sección 9 |
| Configuración | `supabase.properties` y `play.properties` en la raíz (fuera de git) antes de compilar el release. La clave de `play.properties` solo existe tras crear la app en Play Console: el primer bundle sale sin ella (sirve para la prueba interna, pero no comprueba las compras de Plus); después de copiarla hay que generar y subir otro |

### Notas de la versión 0.1.0

Se pegan tal cual en «Notas de la versión» al crear cada versión. `es-419` es español (Latinoamérica), el idioma
predeterminado de la ficha; Play admite 500 caracteres por idioma (teléfono: 466; reloj: 166).

Teléfono:

```
<es-419>
Primera versión de LevelUp.
• Más de 130 retos diarios en nueve áreas, según tus objetivos y tu tiempo.
• Experiencia, niveles, títulos, 53 logros (4 secretos) y misiones.
• Sin castigos: simplifica, cambia o pospón un reto; los descansos no rompen tu racha.
• Metas, favoritos, revisión semanal, widget de hoy y atajos.
• Pasos y ejercicio de Health Connect en los retos físicos (opcional).
• Sin cuenta: tus datos se quedan en tu teléfono.
• También en tu reloj Wear OS.
</es-419>
```

Reloj (canal de Wear OS):

```
<es-419>
Primera versión para Wear OS.
• Ve los retos y metas de hoy en tu reloj y completa un reto sin sacar el teléfono.
• Complicación para tu esfera con tu avance del día.
</es-419>
```

## 2. Ficha (español)

**Nombre** (30 máx.): `LevelUp`

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
> • Niveles generales y por categoría, títulos, 53 logros con trofeos de bronce a platino (algunos secretos), misiones de varios días y marcos para tu avatar.
> • Estilo tranquilo para quien prefiere menos celebraciones.
>
> PENSADA PARA CADA CABEZA
> • Pasos visibles, temporizador opcional, modo sencillo y avisos amables, útiles si te cuesta empezar o
>   mantener la atención.
> • Texto más grande, alto contraste, menos movimiento y compatibilidad con TalkBack.
>
> PRIVADO
> • Sin anuncios, sin rastreo y sin cuenta. Funciona sin internet y tus datos se quedan en tu teléfono.
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
| Política de privacidad | https://shop.sackito.online/privacidad/levelup (copia de [privacidad.md](privacidad.md) en `lib/privacidad-apps.ts` de rpgroll-shop: si cambia una, cambiar la otra) |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todo disponible sin restricciones: la 0.1.0 no tiene cuenta ni inicio de sesión |
| Clasificación de contenido | Cuestionario IARC: sin violencia ni ubicación. **Sí** a compras digitales. **No** a interacción entre usuarios |
| Público objetivo | 13 años o más. No dirigida a menores de 13 |
| Borrado de cuenta | No aplica: la 0.1.0 no permite crear cuentas |
| Apps de salud | Retos de bienestar general, sin funciones médicas. **Declaración de Health Connect obligatoria**: la app pide `READ_STEPS` y `READ_EXERCISE` (opcional, en Laboratorio) para mostrar los pasos y minutos de ejercicio de hoy en los retos físicos; solo lectura, procesado en el teléfono, sin guardar ni subir nada. Play revisa este formulario aparte |

## 4. Seguridad de los datos

La 0.1.0 no tiene cuenta: nada sale del teléfono. Respuesta: **Recopila: No. Comparte: No.** Los pasos y el
ejercicio de Health Connect se leen y se muestran solo en el teléfono (no se guardan ni se envían): según la ayuda
de Play, lo que se procesa solo en el dispositivo no cuenta como recopilado, pero **Tú** confírmalo al rellenarlo.

**Cuando vuelva la cuenta** (sin `cuenta=no`), el formulario pasa a ser este. Con cuenta (opcional) los datos van a
Supabase, cifrados en tránsito (HTTPS). Nada se comparte con terceros (Supabase es un proveedor que trata los datos
por cuenta del desarrollador, lo que Play no cuenta como «compartir»).

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
| Actividad en la app → Otro contenido generado por el usuario (retos propios, notas, metas, favoritos, nombre de usuario) | Funciones de la app |

No se recopilan ubicación, contactos, datos financieros (el pago lo gestiona Google Play), identificadores del
dispositivo ni datos de diagnóstico. Los pasos y el ejercicio de Health Connect se leen y se muestran solo en el
teléfono (no se guardan ni se envían): según la ayuda de Play, lo que se procesa solo en el dispositivo no cuenta
como recopilado, pero **Tú** confírmalo al rellenarlo.

**Tú**: repasa la lista con la ayuda vigente de Play Console al rellenarla; las categorías cambian a veces.

## 5. Reloj (Wear OS)

El módulo `wearApp` se publica en la **misma ficha**, en el canal de Wear OS (Configuración avanzada → Factores de
forma → Wear OS), con su propio AAB (`./gradlew :wearApp:bundleRelease`, `versionCode` 100001 y siguientes, que no
pueden coincidir con los del teléfono). Pasa una revisión propia de Wear OS; sus capturas están en la sección 6.

## 6. Gráficos

En `E:\Dev\levelup\docs\play\capturas\`, sacados de la interfaz real con Robolectric (`StoreScreenshots`):

| Play | Archivo |
|---|---|
| Icono 512 × 512 | `icono-512.png` |
| Gráfico destacado 1024 × 500 | `grafico-1024x500.png` |
| Capturas del teléfono (1080 × 1920) | `0-bienvenida.png` … `5-metas.png`, en ese orden |
| Capturas del reloj (454 × 454, cuadradas) | `reloj-1-hoy.png`, `reloj-2-sin-datos.png` |

Regenerarlos (tras cambiar la interfaz):
`./gradlew :shared:testAndroidHostTest --tests "app.levelup.store.StoreScreenshots" -Pcapturas=E:/Dev/levelup/docs/play/capturas --rerun`.
Las del reloj (`WatchScreenshots`):
`./gradlew :wearApp:testDebugUnitTest --tests "app.levelup.wear.WatchScreenshots" -Pcapturas=E:/Dev/levelup/docs/play/capturas --rerun`.
`animacion/` guarda la bienvenida a 0, 250, 500, 800 y 1100 ms para revisar sus fases; no se sube.

## 7. Notas para la revisión

En inglés, para el campo de instrucciones de «Acceso a la app» (o las notas de la versión de prueba) si Play las pide:

```
LevelUp works fully without an account or sign-in: open the app, follow the short 6-step setup (each step can be skipped) and
the daily challenges appear on Home. There is no user-generated content shared with other people in this version.
LevelUp Plus (one-time in-app purchase, product id levelup_plus) only unlocks decorative frames, app colors and
extended stats: Profile > LevelUp Plus. Health Connect (steps and exercise minutes, read-only, processed on the
device) is optional and lives in Profile > Lab. The Wear OS app shows today's challenges sent by the phone.
```

## 8. Lista antes de publicar

- [ ] `supabase.properties` con `cuenta=no` (sin SMTP propio) o sin él (con SMTP y la sección 9 aplicada).
- [ ] `play.properties` con la clave de licencia de Play y bundles regenerados después de copiarla.
- [ ] Pruebas en verde: `./gradlew :shared:testAndroidHostTest` (y con `-PsupabaseLocalKey` si cambió la nube).
- [ ] Bundles: `./gradlew :androidApp:bundleRelease :wearApp:bundleRelease`; `versionCode` subido en los dos.
- [ ] Notas de la versión (sección 1) y capturas al día (sección 6).
- [ ] Contenido de la app: privacidad, anuncios, acceso, IARC, público, Health Connect (sección 3).
- [ ] Seguridad de los datos (sección 4).
- [ ] Producto `levelup_plus` activo y probado con una cuenta de prueba de licencias.
- [ ] Primero prueba interna (y la cerrada de 12 personas durante 14 días si la cuenta de Play es personal y nueva).
- [ ] Instalar el AAB de la prueba interna en un teléfono real y repasar lo de «Pruebas que faltan» en PENDIENTES.

## 9. Cuando vuelva la cuenta

Al quitar `cuenta=no` (con el SMTP propio funcionando; las condiciones de uso, [condiciones.md](condiciones.md), publicadas en https://shop.sackito.online/condiciones/levelup): en las notas, la línea «Sin cuenta…» pasa a «Cuenta opcional
para sincronizar y retarte con amigos.»; la descripción breve vuelve a «…a tu ritmo, con o sin cuenta.»; la completa
recupera antes de PRIVADO:

> CON AMIGOS, SI QUIERES
> • Cuenta opcional para sincronizar tus teléfonos.
> • Amigos, felicitaciones, retos en grupo y una clasificación semanal a la que solo entras si quieres.

y su primera línea de PRIVADO dice «Sin anuncios y sin rastreo. Sin cuenta funciona sin internet y tus datos se
quedan en tu teléfono.». En la sección 3: acceso «Todo disponible sin cuenta. Lo social necesita cuenta, pero
cualquiera puede crearla en la app; si Play pide credenciales de revisión, **Tú** crea una cuenta de prueba»; IARC
**Sí** a interacción entre usuarios, limitada (solo reacciones predefinidas, sin chat ni texto libre, con bloqueo y
denuncia); borrado de cuenta en la app (Cuenta y sincronización → Borrar mi cuenta) y la URL de la política (su
sección «Tus derechos y cómo borrar tus datos»). Seguridad de los datos: las tablas de la sección 4.
