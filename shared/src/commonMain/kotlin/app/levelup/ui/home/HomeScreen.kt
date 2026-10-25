package app.levelup.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Avatars
import app.levelup.content.Categories
import app.levelup.content.Cosmetics
import app.levelup.content.Lab
import app.levelup.domain.Challenge
import app.levelup.domain.DayBlock
import app.levelup.domain.DayKind
import app.levelup.domain.GamificationStyle
import app.levelup.domain.GoalStatus
import app.levelup.domain.InstanceStatus
import app.levelup.domain.XpRules
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.ProgressState
import app.levelup.ui.challenges.InstanceRow
import app.levelup.ui.challenges.metaLine
import app.levelup.ui.components.Avatar
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.LabeledProgress
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.extra.PlaceDialog
import app.levelup.ui.extra.ResumeDialog
import app.levelup.ui.extra.resumableHabits
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space
import app.levelup.ui.theme.gameOn
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: LevelUpViewModel, nav: Nav, tabBar: @Composable () -> Unit) {
    val prefs = LocalPrefs.current
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val customs by vm.customChallenges.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val visible = today.filter { it.status != InstanceStatus.REPLACED }
    val main = visible.firstOrNull { it.isOpen }
    val simple = prefs.simpleMode
    val gameOn = prefs.gameOn
    val hour = remember { vm.hourNow() }
    var askPlace by remember { mutableStateOf(false) }
    var surprise by remember { mutableStateOf<Challenge?>(null) }
    var noSurprise by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        "Inicio", bottomBar = tabBar,
        actions = { IconButton(onClick = nav::search) { Icon(Icons.Filled.Search, contentDescription = "Buscar") } },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding() + Space.s,
                bottom = padding.calculateBottomPadding() + Space.xl),
        ) {
            item {
                val greeting = when (hour) { in 5..11 -> "Buenos días"; in 12..19 -> "Buenas tardes"; else -> "Buenas noches" }
                Text(
                    if (prefs.displayName.isBlank()) greeting else "$greeting, ${prefs.displayName}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }.padding(bottom = Space.m),
                )
            }
            if (!simple && gameOn) {
                item { LevelCard(progress, prefs.equippedFrame, prefs.displayName, Avatars.emoji(prefs.avatar), prefs.gamification) }
            }

            if (progress.returning) {
                item {
                    InfoCard(Modifier.padding(top = Space.m), container = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Qué bien verte de nuevo", style = MaterialTheme.typography.titleMedium)
                        Text("Hoy tienes retos cortos y sencillos para retomar sin presión. Lo que hiciste antes sigue contando.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!simple && progress.weeklyReviewDue) {
                item {
                    InfoCard(Modifier.padding(top = Space.m)) {
                        Text("Tu semana en un vistazo", style = MaterialTheme.typography.titleMedium)
                        Text("Mira cómo fue, ajusta lo que no encaje y repasa tus metas. Son dos minutos.", style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = vm::markWeeklyReviewed) { Text("Ahora no") }
                            TextButton(onClick = nav::weeklyReview) { Text("Revisar") }
                        }
                    }
                }
            }

            if (visible.isEmpty() && progress.dayKind != DayKind.ACTIVE) {
                item {
                    InfoCard(Modifier.padding(top = Space.m)) {
                        Text(
                            when (progress.dayKind) {
                                DayKind.PAUSED -> "Tus objetivos están en pausa"
                                DayKind.REST -> "Hoy es día de descanso"
                                else -> "Hoy no tienes retos programados"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text("Descansar también es parte del camino. Tu progreso sigue aquí.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.size(Space.m))
                        SecondaryButton("Quiero un reto igualmente", { askPlace = true })
                    }
                }
            }

            suggestions.forEach { s ->
                item(key = "sug-${s.category}") {
                    InfoCard(Modifier.padding(top = Space.m), container = MaterialTheme.colorScheme.secondaryContainer) {
                        val name = Categories.name(s.category, custom)
                        Text(
                            if (s.isUp) "Los retos de $name te están saliendo bien. ¿Subimos a dificultad ${s.to} (${Fmt.difficulty(s.to).lowercase()})?"
                            else "Los retos de $name están costando. ¿Bajamos a dificultad ${s.to} (${Fmt.difficulty(s.to).lowercase()})?",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { vm.dismissSuggestion(s) }) { Text("Ahora no") }
                            TextButton(onClick = { vm.applySuggestion(s) }) { Text(if (s.isUp) "Subir" else "Bajar") }
                        }
                    }
                }
            }

            if (main != null) {
                item(key = "main") {
                    InfoCard(Modifier.padding(top = Space.m), container = MaterialTheme.colorScheme.primaryContainer) {
                        Text(if (main.status == InstanceStatus.STARTED) "Reto principal · en curso" else "Reto principal",
                            style = MaterialTheme.typography.labelLarge)
                        Text(main.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = Space.xs))
                        Text(metaLine(main, custom, gameOn), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.size(Space.m))
                        PrimaryButton("Completar", { vm.complete(main.id) })
                        Spacer(Modifier.size(Space.s))
                        SecondaryButton("Ver detalles", { nav.instance(main.id) })
                        TextButton(onClick = { nav.focus(main.id) }) { Text("Modo concentración") }
                    }
                }
            } else if (visible.isNotEmpty() && visible.none { it.isOpen }) {
                item {
                    InfoCard(Modifier.padding(top = Space.m), container = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Retos de hoy terminados", style = MaterialTheme.typography.titleMedium)
                        Text("Lo que hiciste hoy ya cuenta. Si te quedan ganas, puedes añadir otro.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.size(Space.m))
                        SecondaryButton("Añadir otro reto", { askPlace = true })
                    }
                }
            }

            if (visible.isNotEmpty()) {
                item {
                    val done = visible.count { it.isDone }
                    SectionTitle("Hoy")
                    LabeledProgress(done / visible.size.toFloat(), "$done de ${Fmt.plural(visible.size, "reto", "retos")} hechos")
                    val week = progress.weeks.lastOrNull()
                    if (week != null) {
                        Text(
                            "Esta semana: ${week.activeDays} de ${Fmt.plural(week.goal, "día", "días")}" + if (week.met) " · objetivo cumplido" else "",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Space.xs),
                        )
                    }
                }
                val rest = visible.filter { it.id != main?.id }
                // Por momentos del día solo si alguno tiene momento: si no, la lista de siempre.
                val groups = if (rest.any { it.block != null }) {
                    (DayBlock.entries.map { it as DayBlock? } + null).map { b -> b to rest.filter { it.block == b } }.filter { it.second.isNotEmpty() }
                } else {
                    listOf(null to rest)
                }
                groups.forEach { (block, list) ->
                    if (groups.size > 1 || block != null) {
                        item(key = "b-$block") {
                            Text(block?.let(Fmt::block) ?: "En cualquier momento", style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = Space.m).semantics { heading() })
                        }
                    }
                    items(list, key = { it.id }) { i ->
                        InstanceRow(i, custom, onComplete = { vm.complete(i.id) }, onOpen = { nav.instance(i.id) })
                    }
                }
            }

            if (!simple && Lab.SURPRISE in prefs.labFlags) {
                item {
                    SectionTitle("Reto sorpresa")
                    InfoCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                        val s = surprise
                        when {
                            s != null -> {
                                Text(s.title, style = MaterialTheme.typography.titleSmall)
                                Text(s.goal, style = MaterialTheme.typography.bodyMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { nav.catalog(s.id) }) { Text("Ver") }
                                    TextButton(onClick = { vm.addToToday(s.id); surprise = null }) { Text("Añadir a hoy") }
                                }
                            }
                            noSurprise -> Text("Ya probaste todo lo que encaja contigo. ¡Impresionante!", style = MaterialTheme.typography.bodyMedium)
                            else -> {
                                Text("Algo que nunca has hecho. Solo es una idea: no cambia tu plan.", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    scope.launch {
                                        surprise = vm.surprise()
                                        noSurprise = surprise == null
                                    }
                                }) { Text("Sorpréndeme") }
                            }
                        }
                    }
                }
            }

            val activeGoals = goals.filter { it.status == GoalStatus.ACTIVE }
            if (!simple && activeGoals.isNotEmpty()) {
                item {
                    SectionTitle("Tus metas")
                    activeGoals.take(3).forEach { Text("• ${it.title}", style = MaterialTheme.typography.bodyLarge) }
                    TextButton(onClick = nav::goals) { Text("Ver mis metas") }
                }
            }

            val mission = progress.activeMission
            if (!simple && gameOn && mission != null) {
                item {
                    SectionTitle("Misión")
                    InfoCard {
                        Text(mission.first.title, style = MaterialTheme.typography.titleSmall)
                        Text(mission.first.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(Space.s))
                        LabeledProgress(
                            progress.missionProgress / mission.first.goal.target.toFloat(),
                            "${progress.missionProgress} de ${mission.first.goal.target} · hasta el ${Fmt.day(mission.second.endsOn)}",
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            if (!simple && gameOn && progress.categoryLevels.isNotEmpty()) {
                item {
                    SectionTitle("Tus categorías")
                    progress.categoryLevels.entries.sortedByDescending { it.value.totalXp }.take(4).forEach { (key, lp) ->
                        LabeledProgress(lp.fraction, "${Categories.name(key, custom)} · nivel ${lp.level}",
                            Modifier.padding(vertical = Space.xs))
                    }
                    TextButton(onClick = nav::achievements) { Text("Ver logros y trofeos") }
                }
            }
        }
    }

    if (askPlace) {
        PlaceDialog(onPick = { place -> askPlace = false; vm.addExtraToday(place) }, onDismiss = { askPlace = false })
    }
    val pending = progress.pendingReturn
    if (pending != null && progress.dayKind != DayKind.PAUSED) {
        val habits = resumableHabits(customs)
        if (habits.isEmpty()) {
            LaunchedEffect(pending.id) { vm.resumeAfterPause(pending.id, emptySet()) }
        } else {
            ResumeDialog(pending, habits) { keep -> vm.resumeAfterPause(pending.id, keep) }
        }
    }
}

@Composable
private fun LevelCard(progress: ProgressState, frameId: String, name: String, emoji: String?, style: GamificationStyle) {
    val frame = Cosmetics.byId[frameId] ?: Cosmetics.byId.getValue(Cosmetics.DEFAULT)
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, frame, progress.level.level, emoji = emoji)
            Spacer(Modifier.size(Space.l))
            Column(Modifier.weight(1f)) {
                Text("Nivel ${progress.level.level} · ${XpRules.titleFor(progress.level.level)}", style = MaterialTheme.typography.titleMedium)
                LabeledProgress(
                    progress.level.fraction,
                    "${progress.level.xpInLevel} de ${progress.level.xpForNext} XP para el nivel ${progress.level.level + 1}",
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (style == GamificationStyle.FULL && progress.stats.currentStreak > 0) {
                    Text("Racha: ${Fmt.plural(progress.stats.currentStreak, "día", "días")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
