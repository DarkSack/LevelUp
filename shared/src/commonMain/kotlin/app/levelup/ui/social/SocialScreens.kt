package app.levelup.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import app.levelup.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import app.levelup.cloud.AccountState
import app.levelup.cloud.Encouragements
import app.levelup.cloud.GroupChallenge
import app.levelup.cloud.GroupHistoryItem
import app.levelup.cloud.MyProfile
import app.levelup.cloud.ProfileCard
import app.levelup.cloud.Reactions
import app.levelup.cloud.ReportReasons
import app.levelup.cloud.SocialNotice
import app.levelup.content.AchievementCatalog
import app.levelup.content.Avatars
import app.levelup.content.Catalog
import app.levelup.content.Cosmetics
import app.levelup.domain.AchievementState
import app.levelup.domain.XpRules
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.components.MultiChoiceRow
import app.levelup.ui.Fmt
import app.levelup.ui.Nav
import app.levelup.ui.account.CloudViewModel
import app.levelup.ui.account.SignInRequired
import app.levelup.ui.components.Avatar
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.EmptyState
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.LabeledProgress
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.StepperRow
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import app.levelup.i18n.tr

private fun frameOf(id: String) = Cosmetics.byId[id] ?: Cosmetics.byId.getValue(Cosmetics.DEFAULT)

private fun challengeTitle(id: String) = Catalog.byId[id]?.title ?: "Reto"

