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
import app.levelup.ui.components.Text
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
import app.levelup.content.Avatars
import app.levelup.content.Cosmetics
import app.levelup.content.MissionCatalog
import app.levelup.domain.DayKind
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import app.levelup.domain.XpRules
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.Avatar
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.extra.LabSection
import app.levelup.ui.theme.gameOn
import app.levelup.ui.settings.AccessibilitySection
import app.levelup.ui.settings.GameStyleSection
import app.levelup.ui.settings.GoalsSection
import app.levelup.ui.settings.RemindersSection
import app.levelup.ui.settings.RestSection
import app.levelup.ui.settings.RhythmSection
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.LocalHasAccount
import app.levelup.ui.theme.Space
import app.levelup.i18n.tr

private data class Entry(val section: String, val title: String, val subtitle: String)

private val entries = listOf(
    Entry("account", "Cuenta y sincronización", "Opcional: guarda tu progreso en la nube"),
    Entry("social", "Amigos y grupos", "Felicitaciones, retos en grupo y clasificación"),
    Entry("plus", "LevelUp Plus", "Colores, marcos y estadísticas ampliadas"),
    Entry("metas", "Mis metas", "Lo que te importa, con o sin fecha"),
    Entry("review", "Revisión semanal", "Cómo fue la semana y qué ajustar"),
    Entry("goals", "Objetivos", "Categorías y prioridades"),
    Entry("rhythm", "Ritmo y dificultad", "Tiempo, retos al día y objetivo semanal"),
    Entry("rest", "Descanso y pausas", "Días libres y pausar objetivos"),
    Entry("accessibility", "Accesibilidad", "Texto, movimiento, contraste y modo sencillo"),
    Entry("reminders", "Recordatorios", "Hora, silencio e intensidad"),
    Entry("style", "Estilo de juego", "Completo, tranquilo o sin juego"),
    Entry("frames", "Avatar y marco", "Tu dibujo y las recompensas de logros"),
    Entry("lab", "Laboratorio", "Funciones en prueba que puedes apagar"),
    Entry("data", "Tus datos", "Exportar, importar, cambiar de teléfono o borrar"),
    Entry("privacy", "Privacidad", "Qué se guarda y dónde"),
    Entry("about", "Acerca de LevelUp", "Versión, licencias y tutorial"),
)

