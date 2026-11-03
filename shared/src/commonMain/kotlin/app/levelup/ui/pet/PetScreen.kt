package app.levelup.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.AchievementCatalog
import app.levelup.content.Pets
import app.levelup.domain.DayKind
import app.levelup.domain.Preferences
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.OutlinedTextField
import app.levelup.domain.InstanceStatus
import app.levelup.i18n.tr
import app.levelup.pets.Accessory
import app.levelup.pets.FlameTier
import app.levelup.pets.Pet
import app.levelup.pets.PetEgg
import app.levelup.pets.PetStage
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.levelup.pets.PetFrame
import app.levelup.pets.PetMood
import app.levelup.pets.STILL
import app.levelup.pets.Slot
import app.levelup.pets.Species
import app.levelup.pets.StreakFlame
import app.levelup.pets.toggle
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.ProgressState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.levelup.domain.Gems
import app.levelup.domain.StreakRepair
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.LabeledProgress
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.Text
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.LocalReduceMotion
import app.levelup.ui.theme.Space

/** Días libres o en pausa sin nada hecho: la mascota descansa contigo. */
private fun resting(p: ProgressState, doneToday: Boolean) = !doneToday && p.dayKind != DayKind.ACTIVE

/** Duerme si nunca hubo racha o es día libre, triste si la racha se apagó, y feliz o de fiesta al completar. */
fun petMood(p: ProgressState, doneToday: Boolean): PetMood =
    if (resting(p, doneToday)) PetMood.SLEEPY
    else PetMood.of(p.stats.currentStreak, doneToday, hadStreak = p.stats.bestStreak > 0)

/** Cómo está, en una frase: TalkBack de la mascota flotante y texto de su pantalla. */
private fun petStatus(name: String, p: ProgressState, doneToday: Boolean): String {
    val days = Fmt.plural(p.stats.currentStreak, "día", "días")
    return when (petMood(p, doneToday)) {
        PetMood.SLEEPY -> if (resting(p, doneToday)) tr("{0} descansa contigo hoy.", name)
            else tr("{0} está dormido. Despierta cuando completas un reto.", name)
        PetMood.SAD -> tr("{0} extraña tu racha. Con un reto pequeño se anima.", name)
        PetMood.CALM -> tr("{0} te espera para seguir tu racha de {1}.", name, days)
        PetMood.HAPPY -> tr("¡{0} está feliz! Racha de {1}.", name, days)
        PetMood.PARTY -> tr("¡{0} está de fiesta! Racha de {1}.", name, days)
    }
}

private fun wornOf(raw: String, p: ProgressState) = Accessory.parse(raw).intersect(p.petAccessories)

/** El nombre que le puso la persona o, si no, el de su especie. */
fun petName(prefs: Preferences, species: Species): String = Pets.customName(prefs, species) ?: tr(Pets.name(species))

/**
 * La mascota que flota arriba a la derecha de Inicio, con su llama y los días de racha. Se entiende sin texto.
 * Sin mascota elegida todavía ([species] null) se ve un huevo que, al tocarlo, lleva a elegirla.
 */
