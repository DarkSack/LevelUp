package app.levelup.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Categories
import app.levelup.domain.Challenge
import app.levelup.domain.Preferences
import app.levelup.domain.XpRules
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.settings.AccessibilitySection
import app.levelup.ui.settings.GoalsSection
import app.levelup.ui.settings.RemindersSection
import app.levelup.ui.settings.RhythmSection
import app.levelup.ui.theme.Space

const val LAST_STEP = 6

/**
 * Pasos 1–6 tras la bienvenida (paso 0). El paso actual se guarda en Room, así que cerrar la app a mitad
 * vuelve al mismo sitio. Todo se puede omitir, retroceder y cambiar después en Perfil.
 */
@Composable
fun OnboardingFlow(vm: LevelUpViewModel, prefs: Preferences) {
    val step = prefs.onboardingStep.coerceIn(1, LAST_STEP)
    fun go(to: Int) = vm.updatePrefs { it.copy(onboardingStep = to) }
    val onChange: ((Preferences) -> Preferences) -> Unit = { vm.updatePrefs(it) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go(step - 1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Paso anterior")
            }
            Text("Paso $step de $LAST_STEP", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (step < LAST_STEP) {
                TextButton(onClick = { go(step + 1) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Omitir") }
            }
        }
        LinearProgressIndicator(progress = { step / LAST_STEP.toFloat() }, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.l))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            when (step) {
                1 -> ValueStep()
                2 -> {
                    StepTitle("¿En qué quieres avanzar?")
                    val custom by vm.customCategories.collectAsStateWithLifecycle()
                    GoalsSection(prefs, custom, onChange)
                }
                3 -> {
                    StepTitle("Hazla cómoda para ti")
                    Text("Puedes cambiar esto en cualquier momento desde Perfil.", style = MaterialTheme.typography.bodyMedium)
                    AccessibilitySection(prefs, onChange)
                }
                4 -> {
                    StepTitle("Tu ritmo")
                    RhythmSection(prefs, onChange)
                }
                5 -> {
                    StepTitle("Recordatorios (opcional)")
                    RemindersSection(prefs, notificationsAllowed = true, onChange = onChange, onToggle = vm::setReminders)
                }
                else -> ReadyStep(vm, prefs)
            }
        }
        Column(Modifier.padding(Space.l)) {
            if (step < LAST_STEP) {
                PrimaryButton("Continuar", { go(step + 1) })
            } else {
                PrimaryButton("Empezar", vm::finishOnboarding)
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }.padding(bottom = Space.s))
}

@Composable
private fun ValueStep() {
    StepTitle("Así funciona LevelUp")
    listOf(
        "Retos pequeños y concretos, elegidos según tus objetivos y tu tiempo.",
        "Cada reto te da experiencia (XP) y subes de nivel. Lo ganado nunca se pierde.",
        "¿Un día difícil? Simplifica el reto, cámbialo o descansa. Sin castigos.",
        "Funciona sin internet y sin cuenta: tus datos se quedan en tu teléfono.",
    ).forEach { line ->
        Row(Modifier.padding(vertical = Space.s), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(Space.m))
            Text(line, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ReadyStep(vm: LevelUpViewModel, prefs: Preferences) {
    var name by remember { mutableStateOf(prefs.displayName) }
    val preview by produceState<List<Challenge>?>(null, prefs.categories, prefs.difficulty, prefs.minutesPerDay, prefs.dailyCount, prefs.challengeDays, prefs.restDays) {
        value = vm.previewToday()
    }
    StepTitle("Todo listo")
    OutlinedTextField(
        value = name,
        onValueChange = { value ->
            name = value.take(30)
            vm.updatePrefs { it.copy(displayName = name.trim()) }
        },
        label = { Text("¿Cómo te llamamos? (opcional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Tus primeros retos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Space.m).semantics { heading() })
    val list = preview
    when {
        list == null -> Text("Preparando…")
        list.isEmpty() -> Text("Hoy es día de descanso según lo que elegiste. Mañana empiezan tus retos.")
        else -> list.forEach { c ->
            InfoCard {
                Text(c.title, style = MaterialTheme.typography.titleSmall)
                Text("${Categories.name(c.category)} · ${Fmt.minutes(c.minutes)} · +${c.xp} XP", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text("Experiencia y niveles", style = MaterialTheme.typography.titleSmall)
        Text(
            "Cada reto da entre ${XpRules.xpFor(1)} y ${XpRules.xpFor(5)} XP según su dificultad. Con " +
                "${XpRules.totalForLevel(2)} XP llegas al nivel 2. Cada categoría tiene además su propio nivel.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    InfoCard {
        Text("Sin cuenta", style = MaterialTheme.typography.titleSmall)
        Text(
            "Usas LevelUp sin registrarte. Todo se guarda en este teléfono; puedes exportar tus datos desde Perfil " +
                "para pasarlos a otro.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
