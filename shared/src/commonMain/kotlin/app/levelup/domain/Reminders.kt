package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import app.levelup.i18n.tr

data class ReminderMessage(val title: String, val body: String, val isRecovery: Boolean = false)

/** Decide si un recordatorio se muestra y con qué texto. Sin culpa: nunca habla de rachas perdidas. */
object Reminders {
    const val RECOVERY_AFTER_DAYS = 3

    fun inQuietHours(minuteOfDay: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minuteOfDay in start until end
        else -> minuteOfDay >= start || minuteOfDay < end
    }

    /** Margen de un aviso de hábito: WorkManager puede retrasarlo unos minutos. */
    const val HABIT_WINDOW_MINUTES = 30

    /** Títulos de los hábitos de hoy sin hacer (título y minuto de su aviso) cuyo aviso cayó en la última media hora. */
    fun dueHabits(now: LocalDateTime, prefs: Preferences, dayKind: DayKind, pending: List<Pair<String, Int>>): List<String> {
        val minute = now.hour * 60 + now.minute
        if (prefs.notificationLevel == NotificationLevel.OFF || inQuietHours(minute, prefs.quietStartMinute, prefs.quietEndMinute)) return emptyList()
        if (dayKind == DayKind.PAUSED || dayKind == DayKind.REST) return emptyList()
        return pending.filter { (_, at) -> at <= minute && minute - at < HABIT_WINDOW_MINUTES }.map { it.first }
    }

    /** El primer minuto de [minutes] que llega después de [minute] (al día siguiente si ya pasaron todos); null si no hay. */
    fun nextHabitMinute(minutes: Collection<Int>, minute: Int): Int? = minutes.minByOrNull { (it - minute - 1 + 24 * 60) % (24 * 60) }

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
        val title = if (pendingTitles.size == 1) "Un reto para hoy" else tr("{0} retos para hoy", pendingTitles.size)
        val body = if (prefs.notificationLevel == NotificationLevel.NORMAL) {
            pendingTitles.take(3).joinToString(" · ") { tr(it) }
        } else {
            pendingTitles.first()
        }
        return ReminderMessage(title, body)
    }
}
