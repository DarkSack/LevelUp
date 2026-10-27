# Publicar LevelUp en Google Play

Lo que pide Play Console, listo para copiar. Lo que solo puede hacer el dueño de la cuenta está
marcado con **Tú**. Antes de subir nada, revisar [PENDIENTES.md](../PENDIENTES.md) (backend, producto de Plus, clave de licencia).

## 1. El paquete

| | |
|---|---|
| Archivo para Play | `E:\Dev\levelup\androidApp\build\outputs\bundle\release\androidApp-release.aab` (se sube este, no un APK) |
| Reloj (Wear OS) | `E:\Dev\levelup\wearApp\build\outputs\bundle\release\wearApp-release.aab` (canal de Wear OS, ver la sección 5) |
| Generarlo | `./gradlew :androidApp:bundleRelease :wearApp:bundleRelease` |
| Paquete y versión | `sack.levelup`, versionName 1.0.0, versionCode 1 (reloj: 1.0.0, 100001) |
| SDK | mínimo 26 (Android 8), objetivo 36 |
| Firma | Llave de subida ya creada (`keystore.properties`, fuera de git). **Tú**: respaldar el `.jks` y su contraseña fuera del equipo |
| Cuenta | Opcional: correo o nombre de usuario y contraseña, sin verificación por correo ni recuperación de contraseña. En Supabase «Confirm email» tiene que estar apagado; `cuenta=no` en `supabase.properties` compilaría sin la nube |
| Configuración | `supabase.properties` y `play.properties` en la raíz (fuera de git) antes de compilar el release. La clave de `play.properties` solo existe tras crear la app en Play Console: el primer bundle sale sin ella (sirve para la prueba interna, pero no comprueba las compras de Plus); después de copiarla hay que generar y subir otro |

### Notas de la versión 1.0.0

Se pegan tal cual en «Notas de la versión» al crear cada versión. `es-419` es español (Latinoamérica), el idioma
predeterminado de la ficha, y van también en inglés (`en-US`) y portugués (`pt-BR`), los otros idiomas de la app;
Play admite 500 caracteres por idioma (teléfono: 479, 486 y 499; reloj: 166, 200 y 180).

Teléfono:

```
<es-419>
Primera versión de LevelUp.
• Más de 130 retos diarios en nueve áreas, según tus objetivos y tu tiempo.
• Experiencia, niveles, títulos, 53 logros (4 secretos) y misiones.
• Sin castigos: simplifica, cambia o pospón un reto; los descansos no rompen tu racha.
• Metas, favoritos, revisión semanal, widget de hoy y atajos.
• Pasos y ejercicio de Health Connect en los retos físicos (opcional).
• Cuenta opcional para sincronizar y retarte con amigos.
• También en tu reloj Wear OS.
</es-419>
<en-US>
First release of LevelUp.
• Over 130 daily challenges in nine areas, matched to your goals and your time.
• Experience, levels, titles, 53 achievements (4 secret) and missions.
• No penalties: simplify, swap or postpone a challenge; rest days don't break your streak.
• Goals, favorites, weekly review, today widget and shortcuts.
• Health Connect steps and exercise in physical challenges (optional).
• Optional account to sync and challenge your friends.
• Also on your Wear OS watch.
</en-US>
<pt-BR>
Primeira versão do LevelUp.
• Mais de 130 desafios diários em nove áreas, conforme seus objetivos e seu tempo.
• Experiência, níveis, títulos, 53 conquistas (4 secretas) e missões.
• Sem castigos: simplifique, troque ou adie um desafio; o descanso não quebra sua sequência.
• Metas, favoritos, revisão semanal, widget de hoje e atalhos.
• Passos e exercício do Health Connect nos desafios físicos (opcional).
• Conta opcional para sincronizar e desafiar seus amigos.
• Também no seu relógio Wear OS.
</pt-BR>
```

Reloj (canal de Wear OS):

```
<es-419>
Primera versión para Wear OS.
• Ve los retos y metas de hoy en tu reloj y completa un reto sin sacar el teléfono.
• Complicación para tu esfera con tu avance del día.
</es-419>
<en-US>
First release for Wear OS.
• See today's challenges and goals on your watch and complete a challenge without taking out your phone.
• A complication for your watch face with your progress for the day.
</en-US>
<pt-BR>
Primeira versão para Wear OS.
• Veja os desafios e metas de hoje no relógio e conclua um desafio sem tirar o celular do bolso.
• Complicação para o mostrador com seu avanço do dia.
</pt-BR>
```

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
> • Niveles generales y por categoría, títulos, 53 logros con trofeos de bronce a platino (algunos secretos), misiones de varios días y marcos para tu avatar.
> • Estilo tranquilo para quien prefiere menos celebraciones.
>
> PENSADA PARA CADA CABEZA
> • Pasos visibles, temporizador opcional, modo sencillo y avisos amables, útiles si te cuesta empezar o
>   mantener la atención.
> • Texto más grande, alto contraste, menos movimiento y compatibilidad con TalkBack.
>
> CON AMIGOS, SI QUIERES
> • Cuenta opcional, con correo o nombre de usuario, para sincronizar tus teléfonos.
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

