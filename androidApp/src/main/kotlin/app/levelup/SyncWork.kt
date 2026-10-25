package app.levelup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.levelup.cloud.CloudService
import app.levelup.domain.Reminders
import app.levelup.ui.social.noticeText
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/**
 * Sincronización en segundo plano: poco después de cada cambio (con red y reintentos) y cada 6 horas mientras
 * haya sesión, para traer lo de otros teléfonos y los avisos de amigos.
 */
object SyncWork {
    private const val SOON = "sync-soon"
    private const val PERIODIC = "sync-periodic"
    private const val CHANNEL = "social"
    private const val NOTIFICATION_ID = 2
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.channel_social), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_social_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun soon(context: Context) {
        val request = OneTimeWorkRequestBuilder<Worker>()
            .setConstraints(online)
            .setInitialDelay(5, TimeUnit.SECONDS) // junta varios toques seguidos en una sola subida
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.KEEP, request)
    }

    fun periodic(context: Context, signedIn: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!signedIn) {
            work.cancelUniqueWork(PERIODIC)
            work.cancelUniqueWork(SOON)
            return
        }
        val request = PeriodicWorkRequestBuilder<Worker>(6, TimeUnit.HOURS).setConstraints(online).build()
        work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as LevelUpApplication
            val service = app.cloud ?: return Result.success()
            if (service.cloud.currentUserId() == null) return Result.success()
            service.reconcile()
            val synced = service.syncNow().isSuccess
            runCatching { notifySocial(app, service) }
            // Lo que se cambió mientras subía también tiene que llegar.
            return if (!synced || service.hasPending()) Result.retry() else Result.success()
        }
    }

    /** Un aviso con lo nuevo desde el último. En horas de silencio se espera a la siguiente vez. */
    private suspend fun notifySocial(app: LevelUpApplication, service: CloudService) {
        val prefs = app.repo.prefs()
        if (!prefs.socialNotifications || !NotificationManagerCompat.from(app).areNotificationsEnabled()) return
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        if (Reminders.inQuietHours(now, prefs.quietStartMinute, prefs.quietEndMinute)) return

        val seen = app.getSharedPreferences("levelup_social", Context.MODE_PRIVATE)
        val last = seen.getLong("last", 0)
        fun millis(iso: String) = OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        val fresh = service.cloud.notices().filter { !it.read && millis(it.createdAt) > last }
        if (fresh.isEmpty()) return
        seen.edit().putLong("last", fresh.maxOf { millis(it.createdAt) }).apply()

        val lines = fresh.map(::noticeText)
        val open = PendingIntent.getActivity(
            app, 1,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (lines.size == 1) "LevelUp" else "${lines.size} avisos de tus amigos")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.InboxStyle().also { style -> lines.take(5).forEach { style.addLine(it) } })
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permiso retirado justo ahora: nada que hacer.
        }
    }
}
