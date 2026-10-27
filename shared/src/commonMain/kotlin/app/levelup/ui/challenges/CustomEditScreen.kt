package app.levelup.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Categories
import app.levelup.content.Tags
import app.levelup.domain.DayBlock
import app.levelup.domain.CustomChallenge
import app.levelup.domain.CustomChallengeDraft
import app.levelup.domain.CustomChallengeRules
import app.levelup.domain.CustomStatus
import app.levelup.domain.Days
import app.levelup.domain.DraftField
import app.levelup.domain.Frequency
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.MultiChoiceRow
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.StepperRow
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.theme.Space
import kotlinx.datetime.DayOfWeek

private enum class Repeat(val label: String) { WHEN_FITS("Cuando toque"), WEEKLY("Una vez por semana"), ONCE("Una sola vez"), HABIT("Hábito: días fijos") }

/** Crear o editar un reto propio. Las reglas de validación son las del dominio (CustomChallengeRules). */
@Composable
fun CustomEditScreen(vm: LevelUpViewModel, nav: Nav, id: String?) {
    val categories by vm.customCategories.collectAsStateWithLifecycle()
    var loaded by remember { mutableStateOf(id == null) }
    var existing by remember { mutableStateOf<CustomChallenge?>(null) }
    var draft by remember { mutableStateOf(CustomChallengeDraft(category = Categories.BIENESTAR)) }
    var repeat by remember { mutableStateOf(Repeat.WHEN_FITS) }
    var errors by remember { mutableStateOf<Map<DraftField, String>>(emptyMap()) }
    var newCategoryOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) {
            vm.customChallenge(id)?.let { c ->
                existing = c
                draft = CustomChallengeDraft(
                    c.title, c.description, c.category, c.goal, c.doneWhen, c.difficulty, c.minutes, c.frequency, c.habitDays, c.steps,
                    c.tags, c.minimum, c.block, c.reminderMinute,
                )
                repeat = when {
                    c.habitDays != 0 -> Repeat.HABIT
                    c.frequency == Frequency.WEEKLY -> Repeat.WEEKLY
                    c.frequency == Frequency.ONCE -> Repeat.ONCE
                    else -> Repeat.WHEN_FITS
                }
            }
            loaded = true
        }
    }

    ScreenScaffold(if (id == null) "Nuevo reto" else "Editar reto", onBack = nav::back) { padding ->
        if (!loaded) return@ScreenScaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Field("Título", draft.title, errors[DraftField.TITLE], CustomChallengeRules.TITLE_MAX) { draft = draft.copy(title = it) }
            Field("Qué hay que hacer", draft.goal, errors[DraftField.GOAL], CustomChallengeRules.GOAL_MAX,
                hint = "Algo medible: «Leer 10 páginas»") { draft = draft.copy(goal = it) }
            Field("Está hecho cuando (opcional)", draft.doneWhen, errors[DraftField.DONE_WHEN], CustomChallengeRules.GOAL_MAX) {
                draft = draft.copy(doneWhen = it)
            }
            Field("Versión mínima (opcional)", draft.minimum, errors[DraftField.MINIMUM], CustomChallengeRules.GOAL_MAX,
                hint = "Para días difíciles: «Leer una página»") { draft = draft.copy(minimum = it) }
            Field("Descripción (opcional)", draft.description, errors[DraftField.DESCRIPTION], CustomChallengeRules.TEXT_MAX, singleLine = false) {
                draft = draft.copy(description = it)
            }

            ChoiceRow(
                "Categoría",
                Categories.builtIn.map { it.key to it.name } + categories.filter { !it.archived }.map { it.id to it.name },
                draft.category, { draft = draft.copy(category = it) },
            )
            errors[DraftField.CATEGORY]?.let { ErrorText(it) }
            TextButton(onClick = { newCategoryOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Nueva categoría") }

            ChoiceRow("Dificultad", (1..5).map { it to Fmt.difficulty(it) }, draft.difficulty, { draft = draft.copy(difficulty = it) })
            StepperRow(
                "Duración", Fmt.minutes(draft.minutes),
                onMinus = if (draft.minutes > 1) ({ draft = draft.copy(minutes = (draft.minutes - if (draft.minutes > 5) 5 else 1).coerceAtLeast(1)) }) else null,
                onPlus = if (draft.minutes < 240) ({ draft = draft.copy(minutes = (draft.minutes + if (draft.minutes >= 5) 5 else 1).coerceAtMost(240)) }) else null,
            )
            errors[DraftField.MINUTES]?.let { ErrorText(it) }

            ChoiceRow("¿Cada cuánto?", Repeat.entries.map { it to it.label }, repeat, {
                repeat = it
                draft = draft.copy(
                    frequency = when (it) { Repeat.WEEKLY -> Frequency.WEEKLY; Repeat.ONCE -> Frequency.ONCE; else -> Frequency.DAILY },
                    habitDays = if (it == Repeat.HABIT) draft.habitDays.takeIf { d -> d != 0 } ?: Days.of(DayOfWeek.MONDAY) else 0,
                )
            }, description = when (repeat) {
                Repeat.WHEN_FITS -> "Puede salir en tu selección de cualquier día."
                Repeat.WEEKLY -> "Como mucho una vez por semana."
                Repeat.ONCE -> "Desaparece de la selección cuando lo completes."
                Repeat.HABIT -> "Aparece siempre los días que elijas, además de tus retos del día."
            })
            if (repeat == Repeat.HABIT) {
                MultiChoiceRow(DayOfWeek.entries.map { it to Fmt.weekdayShort(it) }, Days.list(draft.habitDays).toSet()) { day ->
                    val mask = draft.habitDays xor Days.bit(day)
                    if (mask != 0) draft = draft.copy(habitDays = mask)
                }
                val at = draft.reminderMinute
                ToggleRow("Aviso propio", at != null, { on ->
                    draft = draft.copy(reminderMinute = if (on) 9 * 60 else null)
                    if (on) vm.askNotifications()
                }, description = "Un aviso los días de este hábito, si aún no lo hiciste. Respeta tus horas de silencio.")
                if (at != null) {
                    StepperRow("Hora del aviso", Fmt.time(at),
                        onMinus = { draft = draft.copy(reminderMinute = (at - 15 + 24 * 60) % (24 * 60)) },
                        onPlus = { draft = draft.copy(reminderMinute = (at + 15) % (24 * 60)) })
                }
            }

            SectionTitle("Dónde y cuándo (opcional)")
            MultiChoiceRow(Tags.places + (Tags.SIN_MATERIALES to "Sin materiales"), draft.tags.toSet()) { tag ->
                draft = draft.copy(tags = if (tag in draft.tags) draft.tags - tag else draft.tags + tag)
            }
            ChoiceRow(
                "Momento del día", listOf(null to "Cualquiera") + DayBlock.entries.map { it to Fmt.block(it) }, draft.block,
                { draft = draft.copy(block = it) }, description = "En Inicio, tus retos se agrupan por mañana, tarde y noche.",
            )

            SectionTitle("Pasos (opcional)")
            draft.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        step,
                        { value -> draft = draft.copy(steps = draft.steps.toMutableList().also { it[index] = value.take(CustomChallengeRules.STEP_MAX) }) },
                        label = { Text("Paso ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { draft = draft.copy(steps = draft.steps.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar paso ${index + 1}")
                    }
                }
            }
            errors[DraftField.STEPS]?.let { ErrorText(it) }
            if (draft.steps.size < CustomChallengeRules.MAX_STEPS) {
                TextButton(onClick = { draft = draft.copy(steps = draft.steps + "") }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Añadir paso")
                }
            }

            PrimaryButton("Guardar", {
                vm.saveCustom(draft, id) { result ->
                    errors = result
                    if (result.isEmpty()) nav.back()
                }
            }, Modifier.padding(top = Space.m))

            existing?.let { c ->
                SectionTitle("Más opciones")
                when (c.status) {
                    CustomStatus.ACTIVE -> SecondaryButton("Pausar", { vm.setCustomStatus(c.id, CustomStatus.PAUSED); nav.back() })
                    CustomStatus.PAUSED -> SecondaryButton("Reanudar", { vm.setCustomStatus(c.id, CustomStatus.ACTIVE); nav.back() })
                    CustomStatus.ARCHIVED -> SecondaryButton("Restaurar", { vm.setCustomStatus(c.id, CustomStatus.ACTIVE); nav.back() })
                }
                if (c.status != CustomStatus.ARCHIVED) {
                    SecondaryButton("Archivar", { vm.setCustomStatus(c.id, CustomStatus.ARCHIVED); nav.back() })
                }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (newCategoryOpen) {
        NewCategoryDialog(vm, onDone = { newId ->
            newCategoryOpen = false
            if (newId != null) draft = draft.copy(category = newId)
        })
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este reto?") },
            text = { Text("Lo que ya completaste con él se queda en tu historial y tu XP no cambia.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteCustom(existing!!.id)
                    nav.back()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    max: Int,
    hint: String? = null,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(max)) },
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        isError = error != null,
        supportingText = { Text(error ?: "${value.length}/$max") },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun NewCategoryDialog(vm: LevelUpViewModel, onDone: (String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Nueva categoría") },
        text = {
            OutlinedTextField(
                name, { name = it.take(30) }, label = { Text("Nombre") }, singleLine = true,
                isError = error != null, supportingText = error?.let { { Text(it) } },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                vm.addCustomCategory(name) { result -> result.fold({ onDone(it) }, { error = it.message }) }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancelar") } },
    )
}
