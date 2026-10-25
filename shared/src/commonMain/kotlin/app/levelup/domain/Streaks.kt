package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Rachas flexibles: los días de descanso y las pausas ni suman ni rompen. Perder una racha no quita XP
 * ni niveles; además del número seguido se muestra el objetivo semanal, que perdona días sueltos.
 */
object Streaks {

    fun isNeutral(day: LocalDate, restDays: Int, pauses: List<Pause>): Boolean =
        Days.has(restDays, day.dayOfWeek) || pauses.any { it.covers(day) }

    /** Días activos seguidos hasta hoy. Hoy sin actividad todavía no rompe nada: el día no ha terminado. */
    fun current(today: LocalDate, active: Set<LocalDate>, restDays: Int, pauses: List<Pause>): Int {
        val first = active.minOrNull() ?: return 0
        var day = if (today in active) today else today.minus(1, DateTimeUnit.DAY)
        var count = 0
        while (day >= first) {
            when {
                day in active -> count++
                isNeutral(day, restDays, pauses) -> Unit
                else -> return count
            }
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }

    fun best(today: LocalDate, active: Set<LocalDate>, restDays: Int, pauses: List<Pause>): Int {
        var day = active.minOrNull() ?: return 0
        var run = 0
        var best = 0
        while (day <= today) {
            when {
                day in active -> { run++; best = maxOf(best, run) }
                isNeutral(day, restDays, pauses) || day == today -> Unit
                else -> run = 0
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return best
    }

    fun weekStart(day: LocalDate): LocalDate = day.minus(day.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    data class Week(val start: LocalDate, val activeDays: Int, val goal: Int) {
        val met: Boolean get() = activeDays >= goal
    }

    /** Semanas desde la primera actividad hasta esta, la actual incluida. */
    fun weeks(today: LocalDate, active: Set<LocalDate>, goal: Int): List<Week> {
        val first = active.minOrNull() ?: return listOf(Week(weekStart(today), 0, goal))
        val byWeek = active.groupingBy { weekStart(it) }.eachCount()
        val weeks = mutableListOf<Week>()
        var start = weekStart(first)
        val last = weekStart(today)
        while (start <= last) {
            weeks += Week(start, byWeek[start] ?: 0, goal)
            start = start.plus(7, DateTimeUnit.DAY)
        }
        return weeks
    }
}
