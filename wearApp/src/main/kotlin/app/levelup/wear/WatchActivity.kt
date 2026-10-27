package app.levelup.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch

class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WatchApp() } }
    }
}

@Composable
private fun WatchApp() {
    val context = LocalContext.current
    val today by remember { WatchData.flow(context) }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    // Al abrir, pedir al teléfono los datos de hoy por si cambió el día.
    LaunchedEffect(Unit) {
        if (!runCatching { WatchData.send(context, WatchData.PATH_REFRESH) }.getOrDefault(false)) {
            message = "Sin conexión con el teléfono"
        }
    }
    WatchScreen(today, message) { i ->
        scope.launch {
            val ok = runCatching { WatchData.send(context, WatchData.PATH_COMPLETE, i.id) }.getOrDefault(false)
            message = if (ok) "Enviado al teléfono" else "Sin conexión con el teléfono"
        }
    }
}

/** La pantalla con lo que haya: sin datos del teléfono, sin retos o la lista de hoy. Aparte para dibujarla en las capturas. */
@Composable
internal fun WatchScreen(today: Today?, message: String?, onComplete: (Today.Item) -> Unit) {
    val list = rememberScalingLazyListState()
    AppScaffold {
        ScreenScaffold(scrollState = list) {
            ScalingLazyColumn(state = list, modifier = Modifier.fillMaxWidth()) {
                item { ListHeader { Text("LevelUp") } }
                val t = today
                when {
                    t == null || t.stale -> item {
                        Text("Abre LevelUp en el teléfono para ver tus retos de hoy.", textAlign = TextAlign.Center)
                    }
                    t.total == 0 -> item { Text("Hoy no hay retos. Descansar también cuenta.", textAlign = TextAlign.Center) }
                    else -> {
                        item { Text("${t.done} de ${t.total} hechos", style = MaterialTheme.typography.titleMedium) }
                        t.items.forEach { i ->
                            item(key = i.id) {
                                Button(
                                    onClick = { if (!i.done) onComplete(i) },
                                    enabled = !i.done,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(i.title, maxLines = 2) },
                                    secondaryLabel = { Text(if (i.done) "Hecho" else "Tocar para completar") },
                                )
                            }
                        }
                        if (t.goals.isNotEmpty()) {
                            item { ListHeader { Text("Tus metas") } }
                            t.goals.forEach { g -> item { Text("• $g") } }
                        }
                    }
                }
                message?.let { m -> item { Text(m, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) } }
            }
        }
    }
}
