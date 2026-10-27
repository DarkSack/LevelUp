package app.levelup.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import app.levelup.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.levelup.content.Categories
import app.levelup.domain.CustomCategory
import app.levelup.domain.Days
import app.levelup.domain.DifficultyMode
import app.levelup.domain.GamificationStyle
import app.levelup.domain.MotionPref
import app.levelup.domain.NotificationLevel
import app.levelup.domain.Preferences
import app.levelup.domain.Reminders
import app.levelup.domain.ThemePref
import app.levelup.ui.Fmt
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.MultiChoiceRow
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.StepperRow
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.theme.LocalHasAccount
import app.levelup.ui.theme.Space
import kotlinx.datetime.DayOfWeek

/** Las mismas secciones sirven en el onboarding y en Perfil: cada cambio se guarda al momento. */
typealias PrefsChange = ((Preferences) -> Preferences) -> Unit

private val weekdayOptions = DayOfWeek.entries.map { it to Fmt.weekdayShort(it) }

@Composable
fun GoalsSection(prefs: Preferences, customCategories: List<CustomCategory>, onChange: PrefsChange) {
    val options = Categories.builtIn.map { it.key to it.name } + customCategories.filter { !it.archived }.map { it.id to it.name }
    Text("Elige una o varias. Si no eliges ninguna, verás retos de todas.", style = MaterialTheme.typography.bodyMedium)
    MultiChoiceRow(options, prefs.categories.toSet()) { key ->
        onChange { p ->
            val categories = if (key in p.categories) p.categories - key else p.categories + key
            p.copy(categories = categories, priorityCategories = p.priorityCategories.filter { it in categories })
        }
    }
    if (prefs.categories.isNotEmpty()) {
        SectionTitle("Prioridades (opcional, hasta 2)")
        Text("Tu reto principal saldrá de aquí.", style = MaterialTheme.typography.bodyMedium)
        MultiChoiceRow(options.filter { it.first in prefs.categories }, prefs.priorityCategories.toSet()) { key ->
            onChange { p ->
                val priority = when {
                    key in p.priorityCategories -> p.priorityCategories - key
                    p.priorityCategories.size >= 2 -> p.priorityCategories.drop(1) + key
                    else -> p.priorityCategories + key
                }
                p.copy(priorityCategories = priority)
            }
        }
    }
}

@Composable
fun RhythmSection(prefs: Preferences, onChange: PrefsChange) {
    ChoiceRow(
        "Dificultad", (1..5).map { it to "$it · ${Fmt.difficulty(it)}" }, prefs.difficulty,
        { d -> onChange { it.copy(difficulty = d, categoryDifficulty = emptyMap()) } },
        description = "Puedes empezar fácil: siempre habrá versiones más sencillas.",
    )
    ChoiceRow(
        "Cómo ajustar la dificultad",
        listOf(DifficultyMode.MANUAL to "Yo decido", DifficultyMode.SUGGEST to "Sugerirme cambios", DifficultyMode.AUTO to "Ajustarla sola"),
        prefs.difficultyMode, { m -> onChange { it.copy(difficultyMode = m) } },
        description = when (prefs.difficultyMode) {
            DifficultyMode.MANUAL -> "Nunca cambia sin que tú lo decidas."
            DifficultyMode.SUGGEST -> "Si un tipo de reto te sale fácil o difícil, te proponemos un paso arriba o abajo."
            DifficultyMode.AUTO -> "Sube o baja un paso como mucho por semana, siempre con calma."
        },
    )
    ChoiceRow(
        "Tiempo al día", listOf(5, 10, 15, 30, 45, 60).map { it to Fmt.minutes(it) }, prefs.minutesPerDay,
        { m -> onChange { it.copy(minutesPerDay = m) } },
        description = "Los retos de cada día caben en este tiempo.",
    )
    StepperRow(
        "Retos al día", "${prefs.dailyCount}",
        onMinus = if (prefs.dailyCount > 1) ({ onChange { it.copy(dailyCount = it.dailyCount - 1) } }) else null,
        onPlus = if (prefs.dailyCount < 5) ({ onChange { it.copy(dailyCount = it.dailyCount + 1) } }) else null,
    )
    Text("Días con retos", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Space.s))
    MultiChoiceRow(weekdayOptions, Days.list(prefs.challengeDays).toSet()) { day ->
        onChange { p ->
            val mask = p.challengeDays xor Days.bit(day)
            if (mask == 0) p else p.copy(challengeDays = mask) // siempre queda al menos un día
        }
    }
    StepperRow(
        "Objetivo semanal", Fmt.plural(prefs.weeklyGoalDays, "día", "días"),
        onMinus = if (prefs.weeklyGoalDays > 1) ({ onChange { it.copy(weeklyGoalDays = it.weeklyGoalDays - 1) } }) else null,
        onPlus = if (prefs.weeklyGoalDays < 7) ({ onChange { it.copy(weeklyGoalDays = it.weeklyGoalDays + 1) } }) else null,
        description = "Días por semana con al menos un reto. Perdona días sueltos.",
    )
}

