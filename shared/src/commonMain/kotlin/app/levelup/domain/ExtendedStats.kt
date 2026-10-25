package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.yearMonth

/** Estadísticas ampliadas de LevelUp Plus. Todo se calcula desde lo guardado; no se guarda nada aparte. */
data class ExtendedStats(
    val completed: Int = 0,
    val minutes: Int = 0,
    val activeDays: Int = 0,
    /** Media de retos por día con actividad (×10 para no usar decimales en la interfaz: 15 = 1,5). */
    val perActiveDayTenths: Int = 0,
    val bestWeekday: DayOfWeek? = null,
    val byCategory: List<Pair<String, Int>> = emptyList(),
    /** XP de los últimos 12 meses, del más antiguo al actual. */
    val xpByMonth: List<Pair<YearMonth, Int>> = emptyList(),
    val simplified: Int = 0,
) {
    companion object {
        fun compute(today: LocalDate, completed: List<ChallengeInstance>, xp: List<Pair<LocalDate, Int>>): ExtendedStats {
            val days = completed.groupBy { it.date }
            val thisMonth = today.yearMonth
            val months = (11 downTo 0).map { thisMonth.minus(it, DateTimeUnit.MONTH) }
            val xpPerMonth = xp.groupBy { it.first.yearMonth }.mapValues { (_, l) -> l.sumOf { it.second } }
            return ExtendedStats(
                completed = completed.size,
                minutes = completed.sumOf { it.minutes },
                activeDays = days.size,
                perActiveDayTenths = if (days.isEmpty()) 0 else completed.size * 10 / days.size,
                bestWeekday = completed.groupingBy { it.date.dayOfWeek }.eachCount().maxByOrNull { it.value }?.key,
                byCategory = completed.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second },
                xpByMonth = months.map { it to (xpPerMonth[it] ?: 0) },
                simplified = completed.count { it.simplified },
            )
        }
    }
}
