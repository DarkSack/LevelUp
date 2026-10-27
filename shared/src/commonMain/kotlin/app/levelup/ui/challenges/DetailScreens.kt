package app.levelup.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import app.levelup.ui.components.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.HealthToday
import app.levelup.content.Categories
import app.levelup.content.Lab
import app.levelup.domain.Challenge
import app.levelup.domain.DayBlock
import app.levelup.domain.InstanceStatus
import app.levelup.domain.NOTE_MAX
import app.levelup.domain.Rating
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.CategoryLabel
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.DifficultyLabel
import app.levelup.ui.components.EmptyState
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space
import app.levelup.ui.theme.gameOn
import kotlinx.coroutines.delay
import app.levelup.i18n.tr

/** Detalle de un reto concreto de hoy: pasos, temporizador opcional y acciones sin penalización. */
@Composable
fun InstanceDetailScreen(vm: LevelUpViewModel, nav: Nav, instanceId: String) {
    val prefs = LocalPrefs.current
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val inst = (today + recent).firstOrNull { it.id == instanceId }
    val definition by produceState<Challenge?>(null, inst?.challengeId) { value = inst?.let { vm.challenge(it.challengeId) } }
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    ScreenScaffold(
        inst?.title ?: "Reto", onBack = nav::back,
        actions = { if (inst != null) FavoriteButton(inst.challengeId in favorites) { vm.toggleFavorite(inst.challengeId) } },
    ) { padding ->
        if (inst == null) {
            // Llega aquí desde un enlace viejo (el widget de ayer, un atajo). Las listas tardan un instante en cargar.
            val missing by produceState(false) { delay(MISSING_DELAY_MS); value = true }
            if (missing) {
                EmptyState("Este reto ya no está", "Era de otro día o ya no está en tu lista. Tus retos de hoy están en Inicio.",
                    Modifier.padding(padding)) { PrimaryButton("Ir a Inicio", { nav.open("hoy") }) }
            }
            return@ScreenScaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryLabel(inst.category, custom)
                Spacer(Modifier.size(Space.m))
                Text(Fmt.minutes(inst.minutes) + if (prefs.gameOn) tr(" · +{0} XP", inst.xp) else "", style = MaterialTheme.typography.labelMedium)
            }
            DifficultyLabel(inst.difficulty)
            Text(tr("Estado: {0}", statusText(inst)) + if (inst.minimal) tr(" (versión mínima)") else "",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (inst.isHabit) Text("Es uno de tus hábitos.", style = MaterialTheme.typography.bodySmall)

            val d = definition
            if (d != null) ChallengeInfo(d) else Text("Este reto ya no existe en tu lista, pero queda en tu historial.")

            if (Lab.HEALTH in prefs.labFlags && inst.category == Categories.FISICO) {
                HealthCard(vm, inst.minutes, if (inst.isOpen) ({ vm.complete(inst.id) }) else null)
            }

            if (d != null && prefs.showSteps && d.steps.isNotEmpty()) {
                SectionTitle("Pasos")
                d.steps.forEachIndexed { index, step ->
                    val checked = inst.stepsDone and (1 shl index) != 0
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(checked, enabled = inst.isOpen, role = Role.Checkbox) { vm.toggleStep(inst.id, index) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.size(Space.s))
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (prefs.focusTimer && inst.isOpen) FocusTimer(inst.minutes)

            if (inst.isOpen || inst.date == vm.today.value) {
                ChoiceRow(
                    "Momento del día", listOf(null to "Cualquiera") + DayBlock.entries.map { it to Fmt.block(it) }, inst.block,
                    { vm.setBlock(inst.id, it) },
                )
            }
            NoteField(inst.id, inst.note, vm::setNote)

            Spacer(Modifier.size(Space.m))
            when {
                inst.isOpen -> {
                    PrimaryButton("Completar", { vm.complete(inst.id) })
                    if (d != null && d.minimum.isNotBlank()) SecondaryButton("Hice la versión mínima", { vm.completeMinimal(inst.id) })
                    SecondaryButton("Modo concentración", { nav.focus(inst.id) })
                    if (inst.status == InstanceStatus.STARTED) {
                        SecondaryButton("Pausar", { vm.pause(inst.id) })
                    } else {
                        SecondaryButton(if (inst.status == InstanceStatus.PAUSED) "Seguir" else "Empezar", { vm.start(inst.id) })
                    }
                    if (d?.simplerId != null) SecondaryButton("Hacerlo más sencillo", { vm.simplify(inst.id) })
                    SecondaryButton("Cambiar por otro", {
                        vm.replace(inst.id)
                        nav.back()
                    })
                    TextButton(onClick = { vm.skip(inst.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Dejar para otro día")
                    }
                }
                inst.isDone -> {
                    SectionTitle("¿Cómo te resultó?")
                    ChoiceRow(
                        null,
                        listOf(Rating.EASY to "Fácil", Rating.RIGHT to "Justo", Rating.HARD to "Difícil"),
                        inst.rating,
                        { rating -> rating?.let { vm.rate(inst.id, it) } },
                        description = "Nos ayuda a ajustar la dificultad. Es opcional.",
                    )
                    TextButton(onClick = { vm.undo(inst.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Marcar como no hecho")
                    }
                }
                inst.status == InstanceStatus.SKIPPED -> {
                    Text("Lo dejaste para otro día. Si cambias de idea, aún puedes completarlo.")
                    PrimaryButton("Completar", { vm.complete(inst.id) })
                }
            }
        }
    }
}

/** Detalle de un reto del catálogo: lo mismo que un reto del día, con "Añadir a hoy". */
@Composable
fun CatalogDetailScreen(vm: LevelUpViewModel, nav: Nav, challengeId: String) {
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val c by produceState<Challenge?>(null, challengeId) { value = vm.challenge(challengeId) }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val gameOn = LocalPrefs.current.gameOn
    ScreenScaffold(
        c?.title ?: "Reto", onBack = nav::back,
        actions = { FavoriteButton(challengeId in favorites) { vm.toggleFavorite(challengeId) } },
    ) { padding ->
        val challenge = c ?: return@ScreenScaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryLabel(challenge.category, custom)
                Spacer(Modifier.size(Space.m))
                Text(Fmt.minutes(challenge.minutes) + if (gameOn) tr(" · +{0} XP", challenge.xp) else "", style = MaterialTheme.typography.labelMedium)
            }
            DifficultyLabel(challenge.difficulty)
            ChallengeInfo(challenge)
            if (challenge.steps.isNotEmpty()) {
                SectionTitle("Pasos")
                challenge.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodyLarge) }
            }
            Spacer(Modifier.size(Space.m))
            val existing = today.firstOrNull { it.challengeId == challengeId && it.status != InstanceStatus.REPLACED }
            if (existing != null && (existing.isOpen || existing.isDone)) {
                Text(if (existing.isDone) "Ya lo completaste hoy." else "Ya está en tus retos de hoy.")
                SecondaryButton("Ir al reto", { nav.instance(existing.id) })
            } else {
                PrimaryButton("Añadir a hoy", { vm.addToToday(challengeId) })
            }
        }
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = tr(if (favorite) "Quitar de favoritos" else "Guardar en favoritos"),
        )
    }
}

/** Nota libre del día sobre el reto. Se busca desde Inicio → Buscar. */
@Composable
private fun NoteField(id: String, saved: String?, onSave: (String, String) -> Unit) {
    var text by rememberSaveable(id, saved) { mutableStateOf(saved.orEmpty()) }
    SectionTitle("Nota (opcional)")
    OutlinedTextField(
        text, { text = it.take(NOTE_MAX) }, Modifier.fillMaxWidth(),
        placeholder = { Text("Cómo te fue, qué te ayudó…") }, minLines = 2,
        supportingText = { Text("${text.length}/$NOTE_MAX") },
    )
    if (text.trim() != saved.orEmpty()) {
        TextButton(onClick = { onSave(id, text) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Guardar nota") }
    }
}

/** Actividad de hoy leída de Health Connect. Solo se muestra; nada se guarda ni se sube. */
@Composable
private fun HealthCard(vm: LevelUpViewModel, minutes: Int, onComplete: (() -> Unit)?) {
    val health = vm.health ?: return
    val data by produceState<HealthToday?>(null) { value = runCatching { health.today() }.getOrNull() }
    val d = data ?: return
    InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text("Tu actividad de hoy", style = MaterialTheme.typography.titleSmall)
        Text(tr("{0} pasos · {1} min de ejercicio (Health Connect)", d.steps, d.exerciseMinutes), style = MaterialTheme.typography.bodyMedium)
        if (onComplete != null && d.exerciseMinutes >= minutes) {
            Text("Parece que ya lo hiciste.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onComplete, modifier = Modifier.heightIn(min = 48.dp)) { Text("Marcarlo como hecho") }
        }
    }
}

@Composable
private fun ChallengeInfo(c: Challenge) {
    Text(c.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Space.s))
    SectionTitle("Qué hacer")
    Text(c.goal, style = MaterialTheme.typography.bodyLarge)
    if (c.minimum.isNotBlank()) {
        SectionTitle("Versión mínima")
        Text(tr("Para días difíciles: {0}", c.minimum), style = MaterialTheme.typography.bodyLarge)
    }
    if (c.doneWhen.isNotBlank()) {
        SectionTitle("Está hecho cuando")
        Text(c.doneWhen, style = MaterialTheme.typography.bodyLarge)
    }
    c.safety?.let {
        InfoCard(Modifier.padding(top = Space.s), container = MaterialTheme.colorScheme.secondaryContainer) {
            Text("Ten en cuenta", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Cronómetro opcional: cuenta hacia arriba y no suena. Llegar a la duración estimada no obliga a nada. */
@Composable
fun FocusTimer(targetMinutes: Int) {
    var running by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            seconds++
        }
    }
    InfoCard {
        Text("Temporizador de enfoque", style = MaterialTheme.typography.titleSmall)
        val mm = (seconds / 60).toString().padStart(2, '0')
        val ss = (seconds % 60).toString().padStart(2, '0')
        Text("$mm:$ss", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { contentDescription = tr("{0} minutos y {1} segundos", seconds / 60, seconds % 60) })
        Text(tr("Duración estimada: {0}", Fmt.minutes(targetMinutes)), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            OutlinedButton(onClick = { running = !running }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (running) "Pausar" else if (seconds == 0) "Empezar" else "Seguir")
            }
            if (seconds > 0) {
                TextButton(onClick = { running = false; seconds = 0 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reiniciar") }
            }
        }
    }
}

/** Margen para que carguen los retos antes de decir que uno ya no está. */
private const val MISSING_DELAY_MS = 600L