@Composable
fun AccessibilitySection(prefs: Preferences, onChange: PrefsChange) {
    ChoiceRow(
        "Tamaño del texto",
        listOf(1f to "Normal", 1.15f to "Grande", 1.3f to "Más grande", 1.5f to "Muy grande"),
        prefs.textScale, { s -> onChange { it.copy(textScale = s) } },
        description = "Se suma al tamaño que tengas en el teléfono.",
    )
    ToggleRow("Modo sencillo", prefs.simpleMode, { v -> onChange { it.copy(simpleMode = v) } },
        "Menos cosas en pantalla: tu reto principal, tus retos de hoy y poco más.")
    ToggleRow("Baja estimulación", prefs.lowStimulation, { v -> onChange { it.copy(lowStimulation = v) } },
        "Colores apagados, sin animaciones ni ventanas de celebración.")
    ChoiceRow(
        "Movimiento", listOf(MotionPref.SYSTEM to "Como el teléfono", MotionPref.REDUCED to "Reducido", MotionPref.FULL to "Completo"),
        prefs.motion, { m -> onChange { it.copy(motion = m) } },
        description = "Reducido quita las animaciones decorativas.",
    )
    ChoiceRow(
        "Tema", listOf(ThemePref.SYSTEM to "Como el teléfono", ThemePref.LIGHT to "Claro", ThemePref.DARK to "Oscuro"),
        prefs.theme, { t -> onChange { it.copy(theme = t) } },
    )
    ToggleRow("Alto contraste", prefs.highContrast, { v -> onChange { it.copy(highContrast = v) } }, "Textos y bordes más marcados.")
    ToggleRow("Vibración", prefs.haptics, { v -> onChange { it.copy(haptics = v) } }, "Una vibración corta al completar un reto.")
    ToggleRow("Mostrar pasos", prefs.showSteps, { v -> onChange { it.copy(showSteps = v) } }, "Divide los retos en pasos pequeños que puedes marcar.")
    ToggleRow("Temporizador de enfoque", prefs.focusTimer, { v -> onChange { it.copy(focusTimer = v) } },
        "Un cronómetro opcional dentro de cada reto. Sin alarmas ni cuenta atrás.")
}

