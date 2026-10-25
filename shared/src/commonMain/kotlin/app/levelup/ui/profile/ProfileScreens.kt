package app.levelup.ui.profile

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.Platform
import app.levelup.content.AchievementCatalog
import app.levelup.content.Cosmetics
import app.levelup.content.MissionCatalog
import app.levelup.domain.DayKind
import app.levelup.domain.XpRules
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.Avatar
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.settings.AccessibilitySection
import app.levelup.ui.settings.GameStyleSection
import app.levelup.ui.settings.GoalsSection
import app.levelup.ui.settings.RemindersSection
import app.levelup.ui.settings.RestSection
import app.levelup.ui.settings.RhythmSection
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space

private data class Entry(val section: String, val title: String, val subtitle: String)

private val entries = listOf(
    Entry("goals", "Objetivos", "Categorías y prioridades"),
    Entry("rhythm", "Ritmo y dificultad", "Tiempo, retos al día y objetivo semanal"),
    Entry("rest", "Descanso y pausas", "Días libres y pausar objetivos"),
    Entry("accessibility", "Accesibilidad", "Texto, movimiento, contraste y modo sencillo"),
    Entry("reminders", "Recordatorios", "Hora, silencio e intensidad"),
    Entry("style", "Estilo de juego", "Completo o tranquilo"),
    Entry("frames", "Marco del avatar", "Recompensas de logros y misiones"),
    Entry("data", "Tus datos", "Exportar, importar o borrar"),
    Entry("privacy", "Privacidad", "Qué se guarda y dónde"),
    Entry("about", "Acerca de LevelUp", "Versión, licencias y tutorial"),
)

