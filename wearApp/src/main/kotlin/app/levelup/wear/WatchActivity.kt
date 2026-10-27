package app.levelup.wear

import android.os.Bundle
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
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
    var message by remember { mutableStateOf<Int?>(null) }
    // Al abrir, pedir al teléfono los datos de hoy por si cambió el día.
    LaunchedEffect(Unit) {
        if (!runCatching { WatchData.send(context, WatchData.PATH_REFRESH) }.getOrDefault(false)) {
            message = R.string.no_connection
        }
    }
    WatchScreen(today, message) { i ->
        scope.launch {
            val ok = runCatching { WatchData.send(context, WatchData.PATH_COMPLETE, i.id) }.getOrDefault(false)
            message = if (ok) R.string.sent else R.string.no_connection
        }
    }
}

/** La pantalla con lo que haya: sin datos del teléfono, sin retos o la lista de hoy. Aparte para dibujarla en las capturas. */
@Composable
internal fun WatchScreen(today: Today?, @StringRes message: Int?, onComplete: (Today.Item) -> Unit) {
    val list = rememberScalingLazyListState()
    AppScaffold {
        ScreenScaffold(scrollState = list) {
            ScalingLazyColumn(state = list, modifier = Modifier.fillMaxWidth()) {
                item { ListHeader { Text("LevelUp") } }
                val t = today
                when {
                    t == null || t.stale -> item {
                        Text(stringResource(R.string.open_phone), textAlign = TextAlign.Center)
                    }
                    t.total == 0 -> item { Text(stringResource(R.string.no_challenges), textAlign = TextAlign.Center) }
                    else -> {
                        item { Text(stringResource(R.string.done_count, t.done, t.total), style = MaterialTheme.typography.titleMedium) }
                        t.items.forEach { i ->
                            item(key = i.id) {
                                Button(
                                    onClick = { if (!i.done) onComplete(i) },
                                    enabled = !i.done,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(i.title, maxLines = 2) },
                                    secondaryLabel = { Text(stringResource(if (i.done) R.string.done else R.string.tap_to_complete)) },
                                )
                            }
                        }
                        if (t.goals.isNotEmpty()) {
                            item { ListHeader { Text(stringResource(R.string.goals)) } }
                            t.goals.forEach { g -> item { Text("• $g") } }
                        }
                    }
                }
                message?.let { m -> item { Text(stringResource(m), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) } }
            }
        }
    }
}