## 2.1 Ficha en inglés y portugués

La app sigue el idioma del teléfono (español, inglés o portugués de Brasil; en Android 13+ también se elige solo para
LevelUp). En Play Console: Ficha principal → **Administrar traducciones → Agregar tus propias traducciones** →
`en-US` y `pt-BR`, y pegar:

**en-US**: nombre `LevelUp`; descripción breve:

> Small challenges to build habits. Level up at your pace, account optional.

> Every small step counts. Each day LevelUp suggests a few concrete challenges, chosen to fit your goals and the
> time you have, and turns your consistency into progress: experience, levels, titles, achievements and missions.
>
> CHALLENGES THAT FIT YOU
> • Over 130 challenges in nine areas: physical, mind, social, productivity, wellbeing, creativity, finances,
>   digital life and home.
> • You choose how many challenges a day and how much time; the difficulty adjusts to how they're going.
> • Create your own challenges and habits with fixed days.
>
> NO PENALTIES
> • A hard day? Simplify the challenge, swap it or leave it for another day.
> • Rest days and breaks don't break your streak. The experience you earn is never lost.
>
> PROGRESS YOU CAN SEE
> • Overall and per-category levels, titles, 53 achievements with bronze to platinum trophies (some of them secret), multi-day missions and frames for your avatar.
> • A calm style for anyone who prefers fewer celebrations.
>
> DESIGNED FOR EVERY MIND
> • Visible steps, an optional timer, simple mode and gentle reminders, helpful if starting or keeping focus is
>   hard for you.
> • Larger text, high contrast, reduced motion and TalkBack support.
>
> WITH FRIENDS, IF YOU WANT
> • Optional account, with email or username, to sync your phones.
> • Friends, cheers, group challenges and a weekly leaderboard you only join if you want.
>
> PRIVATE
> • No ads and no tracking. Without an account it works offline and your data stays on your phone.
> • Export a backup whenever you want.
>
> LEVELUP PLUS (OPTIONAL)
> • One-time purchase: exclusive frames, app colors and extended stats. Nothing in Plus is needed to make
>   progress.

**pt-BR**: nome `LevelUp`; descrição breve:

> Pequenos desafios para criar hábitos. Evolua no seu ritmo, com ou sem conta.

> Cada pequeno passo conta. Todo dia o LevelUp sugere alguns desafios concretos, escolhidos conforme seus objetivos
> e o tempo que você tem, e transforma sua constância em progresso: experiência, níveis, títulos, conquistas e
> missões.
>
> DESAFIOS NA SUA MEDIDA
> • Mais de 130 desafios em nove áreas: físico, mente, social, produtividade, bem-estar, criatividade, finanças,
>   vida digital e casa.
> • Você escolhe quantos desafios por dia e quanto tempo; a dificuldade se ajusta conforme eles vão indo.
> • Crie seus próprios desafios e hábitos com dias fixos.
>
> SEM CASTIGOS
> • Um dia difícil? Simplifique o desafio, troque ou deixe para outro dia.
> • Dias de descanso e pausas que não quebram sua sequência. A experiência ganha nunca se perde.
>
> PROGRESSO QUE SE VÊ
> • Níveis gerais e por categoria, títulos, 53 conquistas com troféus de bronze a platina (algumas secretas), missões de vários dias e molduras para o seu avatar.
> • Estilo tranquilo para quem prefere menos comemorações.
>
> PENSADO PARA CADA CABEÇA
> • Passos visíveis, temporizador opcional, modo simples e avisos gentis, úteis se for difícil começar ou manter
>   a atenção.
> • Texto maior, alto contraste, menos movimento e compatibilidade com o TalkBack.
>
> COM AMIGOS, SE VOCÊ QUISER
> • Conta opcional, com e-mail ou nome de usuário, para sincronizar seus celulares.
> • Amigos, parabéns, desafios em grupo e uma classificação semanal da qual você só participa se quiser.
>
> PRIVADO
> • Sem anúncios e sem rastreamento. Sem conta funciona sem internet e seus dados ficam no seu celular.
> • Exporte um backup quando quiser.
>
> LEVELUP PLUS (OPCIONAL)
> • Compra única: molduras exclusivas, cores para o app e estatísticas ampliadas. Nada do Plus é necessário para
>   progredir.

Las capturas (sección 6) están en español: sirven para las tres fichas mientras no haya otras.

## 3. Contenido de la app

