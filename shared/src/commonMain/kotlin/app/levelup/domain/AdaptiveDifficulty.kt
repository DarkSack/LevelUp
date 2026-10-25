package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Sugiere subir o bajar un paso la dificultad de una categoría. Prudente a propósito:
 * hacen falta varios retos resueltos, un reto omitido nunca basta y la inactividad no cuenta como fallo.
 */
object AdaptiveDifficulty {
    const val WINDOW_DAYS = 14
    const val MIN_RESOLVED = 5
    const val COOLDOWN_DAYS = 7

    data class Suggestion(val category: String, val from: Int, val to: Int) {
        val isUp: Boolean get() = to > from
    }

    fun suggest(category: String, prefs: Preferences, history: List<ChallengeInstance>, today: LocalDate): Suggestion? {
        if (prefs.difficultyMode == DifficultyMode.MANUAL) return null
        prefs.lastDifficultyChange?.let { if (today < it.plus(COOLDOWN_DAYS, DateTimeUnit.DAY)) return null }

        val from = today.minus(WINDOW_DAYS, DateTimeUnit.DAY)
        val window = history.filter { it.category == category && !it.isHabit && it.date in from..today }
        val completed = window.count { it.isDone }
        // Retos sin tocar no cuentan: solo lo que el usuario resolvió (hecho, omitido o cambiado).
        val dropped = window.count { it.status == InstanceStatus.SKIPPED || it.status == InstanceStatus.REPLACED }
        val resolved = completed + dropped
        if (resolved < MIN_RESOLVED) return null

        val rate = completed.toDouble() / resolved
        val easy = window.count { it.rating == Rating.EASY }
        val hard = window.count { it.rating == Rating.HARD }
        val simplified = window.count { it.simplified }
        val current = prefs.difficultyFor(category)

        val up = rate >= 0.8 && hard == 0 && simplified == 0 && (easy * 2 >= completed || completed >= 10)
        val down = hard >= 3 || (rate < 0.5 && dropped + simplified >= 3)
        return when {
            up && current < 5 -> Suggestion(category, current, current + 1)
            down && current > 1 -> Suggestion(category, current, current - 1)
            else -> null
        }
    }
}
