package app.levelup.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** Los retos de hoy tal como los manda el teléfono (ver WearSync en androidApp: mismas rutas y claves). */
data class Today(
    val date: String,
    val mainId: String?,
    val mainTitle: String?,
    val done: Int,
    val total: Int,
    val items: List<Item>,
    val goals: List<String>,
) {
    data class Item(val id: String, val title: String, val done: Boolean)

    /** Datos de otro día: el teléfono aún no generó los de hoy. */
    val stale: Boolean get() = date != LocalDate.now().toString()
}

object WatchData {
    const val PATH_TODAY = "/levelup/hoy"
    const val PATH_COMPLETE = "/levelup/completar"
    const val PATH_REFRESH = "/levelup/pedir"

    private fun parse(map: DataMap): Today {
        val ids = map.getStringArrayList("ids").orEmpty()
        val titles = map.getStringArrayList("titulos").orEmpty()
        val done = map.getIntegerArrayList("hechos").orEmpty()
        return Today(
            date = map.getString("fecha", ""),
            mainId = map.getString("principalId", "").ifEmpty { null },
            mainTitle = map.getString("principalTitulo", "").ifEmpty { null },
            done = map.getInt("hechosHoy"),
            total = map.getInt("total"),
            items = ids.indices.map { i -> Today.Item(ids[i], titles.getOrElse(i) { "" }, done.getOrElse(i) { 0 } == 1) },
            goals = map.getStringArrayList("metas").orEmpty(),
        )
    }

    private val uri: Uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH_TODAY).build()

    suspend fun load(context: Context): Today? {
        val buffer = Wearable.getDataClient(context).getDataItems(uri).await()
        return try {
            buffer.firstOrNull()?.let { parse(DataMapItem.fromDataItem(it).dataMap) }
        } finally {
            buffer.release()
        }
    }

    fun flow(context: Context): Flow<Today?> = callbackFlow {
        val client = Wearable.getDataClient(context)
        val listener = DataClient.OnDataChangedListener { events ->
            events.filter { it.dataItem.uri.path == PATH_TODAY }
                .forEach { trySend(parse(DataMapItem.fromDataItem(it.dataItem).dataMap)) }
        }
        client.addListener(listener)
        trySend(runCatching { load(context) }.getOrNull())
        awaitClose { client.removeListener(listener) }
    }

    /** Pide al teléfono que complete un reto (o que mande datos frescos). false si no hay teléfono conectado. */
    suspend fun send(context: Context, path: String, payload: String = ""): Boolean {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        nodes.forEach { Wearable.getMessageClient(context).sendMessage(it.id, path, payload.encodeToByteArray()).await() }
        return nodes.isNotEmpty()
    }
}
