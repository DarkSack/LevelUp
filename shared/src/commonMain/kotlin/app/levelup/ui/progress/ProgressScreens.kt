package app.levelup.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.AchievementCatalog
import app.levelup.content.Categories
import app.levelup.content.Cosmetics
import app.levelup.content.MissionCatalog
import app.levelup.domain.AchievementState
import app.levelup.domain.CustomStatus
import app.levelup.domain.Days
import app.levelup.domain.GamificationStyle
import app.levelup.domain.MissionGoal
import app.levelup.domain.Streaks
import app.levelup.domain.XpRules
import app.levelup.ui.AchievementView
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.EmptyState
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.LabeledProgress
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.TrophyBadge
import app.levelup.ShareCard
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space
import app.levelup.ui.theme.gameOn

@Composable
fun ProgressScreen(vm: LevelUpViewModel, nav: Nav, tabBar: @Composable () -> Unit) {
    val prefs = LocalPrefs.current
    val p by vm.progress.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val customs by vm.customChallenges.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    ScreenScaffold("Progreso", bottomBar = tabBar) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + Space.xl),
        ) {
            if (prefs.gameOn) item {
                InfoCard(Modifier.padding(top = Space.s)) {
                    Text("Nivel ${p.level.level} · ${XpRules.titleFor(p.level.level)}", style = MaterialTheme.typography.titleLarge)
                    LabeledProgress(p.level.fraction, "${p.level.totalXp} XP en total · ${p.level.xpForNext - p.level.xpInLevel} para el siguiente nivel",
                        color = MaterialTheme.colorScheme.secondary)
                    XpRules.nextTitle(p.level.level)?.let { (lvl, title) ->
                        Text("En el nivel $lvl tu título será «$title».", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Space.xs))
                    }
                }
                PrimaryButton("Logros y trofeos", nav::achievements, Modifier.padding(top = Space.m))
            }

            if (prefs.gameOn) {
                item { SectionTitle("Por categoría") }
                if (p.categoryLevels.isEmpty()) {
                    item { Text("Completa tu primer reto para ver tus categorías.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(p.categoryLevels.entries.sortedByDescending { it.value.totalXp }, key = { it.key }) { (key, lp) ->
                    LabeledProgress(lp.fraction, "${Categories.name(key, custom)} · nivel ${lp.level} · ${lp.totalXp} XP",
                        Modifier.padding(vertical = Space.xs))
                }
                item {
                    Text("Los niveles miden lo que has practicado aquí, no lo que vales.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.xs))
                }

                item {
                    SectionTitle("XP por semana")
                    WeeklyChart(p.weeklyXp)
                }
            }

            item {
                SectionTitle("Tus números")
                val monthActive = recent.filter { it.isDone && it.date.month == today.month && it.date.year == today.year }.map { it.date }.distinct().size
                Stat("Retos completados", "${p.stats.totalCompleted}")
                Stat("Días activos este mes", "$monthActive")
                if (prefs.gamification == GamificationStyle.FULL) {
                    Stat("Racha actual", Fmt.plural(p.stats.currentStreak, "día", "días"))
                    Stat("Mejor racha", Fmt.plural(p.stats.bestStreak, "día", "días"))
                }
                Stat("Semanas con objetivo cumplido", "${p.stats.weeksGoalMet}")
                Stat("Categorías exploradas", "${p.stats.distinctCategories}")
            }

            item {
                SectionTitle("Últimas semanas")
                WeeksRow(p.weeks.takeLast(8))
            }

            val habits = customs.filter { it.status == CustomStatus.ACTIVE && it.habitDays != 0 }
            if (habits.isNotEmpty()) {
                item { SectionTitle("Hábitos esta semana") }
                items(habits, key = { it.id }) { h ->
                    val weekStart = Streaks.weekStart(today)
                    val done = recent.count { it.challengeId == h.id && it.isDone && it.date >= weekStart }
                    val planned = Days.list(h.habitDays).size
                    LabeledProgress(done / planned.toFloat(), "${h.title} · $done de $planned", Modifier.padding(vertical = Space.xs))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** Barras simples con la cifra encima de cada una; TalkBack lee todas las cifras de corrido. */
@Composable
private fun WeeklyChart(weeks: List<Pair<kotlinx.datetime.LocalDate, Int>>) {
    val max = (weeks.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val description = "XP por semana: " + weeks.joinToString("; ") { (start, xp) -> "semana del ${Fmt.day(start)}, $xp XP" }
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weeks.forEach { (_, xp) ->
                Text("$xp", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val slot = size.width / weeks.size
            val w = slot * 0.6f
            weeks.forEachIndexed { i, (_, xp) ->
                val x = i * slot + (slot - w) / 2
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(8f))
                val h = size.height * xp / max
                if (h > 0) drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), CornerRadius(8f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weeks.forEach { (start, _) ->
                Text("${start.day}/${start.month.ordinal + 1}", style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeeksRow(weeks: List<Streaks.Week>) {
    if (weeks.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        weeks.forEach { w ->
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "Semana del ${Fmt.day(w.start)}: ${w.activeDays} de ${w.goal} días" + if (w.met) ", cumplida" else ""
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (w.met) "✓" else "·", style = MaterialTheme.typography.titleLarge,
                    color = if (w.met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Text("${w.activeDays}/${w.goal}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AchievementsScreen(vm: LevelUpViewModel, nav: Nav) {
    val p by vm.progress.collectAsStateWithLifecycle()
    val prefs = LocalPrefs.current
    var selected by remember { mutableStateOf<AchievementView?>(null) }
    val unlockedCount = p.achievements.count { it.state == AchievementState.UNLOCKED }

    ScreenScaffold("Logros y trofeos", onBack = nav::back) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding(), bottom = Space.xxl)) {
            item {
                Text("$unlockedCount de ${p.achievements.size} trofeos", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Space.s))
            }

            item { SectionTitle("Misiones") }
            val active = p.activeMission
            if (active != null) {
                item {
                    InfoCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("En marcha: ${active.first.title}", style = MaterialTheme.typography.titleSmall)
                        Text(active.first.description, style = MaterialTheme.typography.bodyMedium)
                        LabeledProgress(p.missionProgress / active.first.goal.target.toFloat(),
                            "${p.missionProgress} de ${active.first.goal.target} · hasta el ${Fmt.day(active.second.endsOn)}",
                            color = MaterialTheme.colorScheme.tertiary)
                        TextButton(onClick = { vm.abandonMission(active.second.id) }) { Text("Dejar la misión") }
                    }
                }
            } else {
                item {
                    Text("Opcionales y sin castigo: si una vence, puedes volver a empezarla.", style = MaterialTheme.typography.bodyMedium)
                }
                val chosen = prefs.categories
                val available = MissionCatalog.all.filter { m ->
                    val g = m.goal
                    g !is MissionGoal.InCategory || chosen.isEmpty() || g.category in chosen
                }
                items(available, key = { it.id }) { m ->
                    InfoCard(Modifier.padding(vertical = Space.xs)) {
                        Text(m.title, style = MaterialTheme.typography.titleSmall)
                        Text("${m.description} · +${m.xpReward} XP" + (Cosmetics.rewardOf(m.id)?.let { " · marco ${it.name}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall)
                        if (m.id in p.missionsCompleted) Text("Ya la completaste", style = MaterialTheme.typography.labelMedium)
                        SecondaryButton("Empezar", { vm.startMission(m.id) }, Modifier.padding(top = Space.s))
                    }
                }
            }

            val next = p.achievements.filter { it.state == AchievementState.AVAILABLE && it.progress > 0 && !it.achievement.hidden }
                .sortedByDescending { it.progress / it.achievement.target.toFloat() }.take(3)
            if (next.isNotEmpty()) {
                item { SectionTitle("Próximos hitos") }
                items(next, key = { "next-" + it.achievement.id }) { AchievementRow(it) { selected = it } }
            }

            AchievementCatalog.collections.forEach { collection ->
                item(key = "c-$collection") { SectionTitle(collection) }
                items(p.achievements.filter { it.achievement.collection == collection }, key = { it.achievement.id }) { v ->
                    AchievementRow(v) { selected = v }
                }
            }
            if (p.achievements.isEmpty()) item { EmptyState("Sin logros", "") }
        }
    }

    selected?.let { v ->
        val a = v.achievement
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } },
            dismissButton = {
                val on = v.unlockedOn
                if (v.state == AchievementState.UNLOCKED && on != null) {
                    TextButton(onClick = { vm.share(ShareCard(a.name, a.description, a.tier, Fmt.dayWithYear(on))) }) { Text("Compartir") }
                }
            },
            icon = { TrophyBadge(a.tier, v.state, 56.dp) },
            title = { Text(v.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    if (!v.secret) Text(a.description)
                    Text(stateLine(v))
                    val reward = listOfNotNull(
                        a.xpReward.takeIf { it > 0 }?.let { "+$it XP" },
                        Cosmetics.rewardOf(a.id)?.let { "marco ${it.name}" },
                    )
                    if (reward.isNotEmpty() && !v.secret) Text("Recompensa: ${reward.joinToString(" y ")}")
                }
            },
        )
    }
}

/** Secreto sin conseguir: ni su nombre ni cómo conseguirlo. */
private val AchievementView.secret get() = achievement.hidden && state != AchievementState.UNLOCKED
private val AchievementView.name get() = if (secret) "Logro secreto" else achievement.name

private fun stateLine(v: AchievementView): String = when {
    v.secret -> "Se descubre al conseguirlo. Sigue a tu ritmo."
    v.state == AchievementState.UNLOCKED -> "Desbloqueado el ${v.unlockedOn?.let { Fmt.dayWithYear(it) } ?: "—"}"
    v.state == AchievementState.LOCKED -> "Bloqueado: primero consigue «${AchievementCatalog.byId[v.achievement.requires]?.name}»"
    else -> "Disponible: ${v.progress} de ${v.achievement.target}"
}

@Composable
private fun AchievementRow(v: AchievementView, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClickLabel = "Ver detalle", onClick = onClick)
            .padding(vertical = Space.xs).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrophyBadge(v.achievement.tier, v.state)
        Spacer(Modifier.size(Space.m))
        Column(Modifier.weight(1f)) {
            Text(v.name, style = MaterialTheme.typography.titleSmall)
            Text(stateLine(v), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (v.state == AchievementState.AVAILABLE && !v.secret) {
                LabeledProgress(v.progress / v.achievement.target.toFloat(), "", Modifier.padding(top = Space.xs))
            }
        }
    }
}