| Sección | Respuesta |
|---|---|
| Política de privacidad | https://shop.sackito.online/privacidad/levelup (copia de [privacidad.md](privacidad.md) en `lib/privacidad-apps.ts` de rpgroll-shop: si cambia una, cambiar la otra) |
| Condiciones de uso | https://shop.sackito.online/condiciones/levelup (copia de [condiciones.md](condiciones.md) en `lib/condiciones-apps.ts` de rpgroll-shop; la app la enlaza al crear la cuenta). Play no la pide, pero conviene tenerla a mano |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todo disponible sin cuenta. Lo social (amigos, grupos, clasificación) necesita cuenta, pero cualquiera la crea en la app con un nombre de usuario y una contraseña, sin verificación por correo: no hacen falta credenciales de revisión |
| Clasificación de contenido | Cuestionario IARC: sin violencia ni ubicación. **Sí** a compras digitales. **Sí** a interacción entre usuarios, limitada: solo reacciones y ánimos predefinidos, sin chat ni texto libre (lo único que escribe cada persona es su nombre de usuario y su nombre visible), con bloqueo y denuncia |
| Público objetivo | 13 años o más. No dirigida a menores de 13 |
| Borrado de cuenta | En la app (Perfil → Cuenta y sincronización → Borrar mi cuenta) y, sin la app, por correo a donsackito@protonmail.com: URL https://shop.sackito.online/privacidad/levelup (sección «Tus derechos y cómo borrar tus datos») |
| Apps de salud | Retos de bienestar general, sin funciones médicas. **Declaración de Health Connect obligatoria**: la app pide `READ_STEPS` y `READ_EXERCISE` (opcional, en Laboratorio) para mostrar los pasos y minutos de ejercicio de hoy en los retos físicos; solo lectura, procesado en el teléfono, sin guardar ni subir nada. Play revisa este formulario aparte |

## 4. Seguridad de los datos

Con cuenta (opcional) los datos van a Supabase, cifrados en tránsito (HTTPS). Nada se comparte con terceros (Supabase es un proveedor que trata los datos
por cuenta del desarrollador, lo que Play no cuenta como «compartir»).

| Pregunta | Respuesta propuesta |
|---|---|
| ¿Recopila o comparte datos? | Recopila: **Sí**. Comparte: **No** |
| ¿Cifrados en tránsito? | Sí |
| ¿El usuario puede pedir que se borren? | Sí (en la app y por correo) |
| ¿Recopilación opcional? | Sí en todos: solo si crea una cuenta |

| Tipo de dato (Play) | Para qué |
|---|---|
| Información personal → Dirección de correo (solo si la usa para entrar; también vale un nombre de usuario) | Gestión de la cuenta |
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
the daily challenges appear on Home. The account is optional and needs no email verification: Profile > Account and sync >
Create an account, with any username and password, unlocks Friends and groups (only predefined reactions, no chat).
LevelUp Plus (one-time in-app purchase, product id levelup_plus) only unlocks decorative frames, app colors and
extended stats: Profile > LevelUp Plus. Health Connect (steps and exercise minutes, read-only, processed on the
device) is optional and lives in Profile > Lab. The Wear OS app shows today's challenges sent by the phone.
The app follows the phone language (Spanish, English or Brazilian Portuguese); with the phone in English the
menu names above appear exactly as written.
```

## 8. Lista antes de publicar

- [x] `supabase.properties` sin `cuenta=no`, y en Supabase «Confirm email» apagado (Authentication → Sign In / Providers → Email).
- [ ] `play.properties` con la clave de licencia de Play y bundles regenerados después de copiarla.
- [ ] Pruebas en verde: `./gradlew :shared:testAndroidHostTest` (y con `-PsupabaseLocalKey` si cambió la nube).
- [ ] Bundles: `./gradlew :androidApp:bundleRelease :wearApp:bundleRelease`; `versionCode` subido en los dos.
- [ ] Notas de la versión (sección 1) y capturas al día (sección 6).
- [ ] Contenido de la app: privacidad, anuncios, acceso, IARC, público, Health Connect (sección 3).
- [ ] Seguridad de los datos (sección 4).
- [ ] Producto `levelup_plus` activo y probado con una cuenta de prueba de licencias.
- [ ] Primero prueba interna (y la cerrada de 12 personas durante 14 días si la cuenta de Play es personal y nueva).
- [ ] Instalar el AAB de la prueba interna en un teléfono real y repasar lo de «Pruebas que faltan» en PENDIENTES.

## 9. Recuperar contraseñas (más adelante)

La 1.0.0 no envía ningún correo (decisión de Sack, 2026-10-26): la cuenta es correo o nombre de usuario y contraseña,
sin verificación, y una contraseña olvidada no se recupera (se avisa al registrarse; con la sesión iniciada sí se
cambia). Para añadirlo: correo propio (SMTP, ver PENDIENTES.md), las plantillas de `supabase/templates/` y volver a
poner «Olvidé mi contraseña» (estaba en `AccountScreen.kt` hasta el commit b8c41ab). Las cuentas con nombre de usuario
no tienen correo real: para ellas haría falta pedir un correo antes.
