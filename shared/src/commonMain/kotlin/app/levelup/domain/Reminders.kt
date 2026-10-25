package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

data class ReminderMessage(val title: String, val body: String, val isRecovery: Boolean = false)

/** Decide si un recordatorio se muestra y con qué texto. Sin culpa: nunca habla de rachas perdidas. */
object Reminders {
    const val RECOVERY_AFTER_DAYS = 3

    fun inQuietHours(minuteOfDay: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minuteOfDay in start until end
        else -> minuteOfDay >= start || minuteOfDay < end
    }

    fun decide(
        now: LocalDateTime,
        prefs: Preferences,
        dayKind: DayKind,
        pendingTitles: List<String>,
        lastActive: kotlinx.datetime.LocalDate?,
    ): ReminderMessage? {
        if (!prefs.remindersEnabled || prefs.notificationLevel == NotificationLevel.OFF) return null
        if (inQuietHours(now.hour * 60 + now.minute, prefs.quietStartMinute, prefs.quietEndMinute)) return null
        if (dayKind == DayKind.PAUSED || dayKind == DayKind.REST) return null
        if (!Days.has(prefs.reminderDays, now.date.dayOfWeek)) return null

        val today = now.date
        val limit = today.minus(RECOVERY_AFTER_DAYS, DateTimeUnit.DAY)
        val lastNotice = prefs.lastRecoveryNotice
        if (prefs.recoveryReminders && lastActive != null && lastActive <= limit && (lastNotice == null || lastNotice <= limit)) {
            return ReminderMessage(
                title = "Aquí sigue tu progreso",
                body = "Cuando te venga bien, hay un reto corto esperándote. Sin prisa.",
                isRecovery = true,
            )
        }
        if (pendingTitles.isEmpty()) return null
        val title = if (pendingTitles.size == 1) "Un reto para hoy" else "${pendingTitles.size} retos para hoy"
        val body = if (prefs.notificationLevel == NotificationLevel.NORMAL) {
            pendingTitles.take(3).joinToString(" · ")
        } else {
            pendingTitles.first()
        }
        return ReminderMessage(title, body)
    }
}