@Composable
fun FloatingPet(species: Species?, p: ProgressState, doneToday: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val animated = !LocalReduceMotion.current
    val prefs = LocalPrefs.current
    if (species == null) {
        Box(modifier.size(96.dp).clearAndSetSemantics {
            contentDescription = tr("Un huevo de mascota. Tócalo para abrirlo.")
            role = Role.Button
            onClick { onOpen(); true }
        }) {
            PetEgg(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onOpen() } }, animated, alebrije = Pets.eggPending(prefs))
        }
        return
    }
    val mood = petMood(p, doneToday)
    val streak = p.stats.currentStreak
    val status = petStatus(petName(prefs, species), p, doneToday)
    Box(modifier.size(96.dp).clearAndSetSemantics {
        contentDescription = status
        role = Role.Button
        onClick { onOpen(); true }
    }) {
        Pet(species, mood, Modifier.fillMaxSize(), wornOf(prefs.petAccessories, p), animated, onClick = onOpen, stage = PetStage.of(p.stats.bestStreak))
        if (streak > 0 || mood == PetMood.SAD) {
            Row(
                Modifier.align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(start = 2.dp, end = 7.dp, top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StreakFlame(FlameTier.of(streak), Modifier.size(22.dp), animated)
                if (streak > 0) Text("$streak", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Perro o gato, para la primera mascota (en la bienvenida y, si se omitió, en la pantalla de la mascota). */
@Composable
fun StarterChoice(selected: String, onPick: (Species) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Pets.STARTERS.forEach { s ->
            val on = selected == s.key
            Column(
                Modifier.weight(1f)
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(16.dp))
                    .selectable(on, role = Role.RadioButton) { onPick(s) }
                    .padding(Space.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PetFrame(s, PetMood.HAPPY, STILL, Modifier.size(96.dp))
                Text(Pets.name(s), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Pantalla de la mascota: cómo está, su nombre, su llama, sus números, la tienda y con qué adornarla. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetSection(vm: LevelUpViewModel) {
    val prefs = LocalPrefs.current
    val p by vm.progress.collectAsStateWithLifecycle()
    val today by vm.todayInstances.collectAsStateWithLifecycle()
    val animated = !LocalReduceMotion.current
    val species = Species.byKey(prefs.pet)?.takeIf { it in p.ownedPets }
    val worn = wornOf(prefs.petAccessories, p)
    var buying by remember { mutableStateOf<Species?>(null) }
    var buyingExtra by remember { mutableStateOf<Pets.Extra?>(null) }
    var naming by remember { mutableStateOf<Species?>(null) }
    val crack = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // Recién nacido: de fiesta mientras siga en esta pantalla.
    var hatched by remember { mutableStateOf(false) }
    val hatch: () -> Unit = {
        if (!crack.isRunning) scope.launch {
            if (animated) crack.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            hatched = true
            vm.hatchEgg()
        }
    }

    if (Pets.eggPending(prefs)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp).clearAndSetSemantics {
                contentDescription = tr("Un huevo de mascota. Tócalo para abrirlo.")
                role = Role.Button
                onClick { hatch(); true }
            }) {
                PetEgg(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { hatch() } }, animated, alebrije = true, crack = crack.value)
            }
            Text("Por ayudarnos a probar LevelUp, este huevo es solo tuyo. ¡Tócalo!", style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center)
        }
    }

    if (prefs.starterPet.isEmpty()) {
        SectionTitle("¿Perros o gatos?")
        Text("Elige tu primera mascota. Te acompaña en tu racha y después puedes adoptar más con gemas.", style = MaterialTheme.typography.bodyMedium)
        StarterChoice(prefs.starterPet, vm::chooseStarter)
    }

    if (species != null) {
        val done = today.any { it.status == InstanceStatus.COMPLETED }
        val name = petName(prefs, species)
        // Arriba queda aire: la nube de la mascota triste sale por encima de su cuadro.
        Column(Modifier.fillMaxWidth().padding(top = Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
            Pet(species, if (hatched && species == Species.ALEBRIJE) PetMood.PARTY else petMood(p, done), Modifier.size(200.dp), worn, animated, stage = PetStage.of(p.stats.bestStreak))
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { naming = species }) {
                Text(if (Pets.customName(prefs, species) == null) "Ponerle nombre" else "Cambiar su nombre")
            }
            Text(petStatus(name, p, done), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }

        val streak = p.stats.currentStreak
        val best = p.stats.bestStreak
        val tier = FlameTier.of(streak)
        SectionTitle("Su llama")
        Row(verticalAlignment = Alignment.CenterVertically) {
            StreakFlame(tier, Modifier.size(64.dp), animated)
            Spacer(Modifier.size(Space.m))
            Column(Modifier.weight(1f)) {
                Text(if (tier == null && best > 0) "Llama apagada" else if (tier == null) "Sin llama todavía" else Pets.flameName(tier),
                    style = MaterialTheme.typography.titleMedium)
                Text(tr("Racha actual: {0}", Fmt.plural(streak, "día", "días")), style = MaterialTheme.typography.bodyMedium)
                Text(tr("Mejor racha: {0}", Fmt.plural(best, "día", "días")), style = MaterialTheme.typography.bodyMedium)
            }
        }
        val next = tier?.next ?: if (tier == null) FlameTier.entries.first() else null
        if (next != null) {
            val from = tier?.from ?: 0
            LabeledProgress(
                (streak - from).toFloat() / (next.from - from),
                tr("{0} para la {1}", Fmt.plural(next.from - streak, "día", "días"), tr(Pets.flameName(next)).lowercase()),
                Modifier.padding(top = Space.s),
            )
        }
        val missed = p.streakMissed
        if (missed.isNotEmpty()) {
            InfoCard(Modifier.padding(top = Space.m), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text(tr("Tu racha de {0} se apagó", Fmt.plural(p.streakToRestore, "día", "días")), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (missed.size <= p.repairsLeft) tr("Puedes restablecerla: cubre {0} y usa {1} de tus restablecimientos de este mes.",
                        Fmt.plural(missed.size, "día perdido", "días perdidos"), missed.size)
                    else tr("Faltaron {0} y este mes te quedan {1} restablecimientos. Empieza una racha nueva hoy.",
                        Fmt.plural(missed.size, "día", "días"), p.repairsLeft),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (missed.size <= p.repairsLeft) PrimaryButton("Restablecer racha", vm::repairStreak, Modifier.padding(top = Space.s))
            }
        }
        Text(tr("Restablecimientos este mes: {0} de {1}", p.repairsLeft, StreakRepair.PER_MONTH),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.s))
        Text("Llamas que alcanzaste", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Space.m))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, maxItemsInEachRow = 4) {
            FlameTier.entries.forEach { t ->
                val reached = best >= t.from
                Column(
                    Modifier.width(76.dp).padding(bottom = Space.s).semantics(mergeDescendants = true) {
                        contentDescription = tr(Pets.flameName(t)) + ", " + tr(if (reached) "alcanzada" else "aún no")
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StreakFlame(t, Modifier.size(44.dp).alpha(if (reached) 1f else 0.25f), animated && reached)
                    Text(Fmt.plural(t.from, "día", "días"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("Se quedan encendidas aquí aunque tu racha se apague.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionTitle("Juntos")
        StatRow("Retos completados", "${p.stats.totalCompleted}")
        StatRow("Días desde el primer reto", "${p.stats.daysSinceFirst}")
        val stage = PetStage.of(best)
        StatRow("Etapa", tr(stageName(stage)))
        stage.next?.let { n ->
            Text(tr("Crece cuando tu mejor racha llegue a {0}.", Fmt.plural(n.from, "día", "días")),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatRow("Mascotas", "${p.ownedPets.size} / ${Species.entries.size}")
        StatRow("Accesorios conseguidos", "${p.petAccessories.size} / ${Accessory.entries.size}")
    }

    SectionTitle("Mascotas")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("💎", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(Space.s))
        Text(Fmt.plural(p.gems, "gema", "gemas"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    Text(
        tr("Ganas una gema por cada reto que completas (hasta {0} al día), de 5 a 40 por cada logro y {1} por cada revisión semanal. Con ellas adoptas mascotas y compras accesorios; cada mascota nueva cuesta más que la anterior.", Gems.DAILY_CAP, Gems.PER_REVIEW),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val price = Gems.price(Pets.boughtCount(prefs))
    FlowRow(Modifier.padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Species.entries.forEach { s ->
            val owned = s in p.ownedPets
            val on = owned && prefs.pet == s.key
            val firstFree = prefs.starterPet.isEmpty() && s in Pets.STARTERS
            val affordable = !s.exclusive && p.gems >= price
            Column(
                Modifier.width(104.dp).heightIn(min = 140.dp)
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(16.dp))
                    .selectable(on, enabled = owned || firstFree || affordable, role = Role.RadioButton) {
                        when {
                            owned -> vm.updatePrefs { it.copy(pet = s.key) }
                            firstFree -> vm.chooseStarter(s)
                            else -> buying = s
                        }
                    }
                    .padding(Space.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PetFrame(s, if (owned) PetMood.HAPPY else PetMood.SLEEPY, STILL, Modifier.size(72.dp).alpha(if (owned) 1f else 0.45f))
                Text(if (owned) petName(prefs, s) else tr(Pets.name(s)), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                Text(
                    when {
                        on -> "Contigo"
                        owned -> "Tuya"
                        firstFree -> "Gratis"
                        s.exclusive -> "Solo betatesters"
                        else -> tr("💎 {0}", price)
                    },
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (prefs.pet.isNotEmpty()) {
        TextButton(onClick = { vm.updatePrefs { it.copy(pet = "") } }) { Text("Quitar la mascota de Inicio") }
    }
    buying?.let { s ->
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text(tr("¿Adoptar a {0}?", tr(Pets.name(s)))) },
            text = { Text(tr("Cuesta {0} gemas y te quedarán {1}. La siguiente mascota costará {2}.", price, p.gems - price, Gems.price(Pets.boughtCount(prefs) + 1))) },
            confirmButton = { TextButton(onClick = { vm.buyPet(s); buying = null }) { Text("Adoptar") } },
            dismissButton = { TextButton(onClick = { buying = null }) { Text("Cancelar") } },
        )
    }
    naming?.let { s ->
        var text by remember(s) { mutableStateOf(Pets.customName(prefs, s).orEmpty()) }
        AlertDialog(
            onDismissRequest = { naming = null },
            title = { Text("Ponerle nombre") },
            text = {
                OutlinedTextField(text, { text = it.take(20) }, label = { Text("Nombre (opcional)") }, singleLine = true,
                    placeholder = { Text(Pets.name(s)) })
            },
            confirmButton = { TextButton(onClick = { vm.renamePet(s, text); naming = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { naming = null }) { Text("Cancelar") } },
        )
    }
    if (species == null) return

    Text("Toca un accesorio para ponérselo o quitárselo. Lleva uno por sitio.", style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = Space.s))
    Slot.entries.forEach { slot ->
        SectionTitle(Pets.slotName(slot))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Pets.accessories.filter { it.accessory.slot == slot }.forEach { extra ->
                val a = extra.accessory
                val owned = a in p.petAccessories
                val open = a in p.accessoryGates
                val on = a in worn
                val source = extra.unlockedBy?.let { AchievementCatalog.byId[it]?.name }
                Column(
                    Modifier.width(104.dp).heightIn(min = 140.dp)
                        .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp))
                        .selectable(on, enabled = owned || (open && p.gems >= extra.price), role = Role.Checkbox) {
                            if (owned) vm.updatePrefs { it.copy(petAccessories = Accessory.format(worn.toggle(a))) } else buyingExtra = extra
                        }
                        .padding(Space.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PetFrame(species, PetMood.HAPPY, STILL, Modifier.size(72.dp).alpha(if (owned) 1f else 0.4f), worn.filterNot { it.slot == slot }.toSet() + a)
                    Text(extra.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    Text(
                        when {
                            on -> "Puesto"
                            owned -> "Tuyo"
                            !open && extra.season != null && source == null -> tr("Solo en {0}", tr(extra.season.label))
                            !open -> tr("Se consigue con «{0}»", source)
                            extra.season != null -> tr("💎 {0} · de temporada", extra.price)
                            else -> tr("💎 {0}", extra.price)
                        },
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    buyingExtra?.let { extra ->
        AlertDialog(
            onDismissRequest = { buyingExtra = null },
            title = { Text(tr("¿Comprar {0}?", tr(extra.name))) },
            text = { Text(tr("Cuesta {0} gemas y te quedarán {1}.", extra.price, p.gems - extra.price)) },
            confirmButton = { TextButton(onClick = { vm.buyAccessory(extra.accessory); buyingExtra = null }) { Text("Comprar") } },
            dismissButton = { TextButton(onClick = { buyingExtra = null }) { Text("Cancelar") } },
        )
    }
}

private fun stageName(stage: PetStage) = when (stage) {
    PetStage.BABY -> "Bebé"
    PetStage.YOUNG -> "Joven"
    PetStage.ADULT -> "Adulta"
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
