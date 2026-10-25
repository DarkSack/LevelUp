package app.levelup.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.levelup.ui.components.LevelUpLogo
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.theme.LocalReduceMotion
import app.levelup.ui.theme.Space
import kotlinx.coroutines.launch

/** Duración total de la animación de bienvenida. Corta a propósito: se puede continuar desde el primer instante. */
const val WELCOME_ANIMATION_MS = 1600

/**
 * Bienvenida animada: las barras del logo crecen como escalones, aparece la chispa y luego los textos.
 * Sin destellos ni zoom; con "reducir movimiento" se muestra quieta desde el inicio. El botón está activo
 * desde el primer fotograma y "Omitir animación" la termina al momento.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit, startLabel: String = "Comenzar") {
    val reduce = LocalReduceMotion.current
    val t = remember { Animatable(if (reduce) 1f else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(reduce) {
        if (reduce) t.snapTo(1f) else t.animateTo(1f, tween(WELCOME_ANIMATION_MS, easing = LinearEasing))
    }
    fun phase(start: Float, length: Float) = FastOutSlowInEasing.transform(((t.value - start) / length).coerceIn(0f, 1f))
    val textAlpha = phase(0.62f, 0.25f)
    val lift = with(LocalDensity.current) { 8.dp.toPx() }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl, vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.heightIn(min = Space.xl))
        LevelUpLogo(
            Modifier.size(128.dp),
            bars = listOf(phase(0f, 0.3f), phase(0.12f, 0.3f), phase(0.24f, 0.3f)),
            spark = phase(0.45f, 0.2f),
        )
        Spacer(Modifier.size(Space.l))
        Text(
            "LevelUp",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { alpha = phase(0.5f, 0.2f) },
        )
        Spacer(Modifier.size(Space.xl))
        Column(
            Modifier.graphicsLayer { alpha = textAlpha; translationY = (1 - textAlpha) * lift },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Cada pequeño paso cuenta",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.size(Space.s))
            Text(
                "Construye hábitos, descubre tu potencial y avanza a tu ritmo.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(Space.xxl))
        PrimaryButton(startLabel, onStart)
        // El hueco queda reservado aunque se oculte, para que el botón principal no salte de sitio.
        val running = t.value < 1f
        TextButton(
            onClick = { scope.launch { t.snapTo(1f) } },
            enabled = running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .graphicsLayer { alpha = if (running) 1f else 0f }
                .then(if (running) Modifier else Modifier.clearAndSetSemantics {}),
        ) { Text("Omitir animación") }
    }
}
