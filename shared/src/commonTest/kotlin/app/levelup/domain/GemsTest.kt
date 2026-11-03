package app.levelup.domain

import app.levelup.inst
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GemsTest {
    private fun done(date: LocalDate, n: Int, status: InstanceStatus = InstanceStatus.COMPLETED) = List(n) { inst("c$it", date, status) }

    @Test fun oneGemPerChallengeUpToThreeADay() {
        val list = done(LocalDate(2026, 11, 1), 5) + done(LocalDate(2026, 11, 2), 2) + done(LocalDate(2026, 11, 3), 4, InstanceStatus.SKIPPED)
        assertEquals(3 + 2, Gems.earned(list))
    }

    @Test fun eachPetCostsMoreThanTheLast() {
        assertEquals(listOf(100, 175, 275, 400, 550, 725, 925), (0..6).map(Gems::price))
        assertEquals(0, Gems.spent(0))
        assertEquals(100 + 175 + 275, Gems.spent(3))
    }
}
