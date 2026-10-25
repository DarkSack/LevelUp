package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Un cambio que la revisión semanal propone. Nunca se aplica solo: la persona pulsa «Aplicar». */
sealed interface ReviewSuggestion {
    data class FewerPerDay(val to: Int) : ReviewSuggestion
    data class MorePerDay(val to: Int) : ReviewSuggestion
    data class MoveReminder(val minuteOfDay: Int) : ReviewSuggestion
    data class ReminderDays(val mask: Int) : ReviewSuggestion
}

data class WeeklySummary(
    /** Lunes de la semana repasada (la anterior a la actual). */
    val weekStart: LocalDate,
    val completed: Int,
    val planned: Int,
    val skipped: Int,
    val activeDays: Int,
    val xp: Int,
    val achievements: List<String>,
    val topCategory: String?,
    val suggestions: List<ReviewSuggestion>,
)

/**
 * Revisión de cada semana: qué salió, qué logros llegaron y qué ajustar. Las propuestas salen de datos
 * propios (lo omitido, lo valorado como fácil o difícil, a qué hora se completan los retos).
 */
object WeeklyReview {
    const val MIN_SAMPLES = 5

    /** La revisión toca si la semana pasada tuvo algo de actividad y aún no se repasó esta semana. */
    fun isDue(today: LocalDate, lastReview: LocalDate?, history: List<ChallengeInstance>): Boolean {
        val thisWeek = Streaks.weekStart(today)
        if (lastReview != null && lastReview >= thisWeek) return false
        val lastWeek = thisWeek.minus(7, DateTimeUnit.DAY)
        return history.any { it.date >= lastWeek && it.date < thisWeek && it.status != InstanceStatus.REPLACED }
    }

    fun compute(
        today: LocalDate,
        prefs: Preferences,
        /** Instancias de las últimas 4 semanas como mínimo. */
        history: List<ChallengeInstance>,
        /** XP ganada por día. */
        xpByDay: List<Pair<LocalDate, Int>>,
        /** Logros desbloqueados con su día. */
        unlocked: List<Pair<String, LocalDate>>,
        /** Minuto del día (hora local) en que se completó cada reto de las últimas 4 semanas. */
        completionMinutes: List<Int>,
    ): WeeklySummary {
        val start = Streaks.weekStart(today).minus(7, DateTimeUnit.DAY)
        val end = start.plus(6, DateTimeUnit.DAY)
        val week = history.filter { it.date in start..end && it.status != InstanceStatus.REPLACED }
        val done = week.filter { it.isDone }
        val skipped = week.count { it.status == InstanceStatus.SKIPPED || (it.isOpen && it.date < today) }
        val suggestions = buildList {
            val planned = week.size
            val easy = done.count { it.rating == Rating.EASY }
            val hard = done.count { it.rating == Rating.HARD }
            if (planned >= MIN_SAMPLES && skipped * 10 >= planned * 4 && prefs.dailyCount > 1) {
                add(ReviewSuggestion.FewerPerDay(prefs.dailyCount - 1))
            } else if (planned >= MIN_SAMPLES && done.size * 10 >= planned * 9 && easy >= 3 && easy > hard && prefs.dailyCount < 5) {
                add(ReviewSuggestion.MorePerDay(prefs.dailyCount + 1))
            }
            if (prefs.remindersEnabled) {
                reminderTime(completionMinutes, prefs)?.let { add(ReviewSuggestion.MoveReminder(it)) }
                reminderDays(today, history, prefs)?.let { add(ReviewSuggestion.ReminderDays(it)) }
            }
        }
        return WeeklySummary(
            weekStart = start,
            completed = done.size,
            planned = week.size,
            skipped = skipped,
            activeDays = done.map { it.date }.distinct().size,
            xp = xpByDay.filter { it.first in start..end }.sumOf { it.second },
            achievements = unlocked.filter { it.second in start..end }.map { it.first },
            topCategory = done.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key,
            suggestions = suggestions,
        )
    }

    /**
     * Si los retos se suelen completar a otra hora, propone avisar media hora antes de esa costumbre
     * (mediana, redondeada a la media hora). Nada si la diferencia es menor de dos horas o cae en silencio.
     */
    fun reminderTime(completionMinutes: List<Int>, prefs: Preferences): Int? {
        if (completionMinutes.size < MIN_SAMPLES) return null
        val sorted = completionMinutes.sorted()
        val median = sorted[sorted.size / 2]
        val proposal = ((median - 30).coerceAtLeast(0) / 30) * 30
        if (kotlin.math.abs(proposal - prefs.reminderMinute) < 120) return null
        if (Reminders.inQuietHours(proposal, prefs.quietStartMinute, prefs.quietEndMinute)) return null
        return proposal
    }

    /** Si avisa todos los días pero la actividad se concentra en unos pocos, propone avisar solo esos días. */
    fun reminderDays(today: LocalDate, history: List<ChallengeInstance>, prefs: Preferences): Int? {
        if (prefs.reminderDays != Days.ALL) return null
        val from = today.minus(28, DateTimeUnit.DAY)
        val activeDates = history.filter { it.isDone && it.date >= from && it.date < today }.map { it.date }.distinct()
        if (activeDates.size < MIN_SAMPLES) return null
        val usual: List<DayOfWeek> = activeDates.groupingBy { it.dayOfWeek }.eachCount().filterValues { it >= 2 }.keys.toList()
        if (usual.size !in 2..5) return null
        return Days.of(*usual.toTypedArray())
    }
}
