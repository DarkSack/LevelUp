package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtendedStatsTest {
    @Test fun summarizesCompletedChallenges() {
        val tuesday = MONDAY.plus(1, DateTimeUnit.DAY)
        val done = listOf(
            inst("a", MONDAY), inst("b", MONDAY, category = "mental"), inst("c", MONDAY, simplified = true),
            inst("d", tuesday, category = "mental"),
        )
        val lastMonth = MONDAY.minus(1, DateTimeUnit.MONTH)
        val s = ExtendedStats.compute(MONDAY, done, listOf(MONDAY to 40, lastMonth to 25, MONDAY.minus(2, DateTimeUnit.YEAR) to 999))

        assertEquals(4, s.completed)
        assertEquals(20, s.minutes)
        assertEquals(2, s.activeDays)
        assertEquals(20, s.perActiveDayTenths, "2,0 retos por día activo")
        assertEquals(DayOfWeek.MONDAY, s.bestWeekday)
        assertEquals(listOf("fisico" to 2, "mental" to 2), s.byCategory.sortedBy { it.first })
        assertEquals(1, s.simplified)
        assertEquals(12, s.xpByMonth.size)
        assertEquals(YearMonth(2026, 10) to 40, s.xpByMonth.last())
        assertEquals(25, s.xpByMonth[10].second)
        assertEquals(65, s.xpByMonth.sumOf { it.second }, "lo de hace más de un año no entra")
    }

    @Test fun emptyHistory() {
        val s = ExtendedStats.compute(MONDAY, emptyList(), emptyList())
        assertEquals(0, s.perActiveDayTenths)
        assertEquals(null, s.bestWeekday)
    }
}
