package app.levelup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.levelup.data.CompletionResult
import app.levelup.domain.GamificationStyle
import app.levelup.i18n.tr

/**
 * Logro conseguido sin la app abierta (botón del widget o el reloj). Con la app abierta lo celebra la propia
 * pantalla; con «Sin juego» no hay logros que anunciar.
 */
object AchievementNotice {
    private const val CHANNEL = "achievements"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.channel_achievements), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_achievements_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun show(app: LevelUpApplication, result: CompletionResult?) {
        val got = result?.achievements.orEmpty()
        if (got.isEmpty() || app.repo.prefs().gamification == GamificationStyle.OFF) return
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            app, 3,
            Intent(Intent.ACTION_VIEW, Uri.parse("levelup://logros"), app, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        got.forEach { a ->
            val notification = NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(app.getString(R.string.achievement_unlocked))
                .setContentText(tr(a.name))
                .setStyle(NotificationCompat.BigTextStyle().bigText(tr(a.name) + "\n" + tr(a.description)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(app).notify(a.id, 0, notification)
            } catch (_: SecurityException) {
                // El permiso se retiró justo ahora.
            }
        }
    }
}
