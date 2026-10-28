package app.levelup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import app.levelup.ui.theme.StyleBorder
import app.levelup.ui.theme.styleShadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import app.levelup.ui.LocalSnackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.levelup.content.Categories
import app.levelup.domain.Cosmetic
import app.levelup.domain.CustomCategory
import app.levelup.ui.Fmt
import app.levelup.ui.theme.Brand
import app.levelup.ui.theme.Space
import app.levelup.ui.theme.categoryColor
import app.levelup.ui.theme.isDarkTheme
import app.levelup.ui.theme.LocalPrefs
import app.levelup.i18n.tr

/**
 * Símbolo de LevelUp: tres barras que suben como escalones y una chispa encima.
 * [bars] y [spark] (0..1) permiten animar su aparición en la bienvenida.
 */
@Composable
fun LevelUpLogo(modifier: Modifier = Modifier, bars: List<Float> = listOf(1f, 1f, 1f), spark: Float = 1f) {
    Canvas(modifier.semantics { contentDescription = tr("Logotipo de LevelUp") }) {
        val s = size.minDimension
        val origin = Offset((size.width - s) / 2, (size.height - s) / 2)
        drawRoundRect(Brand.Teal, origin, Size(s, s), CornerRadius(s * 0.24f))
        val pad = s * 0.2f
        val inner = s - 2 * pad
        val barW = inner / 4.2f
        val gap = barW * 0.6f
        val heights = listOf(0.34f, 0.54f, 0.76f)
        val bottom = origin.y + s - pad
        heights.forEachIndexed { i, h ->
            val p = bars.getOrElse(i) { 1f }.coerceIn(0f, 1f)
            val barH = inner * h * p
            val x = origin.x + pad + i * (barW + gap)
            if (barH > 0f) drawRoundRect(Color.White, Offset(x, bottom - barH), Size(barW, barH), CornerRadius(barW * 0.3f))
        }
        if (spark > 0f) {
            val cx = origin.x + pad + 2 * (barW + gap) + barW / 2
            val cy = bottom - inner * 0.76f - barW * 0.85f
            drawCircle(Brand.AmberLight.copy(alpha = spark.coerceIn(0f, 1f)), barW * 0.36f * (0.6f + 0.4f * spark), Offset(cx, cy))
        }
    }
}

/** Avatar con el marco cosmético equipado y, si se pasa, el nivel. */
@Composable
fun Avatar(name: String, frame: Cosmetic, level: Int?, size: Dp = 64.dp, emoji: String? = null) {
    val colors = frame.colors.map { Color(it) }
    val brush = if (colors.size == 1) Brush.linearGradient(listOf(colors[0], colors[0])) else Brush.sweepGradient(colors + colors.first())
    val description = buildString {
        append(tr("Avatar con marco {0}", frame.name))
        if (level != null) append(tr(", nivel {0}", level))
    }
    Box(Modifier.size(size).clearAndSetSemantics { contentDescription = description }) {
        Box(
            Modifier.size(size).border(size * 0.08f, brush, CircleShape).padding(size * 0.12f)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                emoji ?: name.trim().firstOrNull()?.uppercase() ?: "★",
                style = if (emoji != null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (level != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.42f)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = Space.l, bottom = Space.s).semantics { heading() },
    )
}

@Composable
fun LabeledProgress(fraction: Float, label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surface,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape)
                .border(StyleBorder, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Space.xs))
    }
}

@Composable
fun CategoryLabel(key: String, customCategories: List<CustomCategory> = emptyList(), modifier: Modifier = Modifier) {
    val dark = isDarkTheme(LocalPrefs.current)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(categoryColor(key, dark), CircleShape))
        Spacer(Modifier.width(Space.xs))
        Text(Categories.name(key, customCategories), style = MaterialTheme.typography.labelMedium)
    }
}

/** Dificultad con puntos y texto: nunca solo con color. */
@Composable
fun DifficultyLabel(difficulty: Int, modifier: Modifier = Modifier) {
    Text(
        "●".repeat(difficulty) + "○".repeat(5 - difficulty) + "  " + Fmt.difficulty(difficulty),
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clearAndSetSemantics { contentDescription = tr("Dificultad {0} de 5, {1}", difficulty, Fmt.difficulty(difficulty)) },
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(StyleBorder, MaterialTheme.colorScheme.outline)) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(StyleBorder, MaterialTheme.colorScheme.outline)) { Text(text) }
}

/** Tarjeta del estilo pop: borde de tinta y sombra sólida. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.surface, content: @Composable () -> Unit) {
    Card(
        modifier.fillMaxWidth().styleShadow(),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(StyleBorder, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(Space.l)) { content() }
    }
}

/** Fila con interruptor: toda la fila es el control (área táctil grande y una sola etiqueta para TalkBack). */
@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(Space.m))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Opciones excluyentes como chips; la elegida lleva una marca, no solo otro color. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceRow(title: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, description: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.s)) {
        if (title != null) Text(title, style = MaterialTheme.typography.bodyLarge)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) ({ Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }) else null,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

/** Selección múltiple como chips con marca. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoiceRow(options: List<Pair<T, String>>, selected: Set<T>, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        options.forEach { (value, label) ->
            val isSelected = value in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(value) },
                label = { Text(label) },
                leadingIcon = if (isSelected) ({ Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }) else null,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
fun StepperRow(title: String, value: String, onMinus: (() -> Unit)?, onPlus: (() -> Unit)?, description: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = tr("Menos {0}", title) }) { Text("−") }
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = Space.s))
        OutlinedButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = tr("Más {0}", title) }) { Text("+") }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.s))
        if (action != null) {
            Spacer(Modifier.size(Space.l))
            action()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(title, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Volver"))
                        }
                    }
                },
                actions = { actions() },
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbar.current) },
        floatingActionButton = floating,
        content = content,
    )
}
