package app.levelup.domain

import app.levelup.MONDAY
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class StreaksTest {
    private fun d(offset: Int): LocalDate = MONDAY.plus(offset, DateTimeUnit.DAY)

    @Test fun consecutiveDaysCount() {
        val active = setOf(d(0), d(1), d(2))
        assertEquals(3, Streaks.current(d(2), active, 0, emptyList()))
    }

    @Test fun todayWithoutActivityDoesNotBreakTheStreakYet() {
        assertEquals(3, Streaks.current(d(3), setOf(d(0), d(1), d(2)), 0, emptyList()))
    }

    @Test fun aMissedDayBreaksIt() {
        assertEquals(1, Streaks.current(d(4), setOf(d(0), d(1), d(4)), 0, emptyList()))
        assertEquals(0, Streaks.current(d(5), setOf(d(0), d(1)), 0, emptyList()))
    }

    @Test fun restDaysAndPausesAreNeutral() {
        // Lunes, martes activos; miércoles descanso; jueves activo.
        val rest = Days.of(DayOfWeek.WEDNESDAY)
        assertEquals(3, Streaks.current(d(3), setOf(d(0), d(1), d(3)), rest, emptyList()))
        // Pausa de miércoles a viernes, vuelve el sábado.
        val pause = Pause("p", d(2), d(4))
        assertEquals(3, Streaks.current(d(5), setOf(d(0), d(1), d(5)), 0, listOf(pause)))
    }

    @Test fun bestStreakSurvivesALaterBreak() {
        val active = setOf(d(0), d(1), d(2), d(3), d(6))
        assertEquals(4, Streaks.best(d(6), active, 0, emptyList()))
        assertEquals(1, Streaks.current(d(6), active, 0, emptyList()))
    }

    @Test fun emptyHistory() {
        assertEquals(0, Streaks.current(MONDAY, emptySet(), 0, emptyList()))
        assertEquals(0, Streaks.best(MONDAY, emptySet(), 0, emptyList()))
        assertEquals(1, Streaks.weeks(MONDAY, emptySet(), 3).size)
    }

    @Test fun weeklyGoal() {
        val lastWeek = MONDAY.minus(7, DateTimeUnit.DAY)
        val active = setOf(lastWeek, lastWeek.plus(2, DateTimeUnit.DAY), lastWeek.plus(4, DateTimeUnit.DAY), MONDAY)
        val weeks = Streaks.weeks(MONDAY.plus(2, DateTimeUnit.DAY), active, 3)
        assertEquals(2, weeks.size)
        assertEquals(listOf(true, false), weeks.map { it.met })
        assertEquals(1, weeks.last().activeDays)
        assertEquals(MONDAY, Streaks.weekStart(MONDAY.plus(6, DateTimeUnit.DAY)))
    }
}
