package app.levelup.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreakRepairTest {
    private val today = LocalDate(2026, 11, 10)
    private val active = (1..7).map { LocalDate(2026, 11, it) }.toSet() // 1 al 7; faltaron el 8 y el 9

    @Test fun missedDaysCanBeCoveredAndTheStreakComesBack() {
        val missed = StreakRepair.missed(today, active, 0, emptyList(), emptySet())
        assertEquals(listOf(LocalDate(2026, 11, 8), LocalDate(2026, 11, 9)), missed)
        assertEquals(0, Streaks.current(today, active, 0, emptyList()))
        assertEquals(7, Streaks.current(today, active, 0, emptyList(), missed.toSet()))
        assertTrue(StreakRepair.missed(today, active, 0, emptyList(), missed.toSet()).isEmpty())
    }

    @Test fun fourPerMonth() {
        val used = StreakRepair.parse("2026-10-30,2026-11-02,2026-11-05,basura")
        assertEquals(StreakRepair.PER_MONTH - 2, StreakRepair.left(today, used))
        assertEquals("2026-10-30,2026-11-02,2026-11-05", StreakRepair.format(used))
    }

    @Test fun nothingToRepairWithoutAPreviousStreak() {
        assertTrue(StreakRepair.missed(today, emptySet(), 0, emptyList(), emptySet()).isEmpty())
    }
}
