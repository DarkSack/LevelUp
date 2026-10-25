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
- **Deshacer** un reto completado por error quita solo su XP. Los logros ya ganados se conservan.
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

## Recordatorios (`Reminders`)

- Apagados hasta que el usuario los activa; el permiso se pide en ese momento, después de explicar para qué sirve.
- Nunca en horas de silencio, días de descanso ni pausas. Sin retos pendientes, no hay aviso.
- Intensidad **Baja:** un aviso con el primer reto. **Normal:** hasta tres títulos.
- Tras 3 días sin actividad, un aviso amable como mucho cada 3 días (desactivable). Nunca menciona rachas perdidas.
