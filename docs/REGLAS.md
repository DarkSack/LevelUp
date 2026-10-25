# Reglas del juego

Todo está en `shared/src/commonMain/kotlin/app/levelup/domain/` y tiene pruebas en `commonTest`.
Si cambias un número, cambia también esta tabla y la prueba correspondiente.

## XP y niveles (`XpRules`)

| Dificultad | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| XP al completar | 10 | 15 | 25 | 40 | 60 |

- **Nivel global:** para llegar al nivel L hacen falta `50 × L × (L − 1)` XP en total.
  Nivel 2 = 100, 3 = 300, 4 = 600, 5 = 1 000, 10 = 4 500. Con tres retos al día de dificultad 2 (unos 45 XP), el nivel 5
  llega en unas tres semanas.
- **Nivel por categoría:** misma fórmula con 25 en lugar de 50. Solo suman los retos de esa categoría.
- **Los niveles nunca bajan.** No hay XP negativa ni castigos.
- **Una acción, una vez:** el ledger de XP tiene `UNIQUE(tipo, id de origen)`. Completar dos veces el mismo reto, o
  repetir un evento, no vuelve a sumar.
- **Deshacer** un reto completado por error quita solo su XP (y la de regreso si era el único del día). Los logros
  ya ganados se conservan.
- **Versión mínima:** completar un reto en su versión mínima da la XP de dificultad 1.
- **XP de regreso:** el primer reto completado tras 14 días o más sin actividad suma 20 XP, una vez por día como
  mucho (id `COMEBACK:<fecha>`). Dos semanas y no una, para que no compense desaparecer cada semana.
- **Sin juego:** con el estilo «Sin juego» la XP y los logros se siguen calculando (por si se vuelve a otro estilo),
  pero no se muestran ni se anuncian.
- **Títulos** (nivel global): 1 Primeros pasos · 3 En marcha · 5 Con ritmo · 8 Constancia · 12 Rumbo firme ·
  16 Huella propia · 20 Inspiración · 30 Leyenda tranquila. Son frases, no medidas del valor de nadie.

## Selección diaria (`ChallengeEngine`)

1. Solo categorías elegidas (todas si no eligió ninguna) y retos disponibles hoy: requisito cumplido, los de una vez
   sin completar, los semanales sin hacer esta semana.
2. Dificultad: nunca más de un paso por encima de la del usuario en esa categoría.
3. Tiempo: la suma de minutos cabe en el tiempo diario. Cada elección deja 5 minutos por cada reto que falta.
4. Evita lo mostrado en los últimos 3 días y lo omitido o cambiado 2 veces en 14 días.
5. Pesos: dificultad exacta ×3 (±1 ×1), categoría prioritaria ×2, reto propio ×1,5, completado hace menos de una
   semana ×0,5, categoría ya elegida hoy ×0,25 (variedad).
6. Si faltan retos, relaja por etapas: primero permite repetir recientes y ampliar la dificultad hacia abajo; nunca
   hacia arriba. Si nada cabe en el tiempo, propone el reto más corto disponible.
7. El reto principal es el primero de una categoría prioritaria.
8. Misma fecha, misma selección (semilla = día).

Días: **descanso** (sin retos ni hábitos), **pausa** (igual), **día sin retos** (no está en "días de retos", pero sí
sus hábitos). En cualquiera, "Quiero un reto igualmente" añade uno.

**Sustituir:** otro reto de la misma categoría si se puede, nunca más difícil ni más largo. **Simplificar:** cambia
a la versión más pequeña del catálogo (cada reto de dificultad ≥ 2 tiene una). El reto vale la XP de la versión
elegida. Ninguno de los dos penaliza.

## Dificultad adaptativa (`AdaptiveDifficulty`)

Modos: **Manual** (nunca cambia), **Sugerir** (propone y el usuario acepta, por defecto), **Automática** (aplica el
cambio sola). En todos los casos:

- Ventana de 14 días, al menos 5 retos resueltos (hechos, omitidos o cambiados). Los que el usuario no tocó no cuentan:
  la inactividad no es un fallo.
- **Subir un paso:** ≥ 80 % hechos, ninguno valorado "difícil", ninguno simplificado y (la mitad valorados "fácil" o
  ≥ 10 hechos).
- **Bajar un paso:** ≥ 3 valorados "difícil", o < 50 % hechos con ≥ 3 omitidos/cambiados/simplificados.
- Después de un cambio, 7 días sin cambios. Siempre entre 1 y 5.

## Rachas y objetivo semanal (`Streaks`)

- Racha = días seguidos con al menos un reto. Los días de descanso y las pausas no suman ni cortan. Hoy sin actividad
  no corta (el día no ha terminado).
- Objetivo semanal = días activos por semana (3 por defecto). Se muestran las semanas cumplidas, no solo la racha.
- Perder una racha no quita nada. Con el estilo "Tranquilo" las rachas no se muestran.

## Logros, misiones y marcos (`Progress.kt`, `content/Rewards.kt`)

- 41 logros en 6 colecciones. Un logro se cumple cuando su métrica llega al objetivo; se evalúa al completar un
  reto, crear un reto propio, configurar descanso o una pausa y al empezar o completar una misión.
- Estados: **bloqueado** (falta el anterior de la serie), **disponible** (con progreso), **desbloqueado** (con fecha).
- Recompensa: 10 a 80 XP, una sola vez. Algunos dan un marco para el perfil.
- Ningún logro premia excesos (muchos retos en un día) ni interacción social obligatoria.
- **Misiones:** opcionales, una a la vez, con plazo de 7 a 14 días. Si vence no pasa nada y se puede volver a empezar.

## Regreso, pausas y revisión semanal

- **Retos de recuperación:** si el último reto hecho fue hace 7 días o más, la selección del día usa dificultad 1,
  como mucho 2 retos y 10 minutos en total, e Inicio muestra un aviso amable (sin hablar de lo perdido).
- **Volver de una pausa** (`PauseRules`): durante 30 días después de una pausa terminada, la app pregunta qué
  hábitos retomar. Los no elegidos pasan a «En pausa» y sus retos abiertos de hoy se marcan «para otro día».
  Los días de pausa no generan retos, así que no hay nada atrasado.
- **Revisión semanal** (`WeeklyReview`): se ofrece si la semana pasada (lunes a domingo) tuvo actividad y aún no
  se revisó. Con 5 retos o más en la semana: si quedó sin hacer el 40 % o más, propone uno menos al día; si se
  hizo el 90 % o más con al menos 3 valorados «fácil» (y más fáciles que difíciles), uno más. Con recordatorios
  activos: mover el aviso a media hora antes de la hora habitual de completar (mediana de 4 semanas, si difiere
  2 h o más y no cae en silencio) y avisar solo los días habituales (los que tienen 2 o más días activos en
  4 semanas, si son entre 2 y 5). Nada se aplica sin pulsar.
- **Reto sorpresa y «Probar algo nuevo»:** un reto del catálogo nunca completado, que no sea hábito, disponible y
  de dificultad como mucho una más que la elegida; las categorías que no sueles elegir pesan el triple.

## Recordatorios (`Reminders`)

- Apagados hasta que el usuario los activa; el permiso se pide en ese momento, después de explicar para qué sirve.
- Nunca en horas de silencio, días de descanso ni pausas, ni en los días de la semana que se quitan de «Días con
  aviso». Sin retos pendientes, no hay aviso.
- Intensidad **Baja:** un aviso con el primer reto. **Normal:** hasta tres títulos.
- Tras 3 días sin actividad, un aviso amable como mucho cada 3 días (desactivable). Nunca menciona rachas perdidas.
