package app.levelup.ui.extra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Lab
import app.levelup.content.Tags
import app.levelup.domain.Challenge
import app.levelup.domain.CustomChallenge
import app.levelup.domain.CustomStatus
import app.levelup.domain.Days
import app.levelup.domain.GoalRules
import app.levelup.domain.GoalStatus
import app.levelup.domain.HitKind
import app.levelup.domain.Pause
import app.levelup.domain.PersonalGoal
import app.levelup.domain.Preferences
import app.levelup.domain.ReviewSuggestion
import app.levelup.domain.Search
import app.levelup.domain.SearchHit
import app.levelup.domain.WeeklySummary
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.challenges.FocusTimer
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.EmptyState
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.settings.GoalsSection
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space
import app.levelup.ui.theme.gameOn
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// ── Búsqueda ──────────────────────────────────────────────────────────────────

private fun kindName(kind: HitKind) = when (kind) {
    HitKind.CHALLENGE -> "Retos"
    HitKind.HABIT -> "Hábitos"
    HitKind.ACHIEVEMENT -> "Logros"
    HitKind.GOAL -> "Metas"
    HitKind.NOTE -> "Notas"
}

@Composable
fun SearchScreen(vm: LevelUpViewModel, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    val hits by produceState(emptyList<SearchHit>(), query) {
        delay(250) // espera a que se deje de escribir
        value = vm.search(query)
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val gameOn = LocalPrefs.current.gameOn

    ScreenScaffold("Buscar", onBack = nav::back) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.s,
                bottom = padding.calculateBottomPadding() + Space.xl),
        ) {
            item {
                OutlinedTextField(
                    query, { query = it.take(60) }, Modifier.fillMaxWidth().focusRequester(focus),
                    label = { Text("Retos, hábitos, logros, metas o notas") }, singleLine = true,
                )
            }
            if (query.trim().length >= Search.MIN_QUERY && hits.isEmpty()) {
                item { EmptyState("Sin resultados", "Prueba con otra palabra. No importan los acentos ni las mayúsculas.") }
            }
            HitKind.entries.filter { gameOn || it != HitKind.ACHIEVEMENT }.forEach { kind ->
                val group = hits.filter { it.kind == kind }
                if (group.isNotEmpty()) {
                    item(key = "h-$kind") { SectionTitle(kindName(kind)) }
                    items(group, key = { "${it.kind}-${it.id}" }) { h ->
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClickLabel = "Abrir") {
                                when (h.kind) {
                                    HitKind.CHALLENGE, HitKind.HABIT -> nav.catalog(h.id)
                                    HitKind.ACHIEVEMENT -> nav.achievements()
                                    HitKind.GOAL -> nav.goals()
                                    // shortcut: el detalle solo encuentra retos de los últimos 60 días; las notas más viejas se leen aquí.
                                    HitKind.NOTE -> nav.instance(h.id)
                                }
                            }.padding(vertical = Space.s).semantics(mergeDescendants = true) {},
                        ) {
                            Text(h.title, style = MaterialTheme.typography.titleSmall)
                            val detail = listOfNotNull(h.date?.let { Fmt.day(it) }, h.detail.takeIf { it.isNotBlank() }).joinToString(" · ")
                            if (detail.isNotEmpty()) {
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Modo concentración ────────────────────────────────────────────────────────

/** Un solo reto en pantalla, sin barra de pestañas ni nada más que hacer que eso. */
@Composable
fun FocusScreen(vm: LevelUpViewModel, nav: Nav, instanceId: String?) {
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val inst = if (instanceId == null) today.firstOrNull { it.isOpen } else (today + recent).firstOrNull { it.id == instanceId }
    val definition by produceState<Challenge?>(null, inst?.challengeId) { value = inst?.let { vm.challenge(it.challengeId) } }

    ScreenScaffold("Concentración", onBack = nav::back) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            if (inst == null) {
                EmptyState("Nada pendiente", "No tienes retos abiertos ahora mismo.")
                return@Column
            }
            Text(inst.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            val d = definition
            if (d != null) Text(d.goal, style = MaterialTheme.typography.bodyLarge)
            if (d != null && d.minimum.isNotBlank()) {
                Text("Versión mínima: ${d.minimum}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (inst.isOpen) {
                FocusTimer(inst.minutes)
                PrimaryButton("Completar", { vm.complete(inst.id); nav.back() })
                if (d != null && d.minimum.isNotBlank()) SecondaryButton("Hice la versión mínima", { vm.completeMinimal(inst.id); nav.back() })
            } else {
                Text("Este reto ya no está abierto.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ── Metas personales ──────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(vm: LevelUpViewModel, nav: Nav) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PersonalGoal?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PersonalGoal?>(null) }

    ScreenScaffold(
        "Mis metas", onBack = nav::back,
        floating = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva meta") },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.s, bottom = 120.dp)) {
            item {
                Text(
                    "Lo que te importa aunque no sea un reto: «terminar el curso», «correr 5 km». La fecha es opcional y nada caduca.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (goals.isEmpty()) item { EmptyState("Aún no tienes metas", "Crea una cuando quieras. Puedes repasarlas cada semana.") }
            listOf(GoalStatus.ACTIVE to "En marcha", GoalStatus.ACHIEVED to "Logradas", GoalStatus.ARCHIVED to "Archivadas").forEach { (status, title) ->
                val group = goals.filter { it.status == status }
                if (group.isNotEmpty()) {
                    item(key = "h-$status") { SectionTitle(title) }
                    items(group, key = { it.id }) { g ->
                        InfoCard(Modifier.padding(vertical = Space.xs)) {
                            Text(g.title, style = MaterialTheme.typography.titleSmall)
                            if (g.why.isNotBlank()) Text(g.why, style = MaterialTheme.typography.bodyMedium)
                            val dates = listOfNotNull(
                                g.targetDate?.let { "Para el ${Fmt.dayWithYear(it)}" },
                                g.achievedAt?.let { "Lograda" },
                            ).joinToString(" · ")
                            if (dates.isNotEmpty()) Text(dates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = g }) { Text("Editar") }
                                when (g.status) {
                                    GoalStatus.ACTIVE -> {
                                        TextButton(onClick = { vm.setGoalStatus(g.id, GoalStatus.ARCHIVED) }) { Text("Archivar") }
                                        TextButton(onClick = { vm.setGoalStatus(g.id, GoalStatus.ACHIEVED) }) { Text("¡Lograda!") }
                                    }
                                    else -> {
                                        TextButton(onClick = { deleting = g }) { Text("Eliminar") }
                                        TextButton(onClick = { vm.setGoalStatus(g.id, GoalStatus.ACTIVE) }) { Text("Reactivar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        GoalDialog(vm, editing, today) { creating = false; editing = null }
    }
    deleting?.let { g ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar «${g.title}»?") },
            text = { Text("No afecta a tus retos ni a tu progreso.") },
            confirmButton = { TextButton(onClick = { vm.deleteGoal(g.id); deleting = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun GoalDialog(vm: LevelUpViewModel, goal: PersonalGoal?, today: LocalDate, onClose: () -> Unit) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var why by remember { mutableStateOf(goal?.why ?: "") }
    // -1 mantiene la fecha que ya tenía; 0 sin fecha; si no, meses desde hoy.
    var months by remember { mutableIntStateOf(if (goal?.targetDate != null) -1 else 0) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (goal == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                OutlinedTextField(title, { title = it.take(GoalRules.TITLE_MAX) }, label = { Text("Meta") }, singleLine = true,
                    isError = error != null, supportingText = { Text(error ?: "${title.length}/${GoalRules.TITLE_MAX}") })
                OutlinedTextField(why, { why = it.take(GoalRules.WHY_MAX) }, label = { Text("Por qué te importa (opcional)") }, minLines = 2)
                val options = buildList {
                    goal?.targetDate?.let { add(-1 to "El ${Fmt.day(it)}") }
                    addAll(listOf(0 to "Sin fecha", 1 to "1 mes", 3 to "3 meses", 6 to "6 meses", 12 to "1 año"))
                }
                ChoiceRow("¿Para cuándo?", options, months, { months = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = when (months) {
                    -1 -> goal?.targetDate
                    0 -> null
                    else -> today.plus(months, DateTimeUnit.MONTH)
                }
                vm.saveGoal(goal?.id, title, why, target) { err -> if (err == null) onClose() else error = err }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

// ── Revisión semanal ──────────────────────────────────────────────────────────

private fun suggestionText(s: ReviewSuggestion): Pair<String, String> = when (s) {
    is ReviewSuggestion.FewerPerDay ->
        "Quedaron bastantes retos sin hacer. Con menos al día puede ser más llevadero." to "Pasar a ${s.to} al día"
    is ReviewSuggestion.MorePerDay ->
        "Hiciste casi todo y te resultó fácil. Si te apetece, puedes sumar uno." to "Pasar a ${s.to} al día"
    is ReviewSuggestion.MoveReminder ->
        "Sueles completar tus retos hacia las ${Fmt.time(s.minuteOfDay + 30)}." to "Avisarme a las ${Fmt.time(s.minuteOfDay)}"
    is ReviewSuggestion.ReminderDays ->
        "Tu actividad se concentra en: ${Days.list(s.mask).joinToString { Fmt.weekday(it) }}." to "Avisarme solo esos días"
}

@Composable
fun WeeklyReviewScreen(vm: LevelUpViewModel, nav: Nav) {
    val prefs = LocalPrefs.current
    val summary by produceState<WeeklySummary?>(null) { value = vm.weeklySummary() }
    val goals by vm.goals.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    var applied by remember { mutableStateOf(setOf<ReviewSuggestion>()) }

    ScreenScaffold("Tu semana", onBack = nav::back) { padding ->
        val s = summary ?: return@ScreenScaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text("Del ${Fmt.day(s.weekStart)} al ${Fmt.day(s.weekStart.plus(6, DateTimeUnit.DAY))}", style = MaterialTheme.typography.titleMedium)
            InfoCard {
                if (s.completed == 0) {
                    Text("Fue una semana tranquila. No pasa nada: esta empieza de cero.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Completaste ${s.completed} de ${Fmt.plural(s.planned, "reto", "retos")}.", style = MaterialTheme.typography.bodyLarge)
                    Text("Días con actividad: ${s.activeDays}", style = MaterialTheme.typography.bodyMedium)
                    if (prefs.gameOn) Text("XP ganada: ${s.xp}", style = MaterialTheme.typography.bodyMedium)
                    s.topCategory?.let { Text("Lo que más hiciste: ${app.levelup.content.Categories.name(it, custom)}", style = MaterialTheme.typography.bodyMedium) }
                    if (prefs.gameOn && s.achievements.isNotEmpty()) {
                        Text("Logros: ${s.achievements.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionTitle("Ajustes que te proponemos")
            if (s.suggestions.isEmpty()) Text("Nada que cambiar: tu ritmo encaja.", style = MaterialTheme.typography.bodyMedium)
            s.suggestions.forEach { sug ->
                val (text, action) = suggestionText(sug)
                InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (sug in applied) {
                            Text("Aplicado", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(Space.m))
                        } else {
                            TextButton(onClick = { vm.applyReviewSuggestion(sug); applied = applied + sug }) { Text(action) }
                        }
                    }
                }
            }
            Text("Nada cambia hasta que pulsas. Los avisos se basan solo en lo que hiciste aquí.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionTitle("Tus metas")
            val active = goals.filter { it.status == GoalStatus.ACTIVE }
            if (active.isEmpty()) {
                Text("No tienes metas en marcha.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("¿Siguen siendo importantes?", style = MaterialTheme.typography.bodyMedium)
                active.forEach { g ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.setGoalStatus(g.id, GoalStatus.ARCHIVED) }) { Text("Ya no") }
                        TextButton(onClick = { vm.setGoalStatus(g.id, GoalStatus.ACHIEVED) }) { Text("Lograda") }
                    }
                }
            }
            SecondaryButton("Ver o crear metas", nav::goals)

            SectionTitle("Prioridades")
            GoalsSection(prefs, custom) { change -> vm.updatePrefs(change) }

            PrimaryButton("Listo", { vm.markWeeklyReviewed(); nav.back() }, Modifier.padding(top = Space.m))
        }
    }
}

// ── Diálogos de Inicio ────────────────────────────────────────────────────────

/** «¿Dónde estás?»: el reto extra sale de los que se pueden hacer ahí. */
@Composable
fun PlaceDialog(onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Dónde estás?") },
        text = {
            Column {
                (Tags.places + (null to "Me da igual")).forEach { (key, label) ->
                    TextButton(onClick = { onPick(key) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Al volver de una pausa: qué hábitos retomar. Los demás quedan en pausa, sin nada atrasado. */
@Composable
fun ResumeDialog(pause: Pause, habits: List<CustomChallenge>, onConfirm: (Set<String>) -> Unit) {
    var keep by remember(pause.id) { mutableStateOf(habits.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Qué bien verte de nuevo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("No hay nada atrasado. ¿Qué hábitos quieres retomar? Los demás quedan en pausa y puedes reanudarlos cuando quieras.")
                Spacer(Modifier.size(Space.s))
                habits.forEach { h ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(h.id in keep, role = Role.Checkbox) { on -> keep = if (on) keep + h.id else keep - h.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(h.id in keep, onCheckedChange = null)
                        Spacer(Modifier.size(Space.s))
                        Text(h.title)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(keep) }) { Text("Retomar") } },
    )
}

/** Hábitos activos que se ofrecen al volver de una pausa. */
fun resumableHabits(customs: List<CustomChallenge>) = customs.filter { it.status == CustomStatus.ACTIVE && it.habitDays != 0 }

// ── Laboratorio ───────────────────────────────────────────────────────────────

@Composable
fun LabSection(vm: LevelUpViewModel, prefs: Preferences) {
    Text(
        "Funciones en prueba. Actívalas o apágalas cuando quieras: no cambian tu XP, tus rachas ni tu historial.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Lab.all.filter { it.id != Lab.HEALTH || vm.health != null }.forEach { e ->
        ToggleRow(e.name, e.id in prefs.labFlags, { vm.setLab(e.id, it) }, e.description)
    }
}
