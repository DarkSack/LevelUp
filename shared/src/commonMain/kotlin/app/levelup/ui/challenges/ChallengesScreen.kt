package app.levelup.ui.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import app.levelup.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Catalog
import app.levelup.content.Categories
import app.levelup.content.Tags
import app.levelup.domain.CustomStatus
import app.levelup.domain.InstanceStatus
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.EmptyState
import app.levelup.ui.components.MultiChoiceRow
import app.levelup.ui.extra.PlaceDialog
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.theme.Space
import app.levelup.i18n.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(vm: LevelUpViewModel, nav: Nav, tabBar: @Composable () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Hoy", "Explorar", "Míos", "Historial")
    ScreenScaffold(
        "Retos",
        bottomBar = tabBar,
        floating = {
            if (tab == 2) {
                ExtendedFloatingActionButton(
                    onClick = { nav.editCustom() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Crear reto") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) }) }
            }
            when (tab) {
                0 -> TodayTab(vm, nav)
                1 -> ExploreTab(vm, nav)
                2 -> MineTab(vm, nav)
                else -> HistoryTab(vm)
            }
        }
    }
}

private val listPadding = PaddingValues(horizontal = Space.l, vertical = Space.m)

@Composable
private fun TodayTab(vm: LevelUpViewModel, nav: Nav) {
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val visible = today.filter { it.status != InstanceStatus.REPLACED }
    var askPlace by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = listPadding) {
        if (visible.isEmpty()) item { EmptyState("Hoy no hay retos", "Puede ser un día de descanso. Si te apetece, añade uno.") }
        items(visible, key = { it.id }) { i -> InstanceRow(i, custom, { vm.complete(i.id) }, { nav.instance(i.id) }) }
        item { SecondaryButton("Quiero otro reto", { askPlace = true }, Modifier.padding(top = Space.m)) }
    }
    if (askPlace) PlaceDialog(onPick = { askPlace = false; vm.addExtraToday(it) }, onDismiss = { askPlace = false })
}

private const val NO_MATERIALS = "sin_materiales"
private const val FAVORITES = "favoritos"

@Composable
private fun ExploreTab(vm: LevelUpViewModel, nav: Nav) {
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("") }
    var difficulty by rememberSaveable { mutableIntStateOf(0) }
    var maxMinutes by rememberSaveable { mutableIntStateOf(0) }
    var place by rememberSaveable { mutableStateOf("") }
    var flags by rememberSaveable { mutableStateOf(listOf<String>()) }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val customs by vm.customChallenges.collectAsStateWithLifecycle()
    // Los favoritos pueden ser también retos propios; el resto de la lista es el catálogo.
    val pool = if (FAVORITES in flags) Catalog.challenges + customs.filter { it.status != CustomStatus.ARCHIVED }.map { it.toChallenge() }
    else Catalog.challenges
    val list = pool.filter { c ->
        (category.isEmpty() || c.category == category) &&
            (difficulty == 0 || c.difficulty == difficulty) &&
            when (maxMinutes) { 0 -> true; 999 -> c.minutes > 30; else -> c.minutes <= maxMinutes } &&
            (place.isEmpty() || place in c.tags) &&
            (NO_MATERIALS !in flags || Tags.SIN_MATERIALES in c.tags) &&
            (FAVORITES !in flags || c.id in favorites)
    }
    val scope = rememberCoroutineScope()
    LazyColumn(contentPadding = listPadding) {
        item {
            // Exploración: un reto que nunca hiciste, mejor de una categoría que no sueles elegir.
            SecondaryButton("Probar algo nuevo", {
                scope.launch { vm.surprise()?.let { nav.catalog(it.id) } ?: vm.notify("Ya probaste todo lo que encaja contigo.") }
            })
            ChoiceRow("Categoría", listOf("" to "Todas") + Categories.builtIn.map { it.key to it.name }, category, { category = it })
            ChoiceRow("Dificultad", listOf(0 to "Todas") + (1..5).map { it to Fmt.difficulty(it) }, difficulty, { difficulty = it })
            ChoiceRow("Duración", listOf(0 to "Todas", 5 to "Hasta 5 min", 15 to "Hasta 15 min", 30 to "Hasta 30 min", 999 to "Más de 30 min"),
                maxMinutes, { maxMinutes = it })
            ChoiceRow("Lugar", listOf("" to "Cualquiera") + Tags.places, place, { place = it })
            MultiChoiceRow(listOf(NO_MATERIALS to "Sin materiales", FAVORITES to "Solo favoritos"), flags.toSet()) { f ->
                flags = if (f in flags) flags - f else flags + f
            }
            Text(Fmt.plural(list.size, "reto", "retos"), style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = Space.s).semantics { heading() })
        }
        if (list.isEmpty()) item { EmptyState("Nada con esos filtros", "Prueba a quitar alguno.") }
        items(list, key = { it.id }) { c -> ChallengeRow(c, custom, favorite = c.id in favorites) { nav.catalog(c.id) } }
    }
}

@Composable
private fun MineTab(vm: LevelUpViewModel, nav: Nav) {
    val customs by vm.customChallenges.collectAsStateWithLifecycle()
    val categories by vm.customCategories.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.m, bottom = Space.xxl * 3)) {
        if (customs.isEmpty()) {
            item {
                EmptyState("Aún no tienes retos propios", "Crea los tuyos. Si les pones días fijos, se convierten en hábitos.")
            }
        }
        listOf(CustomStatus.ACTIVE to "Activos", CustomStatus.PAUSED to "En pausa", CustomStatus.ARCHIVED to "Archivados").forEach { (status, title) ->
            val group = customs.filter { it.status == status }
            if (group.isNotEmpty()) {
                item(key = "h-$status") {
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Space.m).semantics { heading() })
                }
                items(group, key = { it.id }) { c ->
                    val ch = c.toChallenge()
                    val extra = if (ch.isHabit) tr("Hábito: {0}", app.levelup.domain.Days.list(c.habitDays).joinToString { Fmt.weekdayShort(it) }) else null
                    ChallengeRow(ch, categories, extra) { nav.editCustom(c.id) }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(vm: LevelUpViewModel) {
    val recent by vm.recent.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val past = recent.filter { it.date < today && it.status != InstanceStatus.REPLACED }.groupBy { it.date }
    LazyColumn(contentPadding = listPadding) {
        if (past.isEmpty()) item { EmptyState("Todavía no hay historial", "Aquí verás los retos de días anteriores.") }
        past.forEach { (date, list) ->
            item(key = "d-$date") {
                Text(Fmt.day(date).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = Space.m).semantics { heading() })
            }
            items(list, key = { it.id }) { i ->
                Column(Modifier.padding(vertical = Space.xs).semantics(mergeDescendants = true) {}) {
                    Text(i.title, style = MaterialTheme.typography.bodyLarge)
                    val status = if (i.isOpen) "Sin hacer" else statusText(i)
                    Text("$status · ${metaLine(i, custom)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