@Composable
fun ProfileScreen(vm: LevelUpViewModel, nav: Nav, tabBar: @Composable () -> Unit) {
    val prefs = LocalPrefs.current
    val p by vm.progress.collectAsStateWithLifecycle()
    var editName by remember { mutableStateOf(false) }
    val frame = Cosmetics.byId[prefs.equippedFrame] ?: Cosmetics.byId.getValue(Cosmetics.DEFAULT)

    ScreenScaffold("Perfil", bottomBar = tabBar) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + Space.xl)) {
            item {
                Row(Modifier.padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(prefs.displayName, frame, p.level.level, 72.dp)
                    Spacer(Modifier.size(Space.l))
                    Column(Modifier.weight(1f)) {
                        Text(prefs.displayName.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleLarge)
                        Text(XpRules.titleFor(p.level.level), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { editName = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar nombre") }
                }
            }
            items(entries, key = { it.section }) { e ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { nav.settings(e.section) }.padding(vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.bodyLarge)
                        Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
                HorizontalDivider()
            }
        }
    }

    if (editName) {
        var name by remember { mutableStateOf(prefs.displayName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Tu nombre") },
            text = {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Cómo te llamamos (opcional)") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updatePrefs { it.copy(displayName = name.trim()) }
                    editName = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun SettingsScreen(vm: LevelUpViewModel, nav: Nav, platform: Platform, section: String) {
    val prefs = LocalPrefs.current
    val title = entries.firstOrNull { it.section == section }?.title ?: "Ajustes"
    val onChange: ((app.levelup.domain.Preferences) -> app.levelup.domain.Preferences) -> Unit = { vm.updatePrefs(it) }
    ScreenScaffold(title, onBack = nav::back) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            when (section) {
                "goals" -> {
                    val custom by vm.customCategories.collectAsStateWithLifecycle()
                    GoalsSection(prefs, custom, onChange)
                }
                "rhythm" -> RhythmSection(prefs, onChange)
                "rest" -> {
                    val p by vm.progress.collectAsStateWithLifecycle()
                    RestSection(prefs, p.dayKind == DayKind.PAUSED, onChange, vm::setPaused)
                }
                "accessibility" -> {
                    AccessibilitySection(prefs, onChange)
                    InfoCard(Modifier.padding(top = Space.m)) {
                        Text("Vista previa", style = MaterialTheme.typography.titleSmall)
                        Text("Así se ve el texto con tus ajustes actuales.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                "reminders" -> RemindersSection(prefs, platform.notificationsAllowed(), onChange, vm::setReminders)
                "style" -> GameStyleSection(prefs, onChange)
                "frames" -> FramesSection(vm)
                "data" -> DataSection(vm)
                "privacy" -> PrivacySection()
                "about" -> AboutSection(vm, nav, platform)
            }
        }
    }
}

@Composable
private fun FramesSection(vm: LevelUpViewModel) {
    val prefs = LocalPrefs.current
    val p by vm.progress.collectAsStateWithLifecycle()
    val unlocked = p.frames.map { it.id }.toSet()
    Text("Los marcos son decorativos: no dan ventajas.", style = MaterialTheme.typography.bodyMedium)
    Cosmetics.frames.forEach { f ->
        val available = f.id in unlocked
        val source = f.unlockedBy?.let { AchievementCatalog.byId[it]?.name ?: MissionCatalog.byId[it]?.title }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .selectable(prefs.equippedFrame == f.id, enabled = available, role = Role.RadioButton) {
                    vm.updatePrefs { it.copy(equippedFrame = f.id) }
                }
                .padding(vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(prefs.displayName, f, null, 48.dp)
            Spacer(Modifier.size(Space.m))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        f.unlockedBy == null -> "Disponible desde el inicio"
                        available -> "Conseguido con «$source»"
                        else -> "Se consigue con «$source»"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prefs.equippedFrame == f.id) Icon(Icons.Filled.Check, contentDescription = "En uso")
        }
    }
}

@Composable
private fun DataSection(vm: LevelUpViewModel) {
    var confirmImport by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    SectionTitle("Copia de seguridad")
    Text(
        "Guarda un archivo con todo tu progreso (formato JSON, legible). Con él puedes recuperar tus datos en otro teléfono.",
        style = MaterialTheme.typography.bodyMedium,
    )
    PrimaryButton("Exportar mis datos", vm::exportData, Modifier.padding(top = Space.s))
    SecondaryButton("Importar un respaldo", { confirmImport = true })
    Text(
        "Además, si tienes activada la copia de seguridad de Android, el sistema guarda tus datos y los restaura al instalar LevelUp en un teléfono nuevo.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionTitle("Borrar")
    Text("Borra todo lo que LevelUp guarda en este teléfono. No se puede deshacer.", style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Borrar todos mis datos", color = MaterialTheme.colorScheme.error)
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("¿Importar un respaldo?") },
            text = { Text("Tus datos actuales se reemplazarán por los del archivo. Si el archivo no es válido, no se cambia nada.") },
            confirmButton = { TextButton(onClick = { confirmImport = false; vm.importData() }) { Text("Elegir archivo") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancelar") } },
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("¿Borrar todos tus datos?") },
            text = { Text("Se borran tus retos, tu XP, tus logros y tus ajustes. Si quieres conservarlos, exporta antes un respaldo.") },
            confirmButton = { TextButton(onClick = { confirmWipe = false; vm.wipe() }) { Text("Borrar todo", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PrivacySection() {
    listOf(
        "Qué se guarda" to "Tus ajustes, tus retos (los del catálogo que te tocan y los que creas), la experiencia, los logros y las misiones.",
        "Dónde" to "Solo en este teléfono. LevelUp no tiene cuentas ni servidores: no envía tus datos a ningún sitio.",
        "Copia de Android" to "Si tienes activada la copia de seguridad de Android, Google guarda una copia cifrada de los datos de tus apps en tu cuenta, LevelUp incluida. Puedes desactivarla en los ajustes del teléfono.",
        "Anuncios y analítica" to "No hay anuncios, ni analítica, ni seguimiento.",
        "Permisos" to "Solo notificaciones, y solo si activas los recordatorios.",
        "Tus derechos" to "Puedes exportar tus datos o borrarlos por completo en Perfil → Tus datos.",
        "Salud" to "LevelUp no es un servicio médico ni hace diagnósticos. Adapta los retos a tu situación.",
    ).forEach { (title, body) ->
        SectionTitle(title)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AboutSection(vm: LevelUpViewModel, nav: Nav, platform: Platform) {
    Text("LevelUp ${platform.appVersion}", style = MaterialTheme.typography.titleMedium)
    Text("Hábitos con retos pequeños y progreso a tu ritmo.", style = MaterialTheme.typography.bodyMedium)
    SectionTitle("Volver a ver")
    SecondaryButton("La bienvenida", nav::welcome)
    SecondaryButton("El tutorial completo", vm::replayTutorial)
    SectionTitle("Licencias de código abierto")
    Text(
        "Kotlin, Kotlin Multiplatform, Jetpack Compose, Compose Multiplatform, Room, AndroidX, WorkManager, " +
            "kotlinx.coroutines, kotlinx.serialization y kotlinx-datetime: Apache License 2.0.",
        style = MaterialTheme.typography.bodyMedium,
    )
}
