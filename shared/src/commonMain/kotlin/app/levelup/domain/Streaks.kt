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

    /** Días que no suman ni cortan: descanso, pausa o un día perdido que se restableció. */
    fun isNeutral(day: LocalDate, restDays: Int, pauses: List<Pause>, repaired: Set<LocalDate> = emptySet()): Boolean =
        day in repaired || Days.has(restDays, day.dayOfWeek) || pauses.any { it.covers(day) }

    /** Días activos seguidos hasta hoy. Hoy sin actividad todavía no rompe nada: el día no ha terminado. */
    fun current(today: LocalDate, active: Set<LocalDate>, restDays: Int, pauses: List<Pause>, repaired: Set<LocalDate> = emptySet()): Int {
        val first = active.minOrNull() ?: return 0
        var day = if (today in active) today else today.minus(1, DateTimeUnit.DAY)
        var count = 0
        while (day >= first) {
            when {
                day in active -> count++
                isNeutral(day, restDays, pauses, repaired) -> Unit
                else -> return count
            }
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }

    fun best(today: LocalDate, active: Set<LocalDate>, restDays: Int, pauses: List<Pause>, repaired: Set<LocalDate> = emptySet()): Int {
        var day = active.minOrNull() ?: return 0
        var run = 0
        var best = 0
        while (day <= today) {
            when {
                day in active -> { run++; best = maxOf(best, run) }
                isNeutral(day, restDays, pauses, repaired) || day == today -> Unit
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

/**
 * Restablecer la racha: cada día perdido que la cortó se puede cubrir gastando un restablecimiento, hasta
 * [PER_MONTH] al mes (cuenta el mes del día cubierto). Los días cubiertos quedan como neutros, igual que un descanso.
 */
object StreakRepair {
    const val PER_MONTH = 4

    fun parse(raw: String): Set<LocalDate> = raw.split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()

    fun format(days: Set<LocalDate>): String = days.sorted().joinToString(",")

    fun left(today: LocalDate, repaired: Set<LocalDate>): Int =
        PER_MONTH - repaired.count { it.year == today.year && it.month == today.month }

    /** Los días perdidos entre el último día activo y ayer, si hoy no hay racha. Vacío si no hay nada que salvar. */
    fun missed(today: LocalDate, active: Set<LocalDate>, restDays: Int, pauses: List<Pause>, repaired: Set<LocalDate>): List<LocalDate> {
        if (Streaks.current(today, active, restDays, pauses, repaired) > 0) return emptyList()
        val last = active.filter { it < today }.maxOrNull() ?: return emptyList()
        return generateSequence(last.plus(1, DateTimeUnit.DAY)) { it.plus(1, DateTimeUnit.DAY) }
            .takeWhile { it < today }
            .filterNot { Streaks.isNeutral(it, restDays, pauses, repaired) }
            .toList()
    }
}
