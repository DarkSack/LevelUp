package app.levelup.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Complicación de la esfera: retos hechos hoy («1/3»). Tocarla abre LevelUp en el reloj. */
class ProgressComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 1, 3)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val today = runCatching { WatchData.load(this) }.getOrNull()?.takeUnless { it.stale }
        return build(request.complicationType, today?.done ?: 0, today?.total ?: 0)
    }

    private fun build(type: ComplicationType, done: Int, total: Int): ComplicationData? {
        val text = PlainComplicationText.Builder(if (total == 0) "–" else "$done/$total").build()
        val description = PlainComplicationText.Builder(
            if (total == 0) "Sin retos de hoy en el reloj" else "$done de $total retos hechos hoy",
        ).build()
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, WatchActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, description)
                .setTitle(PlainComplicationText.Builder("Retos").build())
                .setTapAction(tap).build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(done.toFloat(), 0f, maxOf(total, 1).toFloat(), description)
                .setText(text)
                .setTapAction(tap).build()
            else -> null
        }
    }
}

/** El teléfono mandó datos nuevos: repintar la complicación. */
class WatchListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        ComplicationDataSourceUpdateRequester.create(this, ComponentName(this, ProgressComplicationService::class.java)).requestUpdateAll()
    }
}
