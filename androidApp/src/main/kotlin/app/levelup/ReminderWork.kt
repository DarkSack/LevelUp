package app.levelup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.levelup.domain.Preferences
import app.levelup.domain.Reminders
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import app.levelup.i18n.tr

/**
 * Un aviso al día a la hora elegida. Cada ejecución programa la siguiente; qué dice (o si calla) lo decide
 * el dominio (Reminders.decide), que respeta horas de silencio, descansos y pausas.
 */
object ReminderWork {
    private const val CHANNEL = "reminders"
    private const val WORK = "daily-reminder"
    private const val NOTIFICATION_ID = 1
    private const val HABIT_WORK = "habit-reminders"
    private const val HABIT_NOTIFICATION_ID = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_reminders_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, prefs: Preferences, now: LocalDateTime = LocalDateTime.now()) {
        val work = WorkManager.getInstance(context)
        if (!prefs.remindersEnabled || !prefs.onboardingDone) {
            work.cancelUniqueWork(WORK)
            return
        }
        val request = OneTimeWorkRequestBuilder<Worker>()
            .setInitialDelay(delayUntil(prefs.reminderMinute, now).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Avisos propios de los hábitos: un trabajo para la próxima hora con alguno; cada ejecución programa el siguiente. */
    suspend fun scheduleHabits(app: LevelUpApplication, now: LocalDateTime = LocalDateTime.now()) {
        val work = WorkManager.getInstance(app)
        val next = Reminders.nextHabitMinute(app.repo.habitReminderMinutes(), now.hour * 60 + now.minute)
        if (next == null) {
            work.cancelUniqueWork(HABIT_WORK)
            return
        }
        val request = OneTimeWorkRequestBuilder<HabitWorker>()
            .setInitialDelay(delayUntil(next, now).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniqueWork(HABIT_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Hasta la próxima vez que el reloj marque [minuteOfDay]: hoy si aún no pasó, si no mañana. */
    fun delayUntil(minuteOfDay: Int, now: LocalDateTime): Duration {
        var next = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    private fun notify(app: LevelUpApplication, id: Int, title: String, body: String) {
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            app, 0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(id, notification)
        } catch (_: SecurityException) {
            // El permiso se retiró justo ahora: no hay nada que hacer hasta que lo devuelvan.
        }
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as LevelUpApplication
            app.repo.reminderNow()?.let { notify(app, NOTIFICATION_ID, tr(it.title), tr(it.body)) }
            schedule(app, app.repo.prefs())
            return Result.success()
        }
    }

    class HabitWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as LevelUpApplication
            val due = app.repo.dueHabitReminders()
            if (due.isNotEmpty()) notify(app, HABIT_NOTIFICATION_ID, app.getString(R.string.habit_reminder_title), due.joinToString(" · ") { tr(it) })
            scheduleHabits(app)
            return Result.success()
        }
    }
}
