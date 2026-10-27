package app.levelup.domain

import app.levelup.MONDAY
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemindersAndRulesTest {
    private val on = Preferences(remindersEnabled = true, notificationLevel = NotificationLevel.LOW)
    private val evening = LocalDateTime(MONDAY, LocalTime(19, 0))

    @Test fun offWhenDisabled() {
        assertNull(Reminders.decide(evening, on.copy(remindersEnabled = false), DayKind.ACTIVE, listOf("Leer"), MONDAY))
        assertNull(Reminders.decide(evening, on.copy(notificationLevel = NotificationLevel.OFF), DayKind.ACTIVE, listOf("Leer"), MONDAY))
    }

    @Test fun quietHoursAcrossMidnight() {
        assertTrue(Reminders.inQuietHours(23 * 60, 22 * 60, 8 * 60))
        assertTrue(Reminders.inQuietHours(7 * 60, 22 * 60, 8 * 60))
        assertTrue(!Reminders.inQuietHours(12 * 60, 22 * 60, 8 * 60))
        assertTrue(Reminders.inQuietHours(14 * 60, 13 * 60, 15 * 60))
        assertTrue(!Reminders.inQuietHours(14 * 60, 9 * 60, 9 * 60))
        val late = LocalDateTime(MONDAY, LocalTime(23, 30))
        assertNull(Reminders.decide(late, on, DayKind.ACTIVE, listOf("Leer"), MONDAY))
    }

    @Test fun silentOnRestAndPause() {
        assertNull(Reminders.decide(evening, on, DayKind.REST, listOf("Leer"), MONDAY))
        assertNull(Reminders.decide(evening, on, DayKind.PAUSED, listOf("Leer"), MONDAY))
    }

    @Test fun habitRemindersFireInTheirHalfHourAndRespectSilence() {
        val pending = listOf("Leer" to 19 * 60, "Estirar" to 18 * 60, "Agua" to 19 * 60 + 10)
        assertEquals(listOf("Leer"), Reminders.dueHabits(LocalDateTime(MONDAY, LocalTime(19, 5)), on, DayKind.ACTIVE, pending))
        assertEquals(listOf("Leer", "Agua"), Reminders.dueHabits(LocalDateTime(MONDAY, LocalTime(19, 20)), on, DayKind.ACTIVE, pending))
        assertEquals(emptyList(), Reminders.dueHabits(evening, on, DayKind.REST, pending))
        assertEquals(emptyList(), Reminders.dueHabits(evening, on.copy(quietStartMinute = 18 * 60, quietEndMinute = 20 * 60), DayKind.ACTIVE, pending))
        // No depende del aviso diario: el hábito lo pidió aparte.
        assertEquals(listOf("Leer"), Reminders.dueHabits(evening, on.copy(remindersEnabled = false), DayKind.ACTIVE, pending))
    }

    @Test fun nextHabitMinuteWrapsToTomorrow() {
        assertEquals(19 * 60, Reminders.nextHabitMinute(listOf(9 * 60, 19 * 60), 12 * 60))
        assertEquals(9 * 60, Reminders.nextHabitMinute(listOf(9 * 60, 19 * 60), 19 * 60))
        assertEquals(9 * 60, Reminders.nextHabitMinute(listOf(9 * 60), 9 * 60))
        assertNull(Reminders.nextHabitMinute(emptyList(), 0))
    }

    @Test fun nothingPendingNothingToSay() {
        assertNull(Reminders.decide(evening, on, DayKind.ACTIVE, emptyList(), MONDAY))
    }

    @Test fun lowShowsOneNormalShowsUpToThree() {
        val titles = listOf("Leer", "Caminar", "Respirar", "Ordenar")
        assertEquals("Leer", Reminders.decide(evening, on, DayKind.ACTIVE, titles, MONDAY)?.body)
        val normal = Reminders.decide(evening, on.copy(notificationLevel = NotificationLevel.NORMAL), DayKind.ACTIVE, titles, MONDAY)
        assertEquals("4 retos para hoy", normal?.title)
        assertEquals("Leer · Caminar · Respirar", normal?.body)
    }

    @Test fun recoveryAfterThreeQuietDaysAtMostEveryThreeDays() {
        val lastActive = MONDAY.minus(3, DateTimeUnit.DAY)
        val msg = Reminders.decide(evening, on, DayKind.ACTIVE, listOf("Leer"), lastActive)
        assertTrue(msg!!.isRecovery)
        assertTrue("racha" !in msg.body.lowercase() && "perd" !in msg.body.lowercase())
        val recentlyNoticed = on.copy(lastRecoveryNotice = MONDAY.minus(1, DateTimeUnit.DAY))
        assertEquals(false, Reminders.decide(evening, recentlyNoticed, DayKind.ACTIVE, listOf("Leer"), lastActive)?.isRecovery)
        assertNull(Reminders.decide(evening, on.copy(recoveryReminders = false), DayKind.ACTIVE, emptyList(), lastActive))
    }

    // ── Retos propios ──────────────────────────────────────────────────────────

    private val valid = CustomChallengeDraft(title = "Leer en el bus", category = "mental", goal = "Leer 10 páginas", minutes = 15)

    @Test fun validDraftHasNoErrors() {
        assertTrue(CustomChallengeRules.validate(valid, setOf("mental"), emptyList()).isEmpty())
    }

    @Test fun eachFieldIsChecked() {
        val bad = valid.copy(
            title = "a", category = "nada", goal = "", difficulty = 9, minutes = 0, steps = List(11) { "paso" },
            description = "x".repeat(301), doneWhen = "y".repeat(141), minimum = "z".repeat(CustomChallengeRules.GOAL_MAX + 1),
        )
        val errors = CustomChallengeRules.validate(bad, setOf("mental"), emptyList())
        assertEquals(DraftField.entries.toSet(), errors.keys)
    }

    @Test fun duplicateTitlesIgnoreCaseAndSpaces() {
        val errors = CustomChallengeRules.validate(valid.copy(title = "  leer   EN el bus "), setOf("mental"), listOf("Leer en el bus"))
        assertEquals(setOf(DraftField.TITLE), errors.keys)
    }
}
