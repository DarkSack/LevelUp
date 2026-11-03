package app.levelup

import app.levelup.pets.Accessory
import app.levelup.content.Pets
import android.content.Context
import app.levelup.domain.TodaySummary
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import app.levelup.i18n.tr

/**
 * Teléfono → reloj por el Data Layer de Wear OS (solo entre el teléfono y el reloj emparejados, misma app y firma).
 * Rutas y claves iguales que WatchData en wearApp.
 */
object WearSync {
    const val PATH_TODAY = "/levelup/hoy"
    const val PATH_COMPLETE = "/levelup/completar"
    const val PATH_REFRESH = "/levelup/pedir"

    /** Sin reloj o sin servicios de Google, falla en silencio: el reloj es opcional. */
    suspend fun publish(context: Context, s: TodaySummary, pet: Pets.Snapshot?) {
        val request = PutDataMapRequest.create(PATH_TODAY).apply {
            dataMap.putString("fecha", s.date.toString())
            dataMap.putString("principalId", s.mainId.orEmpty())
            dataMap.putString("principalTitulo", tr(s.mainTitle.orEmpty()))
            dataMap.putInt("hechosHoy", s.done)
            dataMap.putInt("total", s.total)
            dataMap.putStringArrayList("ids", ArrayList(s.items.map { it.id }))
            dataMap.putStringArrayList("titulos", ArrayList(s.items.map { tr(it.title) }))
            dataMap.putIntegerArrayList("hechos", ArrayList(s.items.map { if (it.done) 1 else 0 }))
            dataMap.putStringArrayList("metas", ArrayList(s.goals.map { tr(it) }))
            // La mascota, con las claves de app.levelup.pets (vacío = sin mascota).
            dataMap.putString("mascota", pet?.species?.key.orEmpty())
            dataMap.putString("mascotaAnimo", pet?.mood?.name.orEmpty())
            dataMap.putString("mascotaAccesorios", pet?.let { Accessory.format(it.accessories) }.orEmpty())
            dataMap.putString("mascotaEtapa", pet?.stage?.name.orEmpty())
            dataMap.putString("mascotaNombre", pet?.name.orEmpty())
        }.asPutDataRequest().setUrgent()
        runCatching { Wearable.getDataClient(context).putDataItem(request).await() }
    }
}

/** Lo que pide el reloj: completar un reto o mandar los datos de hoy otra vez. */
class WearListenerService : WearableListenerService() {
    // Los avisos de WearableListenerService llegan en un hilo de fondo: se puede esperar aquí.
    override fun onMessageReceived(event: MessageEvent) {
        val app = application as LevelUpApplication
        when (event.path) {
            WearSync.PATH_COMPLETE -> {
                val id = event.data.decodeToString()
                if (id.isNotBlank() && id.length <= 120) runBlocking { AchievementNotice.show(app, app.repo.complete(id)) }
            }
            WearSync.PATH_REFRESH -> runBlocking {
                app.repo.ensureToday()
                WearSync.publish(this@WearListenerService, app.repo.todaySummary.first(), app.repo.petSnapshot())
            }
        }
    }
}