/** Puerta de entrada: pide cuenta y perfil antes de mostrar lo social. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(vm: CloudViewModel?, local: LevelUpViewModel, nav: Nav, today: LocalDate) {
    val account = vm?.account?.collectAsStateWithLifecycle()?.value
    val profile = vm?.profile?.collectAsStateWithLifecycle()?.value
    ScreenScaffold(
        "Amigos y grupos",
        onBack = nav::back,
        actions = {
            if (profile != null) {
                IconButton(onClick = nav::socialProfile) { Icon(Icons.Filled.Settings, contentDescription = tr("Ajustes del perfil social")) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm == null -> Text("Esta versión de la app no tiene la nube configurada.", Modifier.padding(Space.l))
                account !is AccountState.SignedIn -> SignInRequired(nav, "Conectar con amigos")
                else -> SocialContent(vm, local, nav, today, account)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialContent(vm: CloudViewModel, local: LevelUpViewModel, nav: Nav, today: LocalDate, account: AccountState.SignedIn) {
    val loaded by vm.profileLoaded.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshSocial() }
    when {
        !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        profile == null -> Column(Modifier.verticalScroll(rememberScrollState()).padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text("Crea tu perfil social", style = MaterialTheme.typography.titleLarge)
            Text("Es opcional. Solo tus amigos ven tu progreso, y solo si tú lo permites. Tus retos y categorías nunca se comparten.")
            ProfileForm(null, account.userId, vm, local) {}
        }
        else -> {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            val notices by vm.notices.collectAsStateWithLifecycle()
            val unread = notices.count { !it.read }
            Column(Modifier.fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf("Amigos", "Grupos", "Clasificación", if (unread > 0) tr("Avisos ({0})", unread) else "Avisos").forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                    }
                }
                when (tab) {
                    0 -> FriendsTab(vm, today)
                    1 -> GroupsTab(vm, nav, account.userId, today)
                    2 -> LeaderboardTab(vm)
                    else -> NoticesTab(vm, today)
                }
            }
        }
    }
}

// ── Amigos ────────────────────────────────────────────────────────────────────

@Composable
private fun FriendsTab(vm: CloudViewModel, today: LocalDate) {
    val friends by vm.friends.collectAsStateWithLifecycle()
    val requests by vm.requests.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        item {
            SectionTitle("Buscar por nombre de usuario")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    query, { v -> query = v.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(20) },
                    label = { Text("Usuario") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(Space.s))
                OutlinedButton(onClick = { vm.search(query) }, enabled = query.length >= 3, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Buscar")
                }
            }
        }
        items(results, key = { "s-" + it.userId }) { p ->
            PersonRow(p) {
                if (p.isFriend) Text("Amigo", style = MaterialTheme.typography.labelMedium)
                else TextButton(onClick = { vm.sendRequest(p.username) }, enabled = !busy) { Text("Añadir") }
            }
        }
        val incoming = requests.filter { it.incoming == true }
        val outgoing = requests.filter { it.incoming == false }
        if (incoming.isNotEmpty()) item { SectionTitle("Te quieren añadir") }
        items(incoming, key = { "i-" + it.requestId }) { p ->
            PersonRow(p) {
                TextButton(onClick = { vm.respond(p.requestId!!, false) }, enabled = !busy) { Text("Rechazar") }
                TextButton(onClick = { vm.respond(p.requestId!!, true) }, enabled = !busy) { Text("Aceptar") }
            }
        }
        if (outgoing.isNotEmpty()) item { SectionTitle("Solicitudes enviadas") }
        items(outgoing, key = { "o-" + it.requestId }) { p ->
            PersonRow(p) { TextButton(onClick = { vm.cancelRequest(p.requestId!!) }, enabled = !busy) { Text("Retirar") } }
        }
        item { SectionTitle("Tus amigos") }
        if (friends.isEmpty()) {
            item { EmptyState("Aún no tienes amigos aquí", "Busca a alguien por su nombre de usuario. LevelUp funciona igual sin amigos.") }
        }
        items(friends, key = { "f-" + it.userId }) { FriendRow(it, vm, today) }
    }
}

@Composable
private fun PersonRow(p: ProfileCard, actions: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p.displayName, frameOf(p.frame), p.level, 40.dp, Avatars.emoji(p.avatar))
        Spacer(Modifier.size(Space.m))
        Column(Modifier.weight(1f)) {
            Text(p.displayName, style = MaterialTheme.typography.bodyLarge)
            Text("@${p.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

@Composable
private fun FriendRow(p: ProfileCard, vm: CloudViewModel, today: LocalDate) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var reporting by remember { mutableStateOf(false) }
    var cheering by remember { mutableStateOf(false) }
    InfoCard {
        PersonRow(p) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Más opciones para {0}", p.displayName)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Quitar de amigos") }, { menu = false; confirm = "remove" })
                    DropdownMenuItem({ Text("Bloquear") }, { menu = false; confirm = "block" })
                    DropdownMenuItem({ Text("Denunciar") }, { menu = false; reporting = true })
                }
            }
        }
        val details = buildList {
            p.level?.let { add(tr("Nivel {0} · {1}", it, XpRules.titleFor(it))) }
            p.streak?.takeIf { it > 0 }?.let { add(tr("Racha de {0}", Fmt.plural(it, "día", "días"))) }
            p.weeklyXp?.let { add(tr("{0} XP esta semana", it)) }
            p.lastActiveAt?.let { Fmt.lastSeen(it, today) }?.let { add(tr("Activo {0}", it)) }
        }
        if (details.isNotEmpty()) Text(details.joinToString(" · ") { tr(it) }, style = MaterialTheme.typography.bodySmall)
        p.title?.let { Text(tr("Título: «{0}»", XpRules.titleFor(it)), style = MaterialTheme.typography.bodySmall) }
        val trophies = p.trophies.mapNotNull { AchievementCatalog.byId[it]?.name }
        if (trophies.isNotEmpty()) Text(tr("Trofeos: {0}", trophies.joinToString { tr(it) }), style = MaterialTheme.typography.bodySmall)
        Text("Felicitar", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = Space.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Reactions.forEach { (kind, emoji) ->
                IconButton(onClick = { vm.react(p.userId, kind) }) {
                    Text(emoji, modifier = Modifier.semantics { this.contentDescription = tr("Enviar {0} a {1}", kind, p.displayName) })
                }
            }
            Box {
                TextButton(onClick = { cheering = true }) { Text("Ánimo") }
                DropdownMenu(cheering, { cheering = false }) {
                    Encouragements.forEach { (kind, text) ->
                        DropdownMenuItem({ Text(text) }, { cheering = false; vm.react(p.userId, kind) })
                    }
                }
            }
        }
    }
    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (action == "block") tr("¿Bloquear a {0}?", p.displayName) else tr("¿Quitar a {0}?", p.displayName)) },
            text = {
                Text(
                    if (action == "block") "Dejaréis de ser amigos, no podrá encontrarte ni enviarte nada. No se le avisa."
                    else "Dejaréis de ver vuestro progreso. Podéis volver a ser amigos más adelante.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirm = null; if (action == "block") vm.block(p.userId) else vm.removeFriend(p.userId) }) {
                    Text(if (action == "block") "Bloquear" else "Quitar")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
    if (reporting) ReportDialog(p, vm) { reporting = false }
}

@Composable
private fun ReportDialog(p: ProfileCard, vm: CloudViewModel, onDone: () -> Unit) {
    var reason by remember { mutableStateOf(ReportReasons.first().first) }
    var alsoBlock by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(tr("Denunciar a {0}", p.displayName)) },
        text = {
            Column {
                ReportReasons.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(reason == key, role = Role.RadioButton) { reason = key },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == key, onClick = null)
                        Spacer(Modifier.size(Space.s))
                        Text(label)
                    }
                }
                ToggleRow("Bloquear también", alsoBlock, { alsoBlock = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.report(p.userId, reason)
                if (alsoBlock) vm.block(p.userId)
                onDone()
            }) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancelar") } },
    )
}

// ── Grupos ────────────────────────────────────────────────────────────────────

@Composable
private fun GroupsTab(vm: CloudViewModel, nav: Nav, me: String, today: LocalDate) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val history by vm.groupHistory.collectAsStateWithLifecycle()
    val friends by vm.friends.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        item {
            Text(
                "Elige un reto del catálogo y hacedlo juntos (sumáis entre todos) o compitiendo (quién lo completa más veces). " +
                    "Cuenta lo que cada uno completa y sincroniza.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton("Crear reto en grupo", nav::createGroup, Modifier.padding(top = Space.s), enabled = friends.isNotEmpty())
            if (friends.isEmpty()) Text("Necesitas al menos un amigo.", style = MaterialTheme.typography.bodySmall)
        }
        if (groups.isEmpty()) item { EmptyState("Sin retos en grupo", "Cuando crees uno o te inviten, aparecerá aquí.") }
        val joined = groups.filter { it.myStatus == "joined" }
        if (joined.isNotEmpty()) item { GroupCalendar(joined, today) }
        items(groups, key = { it.id }) { GroupCard(it, me, vm, busy) }
        if (history.isNotEmpty()) {
            item { SectionTitle("Historial") }
            items(history, key = { "h-" + it.id }) { HistoryRow(it) }
        }
    }
}

/** Calendario del mes con los días que tienen un reto en grupo en marcha. */
@Composable
private fun GroupCalendar(groups: List<GroupChallenge>, today: LocalDate) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val first = LocalDate(today.year, today.month, 1).plus(offset, DateTimeUnit.MONTH)
    val days = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { offset-- }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            // "{1} de {0}" y no "{0} de {1}": esa clave ya es la de Fmt.day y en inglés lleva otro orden.
            Text(tr("{1} de {0}", first.year, Fmt.monthName(first.month)).replaceFirstChar { it.uppercase() },
                Modifier.weight(1f).semantics { heading() }, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { offset++ }) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        val lead = first.dayOfWeek.ordinal // lunes = 0
        ((0 until lead).map { null } + (1..days).map { first.plus(it - 1, DateTimeUnit.DAY) }).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val active = date?.let { d -> groups.filter { d >= it.startsOn && d <= it.endsOn } }.orEmpty()
                    Box(
                        Modifier.weight(1f).heightIn(min = 36.dp).clearAndSetSemantics {
                            if (date != null) contentDescription = Fmt.day(date) +
                                if (active.isEmpty()) "" else ": " + active.joinToString { tr(challengeTitle(it.challengeId)) }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            Box(
                                Modifier.size(32.dp).then(
                                    if (active.isNotEmpty()) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier,
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${date.day}", fontWeight = if (date == today) FontWeight.Bold else null,
                                    color = if (active.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("Los días marcados tienen un reto en grupo en marcha.", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Space.xs))
    }
}

@Composable
private fun HistoryRow(h: GroupHistoryItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xs).semantics(mergeDescendants = true) {}) {
        Text(challengeTitle(h.challengeId), style = MaterialTheme.typography.bodyLarge)
        val result = when {
            h.myStatus == "left" -> "Lo dejaste"
            h.mode == "coop" && h.groupCompleted >= h.target -> "Meta lograda entre todos"
            h.mode == "coop" -> tr("{0} de {1} entre todos", h.groupCompleted, h.target)
            else -> "Competición"
        }
        Text(
            tr("Del {0} al {1} · {2} · ", Fmt.day(h.startsOn), Fmt.day(h.endsOn), Fmt.plural(h.members, "persona", "personas")) +
                tr("tú: {0} · {1}", Fmt.plural(h.myCompleted, "vez", "veces"), result),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupCard(g: GroupChallenge, me: String, vm: CloudViewModel, busy: Boolean) {
    InfoCard {
        Text(challengeTitle(g.challengeId), style = MaterialTheme.typography.titleMedium)
        Text(
            tr("{0} · del {1} al {2}", if (g.mode == "coop") "Juntos" else "Competición", Fmt.day(g.startsOn), Fmt.day(g.endsOn)),
            style = MaterialTheme.typography.bodySmall,
        )
        val joined = g.members.filter { it.status == "joined" }
        if (g.mode == "coop") {
            val total = joined.sumOf { it.completed }
            LabeledProgress((total.toFloat() / g.target).coerceIn(0f, 1f), tr("{0} de {1} entre todos", total, g.target), Modifier.padding(vertical = Space.s))
        } else {
            Text(tr("Meta: {0} veces", g.target), style = MaterialTheme.typography.bodySmall)
        }
        joined.sortedByDescending { it.completed }.forEach { m ->
            Text(
                "${m.displayName}${if (m.userId == me) " (tú)" else ""}: ${m.completed}" +
                    if (m.verified > 0) tr(" ({0} verificados)", m.verified) else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (m.userId == me) FontWeight.Bold else null,
            )
        }
        val invited = g.members.count { it.status == "invited" }
        if (invited > 0) Text("${Fmt.plural(invited, "invitación pendiente", "invitaciones pendientes")}", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (g.myStatus == "invited") {
                TextButton(onClick = { vm.respondGroup(g.id, false) }, enabled = !busy) { Text("Rechazar") }
                TextButton(onClick = { vm.respondGroup(g.id, true) }, enabled = !busy) { Text("Unirme") }
            } else {
                TextButton(onClick = { vm.leaveGroup(g.id) }, enabled = !busy) { Text("Salir") }
            }
        }
    }
}

/** Crear un reto en grupo: reto del catálogo, modo, meta, duración y amigos. */
@Composable
fun CreateGroupScreen(vm: CloudViewModel, nav: Nav, today: LocalDate) {
    val friends by vm.friends.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var challengeId by rememberSaveable { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf("coop") }
    var target by rememberSaveable { mutableIntStateOf(10) }
    var days by rememberSaveable { mutableIntStateOf(7) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    ScreenScaffold("Nuevo reto en grupo", onBack = nav::back) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            item {
                SectionTitle("1. El reto")
                val chosen = challengeId
                if (chosen != null) {
                    Text(challengeTitle(chosen), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { challengeId = null }) { Text("Elegir otro") }
                } else {
                    OutlinedTextField(query, { query = it.take(40) }, label = { Text("Buscar en el catálogo") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
            }
            if (challengeId == null) {
                val matches = Catalog.challenges.filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }.take(30)
                items(matches, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(false, role = Role.Button) { challengeId = c.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title)
                            Text(Fmt.minutes(c.minutes), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
            } else {
                item {
                    SectionTitle("2. Cómo")
                    ChoiceRow(null, listOf("coop" to "Juntos", "versus" to "Competir"), mode, { mode = it },
                        description = if (mode == "coop") "Sumáis entre todos hasta llegar a la meta." else "Gana quien lo completa más veces.")
                    StepperRow("Meta", Fmt.plural(target, "vez", "veces"),
                        onMinus = if (target > 1) ({ target-- }) else null, onPlus = if (target < 100) ({ target++ }) else null)
                    ChoiceRow("Duración", listOf(7 to "1 semana", 14 to "2 semanas", 30 to "1 mes"), days, { days = it })
                    SectionTitle("3. Con quién (hasta 9)")
                }
                items(friends, key = { it.userId }) { f ->
                    val checked = f.userId in selected
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox,
                            enabled = checked || selected.size < 9) { selected = if (checked) selected - f.userId else selected + f.userId },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.size(Space.s))
                        Text(f.displayName)
                    }
                }
                item {
                    PrimaryButton("Crear e invitar", {
                        vm.createGroup(challengeId!!, mode, target, today, today.plus(days - 1, DateTimeUnit.DAY), selected.toList(), nav::back)
                    }, Modifier.padding(top = Space.m), enabled = !busy && selected.isNotEmpty())
                }
            }
        }
    }
}

// ── Clasificación ─────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardTab(vm: CloudViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val rows by vm.leaderboard.collectAsStateWithLifecycle()
    val p = profile ?: return
    LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        item {
            Text(
                "XP de esta semana entre tus amigos que eligen participar. Empieza de cero cada lunes y cuenta como mucho " +
                    "600 XP por día. Es voluntaria: puedes salir cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ToggleRow("Participar en la clasificación", p.leaderboardOptIn, { vm.saveProfile(p.copy(leaderboardOptIn = it)) })
        }
        if (p.leaderboardOptIn) {
            if (rows.size <= 1) item { EmptyState("Aún no hay con quién comparar", "Aparecerán los amigos que también participen.") }
            items(rows, key = { it.userId }) { r ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${rows.indexOf(r) + 1}.", modifier = Modifier.size(32.dp, 24.dp))
                    Avatar(r.displayName, frameOf(r.frame), null, 32.dp)
                    Spacer(Modifier.size(Space.s))
                    Text(r.displayName + if (r.isMe) tr(" (tú)") else "", Modifier.weight(1f), fontWeight = if (r.isMe) FontWeight.Bold else null)
                    Text(if (r.weeklyVerified > 0) tr("{0} XP · {1} verificados", r.weeklyXp, r.weeklyVerified) else tr("{0} XP", r.weeklyXp))
                }
            }
        }
    }
}

// ── Avisos ────────────────────────────────────────────────────────────────────

fun noticeText(n: SocialNotice): String {
    val who = n.actor?.displayName ?: "Alguien"
    return when (n.kind) {
        "friend_request" -> tr("{0} quiere ser tu amigo.", who)
        "friend_accepted" -> tr("{0} aceptó tu solicitud.", who)
        "reaction" -> Encouragements.firstOrNull { it.first == n.refId }?.let { tr("{0} te dice: «{1}»", who, it.second) }
            ?: tr("{0} te felicitó {1}", who, Reactions.firstOrNull { it.first == n.refId }?.second.orEmpty()).trim()
        "group_invite" -> tr("{0} te invitó a un reto en grupo.", who)
        "group_joined" -> tr("{0} se unió a tu reto en grupo.", who)
        else -> tr("Novedad de {0}.", who)
    }
}

@Composable
private fun NoticesTab(vm: CloudViewModel, today: LocalDate) {
    val notices by vm.notices.collectAsStateWithLifecycle()
    LaunchedEffect(notices.any { !it.read }) { if (notices.any { !it.read }) vm.markNoticesRead() }
    LazyColumn(contentPadding = PaddingValues(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        if (notices.isEmpty()) item { EmptyState("Sin avisos", "Aquí verás solicitudes, felicitaciones e invitaciones de tus amigos.") }
        items(notices, key = { it.id }) { n ->
            Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(noticeText(n), style = MaterialTheme.typography.bodyLarge, fontWeight = if (!n.read) FontWeight.Bold else null)
                Fmt.lastSeen(n.createdAt, today)?.let { Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

// ── Perfil social ─────────────────────────────────────────────────────────────

@Composable
fun SocialProfileScreen(vm: CloudViewModel, local: LevelUpViewModel, nav: Nav) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    ScreenScaffold("Perfil social", onBack = nav::back) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            val a = account as? AccountState.SignedIn ?: return@Column
            ProfileForm(profile, a.userId, vm, local, nav::back)
        }
    }
}

@Composable
private fun ProfileForm(current: MyProfile?, userId: String, vm: CloudViewModel, local: LevelUpViewModel, onSaved: () -> Unit) {
    val prefs = LocalPrefs.current
    val progress by local.progress.collectAsStateWithLifecycle()
    var titleLevel by rememberSaveable { mutableStateOf(current?.titleLevel) }
    var trophies by rememberSaveable { mutableStateOf(current?.trophies ?: emptyList()) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    var username by rememberSaveable { mutableStateOf(current?.username ?: "") }
    var displayName by rememberSaveable { mutableStateOf(current?.displayName ?: prefs.displayName) }
    var discoverable by rememberSaveable { mutableStateOf(current?.discoverable ?: true) }
    var shareProgress by rememberSaveable { mutableStateOf(current?.shareProgress ?: true) }
    var showActivity by rememberSaveable { mutableStateOf(current?.showActivity ?: false) }
    var leaderboard by rememberSaveable { mutableStateOf(current?.leaderboardOptIn ?: false) }
    val usernameOk = Regex("^[a-z0-9_]{3,20}$").matches(username)

    OutlinedTextField(
        username, { v -> username = v.lowercase().filter { it in 'a'..'z' || it.isDigit() || it == '_' }.take(20) },
        label = { Text("Nombre de usuario") }, singleLine = true, isError = username.isNotEmpty() && !usernameOk,
        supportingText = { Text("De 3 a 20 letras sin acentos, números o _. Así te encuentran tus amigos.") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        displayName, { displayName = it.take(30) }, label = { Text("Nombre visible") }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    SectionTitle("Privacidad")
    ToggleRow("Aparecer en las búsquedas", discoverable, { discoverable = it }, "Si lo quitas, solo te encuentra quien ya es tu amigo.")
    ToggleRow("Compartir mi progreso con amigos", shareProgress, { shareProgress = it }, "Nivel, racha y XP de la semana. Nunca tus retos ni categorías.")
    ToggleRow("Mostrar cuándo usé la app", showActivity, { showActivity = it }, "Tus amigos ven «activo hoy», «ayer»…")
    ToggleRow("Participar en la clasificación semanal", leaderboard, { leaderboard = it })

    SectionTitle("Lo que muestras")
    Text("Tu avatar y tu marco se cambian en Perfil → Avatar y marco. El título y los trofeos los ven tus amigos si compartes tu progreso.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ChoiceRow(
        "Título", listOf(null to "Ninguno") + XpRules.TITLES.filter { it.first <= progress.level.level }, titleLevel, { titleLevel = it },
    )
    val unlocked = progress.achievements.filter { it.state == AchievementState.UNLOCKED }.map { it.achievement }
    if (unlocked.isNotEmpty()) {
        Text("Trofeos (hasta 3)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Space.s))
        MultiChoiceRow(unlocked.map { it.id to it.name }, trophies.toSet()) { id ->
            trophies = when {
                id in trophies -> trophies - id
                trophies.size < 3 -> trophies + id
                else -> trophies
            }
        }
    }
    PrimaryButton("Guardar", {
        vm.saveProfile(
            MyProfile(
                userId, username, displayName.trim(), prefs.equippedFrame, discoverable, shareProgress, showActivity, leaderboard,
                avatar = prefs.avatar, titleLevel = titleLevel, trophies = trophies,
            ),
            onSaved,
        )
    }, Modifier.padding(top = Space.m), enabled = !busy && usernameOk && displayName.isNotBlank())
}
