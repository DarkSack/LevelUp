package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyReviewTest {
    private val today = MONDAY.plus(7, DateTimeUnit.DAY) // la semana repasada es la de MONDAY
    private val prefs = Preferences(dailyCount = 3, onboardingDone = true)

    private fun day(n: Int) = MONDAY.plus(n, DateTimeUnit.DAY)

    @Test fun isDueOnlyWithActivityLastWeekAndOncePerWeek() {
        val history = listOf(inst("a", day(2)))
        assertTrue(WeeklyReview.isDue(today, null, history))
        assertFalse(WeeklyReview.isDue(today, today, history))
        assertTrue(WeeklyReview.isDue(today, MONDAY, history))
        assertFalse(WeeklyReview.isDue(today, null, emptyList()))
        assertFalse(WeeklyReview.isDue(today, null, listOf(inst("a", today))))
    }

    @Test fun manySkippedProposesFewerPerDay() {
        val history = (0 until 3).map { inst("d$it", day(it)) } +
            (0 until 2).map { inst("s$it", day(it), InstanceStatus.SKIPPED) }
        val s = WeeklyReview.compute(today, prefs, history, emptyList(), emptyList(), emptyList())
        assertEquals(5, s.planned)
        assertEquals(3, s.completed)
        assertEquals(2, s.skipped)
        assertEquals(listOf<ReviewSuggestion>(ReviewSuggestion.FewerPerDay(2)), s.suggestions)
    }

    @Test fun allDoneAndEasyProposesMorePerDay() {
        val history = (0 until 5).map { inst("d$it", day(it), rating = if (it < 3) Rating.EASY else Rating.RIGHT) }
        val s = WeeklyReview.compute(today, prefs, history, emptyList(), emptyList(), emptyList())
        assertEquals(listOf<ReviewSuggestion>(ReviewSuggestion.MorePerDay(4)), s.suggestions)
        assertEquals(5, s.activeDays)
    }

    @Test fun fewSamplesProposeNothing() {
        val history = (0 until 4).map { inst("s$it", day(it), InstanceStatus.SKIPPED) }
        assertTrue(WeeklyReview.compute(today, prefs, history, emptyList(), emptyList(), emptyList()).suggestions.isEmpty())
    }

    @Test fun summaryCountsOnlyLastWeek() {
        val history = listOf(inst("a", day(1)), inst("b", day(-1)), inst("c", today))
        val xp = listOf(day(1) to 30, day(-1) to 50, today to 10)
        val unlocked = listOf("first_1" to day(1), "old" to day(-3))
        val s = WeeklyReview.compute(today, prefs, history, xp, unlocked, emptyList())
        assertEquals(MONDAY, s.weekStart)
        assertEquals(1, s.completed)
        assertEquals(30, s.xp)
        assertEquals(listOf("first_1"), s.achievements)
        assertEquals("fisico", s.topCategory)
    }

    @Test fun reminderMovesTowardsUsualTimeOnlyWhenRemindersAreOn() {
        val minutes = List(6) { 13 * 60 }
        val on = prefs.copy(remindersEnabled = true)
        assertEquals(12 * 60 + 30, WeeklyReview.reminderTime(minutes, on))
        assertNull(WeeklyReview.reminderTime(minutes, on.copy(reminderMinute = 13 * 60)), "menos de dos horas de diferencia")
        assertNull(WeeklyReview.reminderTime(List(6) { 23 * 60 + 30 }, on), "cae en las horas de silencio")
        assertNull(WeeklyReview.reminderTime(minutes.take(4), on), "pocas muestras")

        val off = WeeklyReview.compute(today, prefs, emptyList(), emptyList(), emptyList(), minutes)
        assertTrue(off.suggestions.isEmpty())
    }

    @Test fun reminderDaysFollowTheUsualWeekdays() {
        val history = (1..4).flatMap { w -> listOf(today.minus(w * 7, DateTimeUnit.DAY), today.minus(w * 7 - 2, DateTimeUnit.DAY)) }
            .map { inst("x$it", it) }
        val on = prefs.copy(remindersEnabled = true)
        assertEquals(Days.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), WeeklyReview.reminderDays(today, history, on))
        assertNull(WeeklyReview.reminderDays(today, history, on.copy(reminderDays = Days.of(DayOfWeek.MONDAY))))
    }
}