@Composable
fun RemindersSection(prefs: Preferences, notificationsAllowed: Boolean, onChange: PrefsChange, onToggle: (Boolean) -> Unit) {
    Text(
        "Un aviso al día, a la hora que elijas, solo si tienes retos pendientes. Nunca en tus horas de silencio ni en días de descanso.",
        style = MaterialTheme.typography.bodyMedium,
    )
    ToggleRow("Recordatorios", prefs.remindersEnabled && notificationsAllowed, onToggle,
        if (prefs.remindersEnabled && !notificationsAllowed) "El permiso de notificaciones está desactivado en el teléfono." else null)
    if (LocalHasAccount.current) ToggleRow("Avisos de amigos", prefs.socialNotifications, { v -> onChange { it.copy(socialNotifications = v) } },
        if (!notificationsAllowed) "Necesita el permiso de notificaciones del teléfono."
        else "Solicitudes, felicitaciones e invitaciones, si tienes cuenta. Respetan tus horas de silencio.")
    if (!prefs.remindersEnabled) return
    fun shift(minute: Int, delta: Int) = (minute + delta + 24 * 60) % (24 * 60)
    Text("Días con aviso", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Space.s))
    MultiChoiceRow(weekdayOptions, Days.list(prefs.reminderDays).toSet()) { day ->
        onChange { p ->
            val mask = p.reminderDays xor Days.bit(day)
            if (mask == 0) p else p.copy(reminderDays = mask) // para ningún día, mejor apagar los recordatorios
        }
    }
    StepperRow("Hora del aviso", Fmt.time(prefs.reminderMinute),
        onMinus = { onChange { it.copy(reminderMinute = shift(it.reminderMinute, -30)) } },
        onPlus = { onChange { it.copy(reminderMinute = shift(it.reminderMinute, 30)) } })
    StepperRow("Silencio desde", Fmt.time(prefs.quietStartMinute),
        onMinus = { onChange { it.copy(quietStartMinute = shift(it.quietStartMinute, -30)) } },
        onPlus = { onChange { it.copy(quietStartMinute = shift(it.quietStartMinute, 30)) } })
    StepperRow("Silencio hasta", Fmt.time(prefs.quietEndMinute),
        onMinus = { onChange { it.copy(quietEndMinute = shift(it.quietEndMinute, -30)) } },
        onPlus = { onChange { it.copy(quietEndMinute = shift(it.quietEndMinute, 30)) } })
    if (Reminders.inQuietHours(prefs.reminderMinute, prefs.quietStartMinute, prefs.quietEndMinute)) {
        InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Text("La hora del aviso cae en tus horas de silencio, así que no sonará. Cambia una de las dos.")
        }
    }
    ChoiceRow(
        "Intensidad",
        listOf(NotificationLevel.LOW to "Baja", NotificationLevel.NORMAL to "Normal"),
        if (prefs.notificationLevel == NotificationLevel.OFF) NotificationLevel.LOW else prefs.notificationLevel,
        { l -> onChange { it.copy(notificationLevel = l) } },
        description = if (prefs.notificationLevel == NotificationLevel.NORMAL) "El aviso lista hasta tres retos, hábitos incluidos." else "El aviso menciona solo tu primer reto pendiente.",
    )
    ToggleRow("Avisos para retomar", prefs.recoveryReminders, { v -> onChange { it.copy(recoveryReminders = v) } },
        "Si pasan 3 días sin actividad, un aviso amable como mucho cada 3 días.")
}

@Composable
fun RestSection(prefs: Preferences, paused: Boolean, onChange: PrefsChange, onPause: (Boolean) -> Unit) {
    Text("Días de descanso", style = MaterialTheme.typography.bodyLarge)
    Text("Esos días no hay retos ni avisos, y tu racha no se corta.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    MultiChoiceRow(weekdayOptions, Days.list(prefs.restDays).toSet()) { day ->
        onChange { p ->
            val mask = p.restDays xor Days.bit(day)
            if (mask == Days.ALL) p else p.copy(restDays = mask) // al menos un día sin descanso
        }
    }
    HorizontalDivider(Modifier.padding(vertical = Space.m))
    ToggleRow("Pausar mis objetivos", paused, onPause,
        "Para días difíciles o vacaciones. Mientras dure no hay retos ni avisos y no pierdes nada.")
}

@Composable
fun GameStyleSection(prefs: Preferences, onChange: PrefsChange) {
    ChoiceRow(
        null,
        listOf(GamificationStyle.FULL to "Completo", GamificationStyle.CALM to "Tranquilo", GamificationStyle.OFF to "Sin juego"),
        prefs.gamification, { g -> onChange { it.copy(gamification = g) } },
        description = when (prefs.gamification) {
            GamificationStyle.FULL -> "XP, niveles, rachas y celebraciones cuando desbloqueas algo."
            GamificationStyle.CALM -> "XP, niveles y logros, sin rachas ni ventanas de celebración."
            GamificationStyle.OFF -> "Sin XP, niveles, rachas ni animaciones: solo tus retos, hábitos y metas."
        },
    )
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = Space.l)) {
        SectionTitle(title)
        content()
    }
}