@Composable
fun ProfileScreen(vm: LevelUpViewModel, nav: Nav, tabBar: @Composable () -> Unit) {
    val prefs = LocalPrefs.current
    val hasAccount = LocalHasAccount.current
    val p by vm.progress.collectAsStateWithLifecycle()
    var editName by remember { mutableStateOf(false) }
    val frame = Cosmetics.byId[prefs.equippedFrame] ?: Cosmetics.byId.getValue(Cosmetics.DEFAULT)

    ScreenScaffold("Perfil", bottomBar = tabBar) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + Space.xl)) {
            item {
                Row(Modifier.padding(vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(prefs.displayName, frame, p.level.level.takeIf { prefs.gameOn }, 72.dp, Avatars.emoji(prefs.avatar))
                    Spacer(Modifier.size(Space.l))
                    Column(Modifier.weight(1f)) {
                        Text(prefs.displayName.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleLarge)
                        if (prefs.gameOn) Text(XpRules.titleFor(p.level.level), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { editName = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar nombre") }
                }
            }
            items(if (hasAccount) entries else entries.filter { it.section != "account" && it.section != "social" }, key = { it.section }) { e ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable {
                        when (e.section) {
                            "account" -> nav.account()
                            "social" -> nav.social()
                            "plus" -> nav.plus()
                            "metas" -> nav.goals()
                            "review" -> nav.weeklyReview()
                            else -> nav.settings(e.section)
                        }
                    }.padding(vertical = Space.s),
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
fun SettingsScreen(vm: LevelUpViewModel, nav: Nav, platform: Platform, section: String, signedIn: Boolean = false) {
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
                    var askUntil by remember { mutableStateOf(false) }
                    RestSection(prefs, p.dayKind == DayKind.PAUSED, onChange) { on -> if (on) askUntil = true else vm.setPaused(false) }
                    if (askUntil) PauseUntilDialog(vm) { askUntil = false }
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
                "data" -> DataSection(vm, nav, signedIn)
                "privacy" -> {
                    ToggleRow("Proteger mis pantallas", prefs.secureScreens, { v -> onChange { it.copy(secureScreens = v) } },
                        "Impide capturas y grabaciones de pantalla dentro de LevelUp, y oculta su vista previa en las apps recientes.")
                    PrivacySection()
                }
                "lab" -> LabSection(vm, prefs)
                "about" -> AboutSection(vm, nav, platform)
            }
        }
    }
}

/** «¿Hasta cuándo?»: la pausa puede terminar sola o seguir hasta que la quites. */
@Composable
private fun PauseUntilDialog(vm: LevelUpViewModel, onClose: () -> Unit) {
    val today by vm.today.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("¿Hasta cuándo?") },
        text = {
            Column {
                Text("Mientras dure no hay retos ni avisos, y nada se acumula para después.")
                listOf(null to "Hasta que la quite", 3 to "3 días", 7 to "1 semana", 14 to "2 semanas", 30 to "1 mes").forEach { (days, label) ->
                    TextButton(
                        onClick = { vm.setPaused(true, days?.let { today.plus(it - 1, DateTimeUnit.DAY) }); onClose() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FramesSection(vm: LevelUpViewModel) {
    val prefs = LocalPrefs.current
    val p by vm.progress.collectAsStateWithLifecycle()
    val unlocked = p.frames.map { it.id }.toSet()
    SectionTitle("Avatar")
    Text("Un dibujo en lugar de tu inicial. Si tienes perfil social, tus amigos también lo ven.", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        (listOf("" to "Aa") + Avatars.all).forEach { (key, emoji) ->
            val selected = prefs.avatar == key
            Box(
                Modifier.size(52.dp)
                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .selectable(selected, role = Role.RadioButton) { vm.updatePrefs { it.copy(avatar = key) } }
                    .semantics { contentDescription = tr(if (key.isEmpty()) "Tu inicial" else key) },
                contentAlignment = Alignment.Center,
            ) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
        }
    }
    SectionTitle("Marco")
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
            Avatar(prefs.displayName, f, null, 48.dp, Avatars.emoji(prefs.avatar))
            Spacer(Modifier.size(Space.m))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        f.unlockedBy == null -> "Disponible desde el inicio"
                        f.unlockedBy == Cosmetics.PLUS -> if (available) "Incluido en LevelUp Plus" else "Se consigue con LevelUp Plus"
                        available -> tr("Conseguido con «{0}»", source)
                        AchievementCatalog.byId[f.unlockedBy]?.hidden == true -> "Se consigue con un logro secreto"
                        else -> tr("Se consigue con «{0}»", source)
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prefs.equippedFrame == f.id) Icon(Icons.Filled.Check, contentDescription = tr("En uso"))
        }
    }
}

@Composable
private fun DataSection(vm: LevelUpViewModel, nav: Nav, signedIn: Boolean) {
    var confirmImport by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    SectionTitle("Copia de seguridad")
    Text(
        "Guarda un archivo con todo tu progreso (formato JSON, legible). Con él puedes recuperar tus datos en otro teléfono.",
        style = MaterialTheme.typography.bodyMedium,
    )
    PrimaryButton("Exportar mis datos", vm::exportData, Modifier.padding(top = Space.s))
    SecondaryButton("Importar un respaldo", { confirmImport = true })
    SecondaryButton("Exportar historial en CSV", vm::exportCsv)
    Text(
        "El CSV es para abrirlo en una hoja de cálculo: una fila por reto, con fecha, estado y nota. No sirve para restaurar.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionTitle("Cambiar de teléfono")
    listOfNotNull(
        "Con cuenta: inicia sesión en el teléfono nuevo y todo se descarga solo.".takeIf { LocalHasAccount.current },
        "Sin cuenta: exporta tus datos aquí, pasa el archivo al teléfono nuevo (correo, nube o cable) e impórtalo en Perfil → Tus datos.",
        "LevelUp Plus se recupera en Perfil → LevelUp Plus → «Ya lo compré: restaurar», con la misma cuenta de Google Play.",
    ).forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium) }
    Text(
        "Además, si tienes activada la copia de seguridad de Android, el sistema guarda tus datos y los restaura al instalar LevelUp en un teléfono nuevo.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionTitle("Borrar")
    if (signedIn) {
        Text(
            "Con la sesión iniciada, tus datos también están en tu cuenta. Para borrarlos de este teléfono, cierra sesión; " +
                "para borrarlos de la nube, borra la cuenta.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SecondaryButton("Ir a Cuenta", nav::account)
    } else {
        Text("Borra todo lo que LevelUp guarda en este teléfono. No se puede deshacer.", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Borrar todos mis datos", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("¿Importar un respaldo?") },
            text = {
                Text(
                    if (signedIn) "Tus datos actuales se reemplazarán por los del archivo, también los de tu cuenta. Si el archivo no es válido, no se cambia nada."
                    else "Tus datos actuales se reemplazarán por los del archivo. Si el archivo no es válido, no se cambia nada.",
                )
            },
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
    val hasAccount = LocalHasAccount.current
    listOfNotNull(
        "Sin cuenta" to "Todo se queda en este teléfono: ajustes, retos, experiencia, logros y misiones. Nada sale de él.",
        ("Con cuenta (opcional)" to "Se guardan tu correo o tu nombre de usuario y una copia de tu progreso en Supabase, el proveedor de " +
            "base de datos de LevelUp, para recuperarlo o usarlo en otro teléfono. La conexión va cifrada y solo tu cuenta puede leer tus datos.")
            .takeIf { hasAccount },
        ("Amigos (opcional)" to "Si creas un perfil social, otras personas ven tu nombre de usuario y tu nombre visible. Tus amigos " +
            "ven tu nivel, tu racha y tu XP de la semana solo si lo permites. Tus retos, categorías e historial no se comparten nunca.")
            .takeIf { hasAccount },
        "Copia de Android" to "Si tienes activada la copia de seguridad de Android, Google guarda una copia cifrada de los datos de tus " +
            "apps en tu cuenta, LevelUp incluida (sin tu sesión). Puedes desactivarla en los ajustes del teléfono.",
        "Anuncios y analítica" to "No hay anuncios, ni analítica, ni seguimiento. No hace falta ningún «modo sin analítica»: nunca la hubo.",
        "Health Connect (opcional)" to "Si lo activas en Laboratorio, LevelUp lee tus pasos y minutos de ejercicio de hoy para mostrarlos " +
            "en los retos físicos. Solo se leen en el teléfono: no se guardan ni se suben.",
        "Permisos" to if (hasAccount) "Internet, para la cuenta y lo social, y notificaciones, solo si activas los recordatorios o los avisos de amigos."
            else "Notificaciones, solo si activas los recordatorios.",
        "Tus derechos" to if (hasAccount) "Exporta tus datos o bórralos en Perfil → Tus datos. Borra tu cuenta y todo lo de la nube en Perfil → Cuenta."
            else "Exporta tus datos o bórralos en Perfil → Tus datos.",
        "Salud" to "LevelUp no es un servicio médico ni hace diagnósticos. Adapta los retos a tu situación.",
        "Contacto" to "Johan Jafet Del Valle Santiago · donsackito@protonmail.com",
    ).forEach { (title, body) ->
        SectionTitle(title)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AboutSection(vm: LevelUpViewModel, nav: Nav, platform: Platform) {
    Text(tr("LevelUp {0}", platform.appVersion), style = MaterialTheme.typography.titleMedium)
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
