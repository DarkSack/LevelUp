package app.levelup.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.content.Categories
import app.levelup.domain.ExtendedStats
import app.levelup.ui.Fmt
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.Nav
import app.levelup.ui.components.ChoiceRow
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.LabeledProgress
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.theme.Accents
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.Space

@Composable
fun PlusScreen(vm: LevelUpViewModel, nav: Nav) {
    val plus by vm.plus.collectAsStateWithLifecycle()
    ScreenScaffold("LevelUp Plus", onBack = nav::back) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text(
                "Nada de Plus es necesario para progresar: todos los retos, la XP, las rachas, los logros y lo social son gratis.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionTitle("Qué incluye")
            Text("• 4 marcos de perfil exclusivos.\n• 6 colores para la app.\n• Estadísticas ampliadas de tu historial.")
            Text(
                "Es una compra única: pagas una vez y es tuyo en todos tus teléfonos con la misma cuenta de Google Play.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (plus.owned) {
                InfoCard { Text("Ya tienes LevelUp Plus. Gracias por apoyar la app.") }
                AccentChooser(vm)
                ExtendedStatsSection(vm)
            } else {
                val price = plus.price
                if (price != null) {
                    PrimaryButton("Comprar por $price", vm::buyPlus, Modifier.padding(top = Space.s))
                } else {
                    Text(
                        "La tienda no responde ahora mismo. Revisa tu conexión e inténtalo más tarde.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SecondaryButton("Ya lo compré: restaurar", vm::refreshPlus)
            }
        }
    }
}

@Composable
private fun AccentChooser(vm: LevelUpViewModel) {
    val prefs = LocalPrefs.current
    SectionTitle("Color de la app")
    ChoiceRow(
        title = null,
        options = Accents.map { it.key to it.name },
        selected = prefs.accent,
        onSelect = { key -> vm.updatePrefs { it.copy(accent = key) } },
        description = "Con alto contraste activado se mantienen los colores de máximo contraste.",
    )
}

@Composable
private fun ExtendedStatsSection(vm: LevelUpViewModel) {
    val p by vm.progress.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val s: ExtendedStats = p.extended
    SectionTitle("Estadísticas ampliadas")
    if (s.completed == 0) {
        Text("Completa tu primer reto para ver tus estadísticas.")
        return
    }
    Text("Retos completados: ${s.completed}")
    Text("Tiempo dedicado: ${Fmt.minutes(s.minutes)}")
    Text("Días con actividad: ${s.activeDays}")
    Text("Media por día activo: ${s.perActiveDayTenths / 10},${s.perActiveDayTenths % 10} retos")
    s.bestWeekday?.let { Text("Tu mejor día: ${Fmt.weekday(it)}") }
    if (s.simplified > 0) Text("En versión sencilla: ${s.simplified} (también cuentan)")

    SectionTitle("Por categoría")
    val top = s.byCategory.firstOrNull()?.second ?: 1
    s.byCategory.forEach { (key, n) ->
        LabeledProgress(n.toFloat() / top, "${Categories.name(key, custom)}: $n")
    }

    SectionTitle("XP de los últimos 12 meses")
    val maxXp = s.xpByMonth.maxOf { it.second }.coerceAtLeast(1)
    s.xpByMonth.forEach { (month, xp) ->
        LabeledProgress(xp.toFloat() / maxXp, "${Fmt.monthName(month.month)} ${month.year}: $xp XP")
    }
}
