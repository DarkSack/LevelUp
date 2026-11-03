package app.levelup

import androidx.compose.ui.graphics.asAndroidBitmap
import app.levelup.pets.renderPet
import app.levelup.content.Pets
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import app.levelup.domain.TodaySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.levelup.i18n.tr

/**
 * Widget de Inicio: el reto principal, cuántos van y un botón para completarlo.
 * La app lo repinta cada vez que cambian los retos de hoy; el sistema, cada media hora (cambio de día).
 */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async(context) { app ->
        app.repo.ensureToday()
        render(context, app.repo.todaySummary.first(), app.repo.petSnapshot())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_COMPLETE) return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        async(context) { app ->
            AchievementNotice.show(app, app.repo.complete(id))
            render(context, app.repo.todaySummary.first(), app.repo.petSnapshot())
        }
    }

    private fun async(context: Context, block: suspend (LevelUpApplication) -> Unit) {
        val pending = goAsync()
        val app = context.applicationContext as LevelUpApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_COMPLETE = "app.levelup.widget.COMPLETAR"
        private const val EXTRA_ID = "id"

        fun render(context: Context, summary: TodaySummary, pet: Pets.Snapshot?) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodayWidget::class.java)
            if (manager.getAppWidgetIds(component).isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            if (pet != null) {
                val px = (56 * context.resources.displayMetrics.density).toInt()
                views.setImageViewBitmap(R.id.widget_pet, renderPet(pet.species, pet.mood, px, pet.accessories, pet.stage).asAndroidBitmap())
                views.setContentDescription(R.id.widget_pet, pet.name)
                views.setViewVisibility(R.id.widget_pet, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_pet, View.GONE)
            }
            val main = summary.mainId
            views.setTextViewText(
                R.id.widget_title,
                when {
                    main != null -> summary.mainTitle?.let(::tr)
                    summary.total > 0 -> tr("Retos de hoy terminados")
                    else -> tr("Abre LevelUp para ver tus retos")
                },
            )
            val label = context.getString(R.string.widget_label)
            views.setTextViewText(R.id.widget_progress, if (summary.total > 0) tr("{0} · {1} de {2}", label, summary.done, summary.total) else label)
            views.setOnClickPendingIntent(R.id.widget_root, open(context, if (main != null) "levelup://reto/$main" else "levelup://hoy", 1))
            if (main != null) {
                views.setViewVisibility(R.id.widget_complete, View.VISIBLE)
                val complete = Intent(context, TodayWidget::class.java).setAction(ACTION_COMPLETE).putExtra(EXTRA_ID, main)
                views.setOnClickPendingIntent(
                    R.id.widget_complete,
                    PendingIntent.getBroadcast(context, 2, complete, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                )
            } else {
                views.setViewVisibility(R.id.widget_complete, View.GONE)
            }
            manager.updateAppWidget(component, views)
        }

        private fun open(context: Context, link: String, code: Int): PendingIntent = PendingIntent.getActivity(
            context, code,
            Intent(Intent.ACTION_VIEW, Uri.parse(link), context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
