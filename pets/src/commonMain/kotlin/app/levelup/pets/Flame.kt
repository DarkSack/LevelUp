package app.levelup.pets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.sin

/** La llama de la racha: con más días cambia de color, crece y brilla más. */
enum class FlameTier(val from: Int, internal val outer: Color, internal val mid: Color, internal val core: Color, internal val size: Float) {
    NARANJA(1, Color(0xFFFF9F1C), Color(0xFFFFD166), Color(0xFFFFF3C4), 0.62f),
    ROJA(3, Color(0xFFFF4B1F), Color(0xFFFF9F1C), Color(0xFFFFE08A), 0.72f),
    AZUL(7, Color(0xFF2F6BFF), Color(0xFF5BC0FF), Color(0xFFE0F4FF), 0.82f),
    VIOLETA(30, Color(0xFF8E2DE2), Color(0xFFE36BFF), Color(0xFFFFE3FF), 0.92f),
    ARCOIRIS(100, Color(0xFFFF2E93), Color(0xFFFFC21A), Color(0xFFFFFFFF), 1f),
    DORADA(300, Color(0xFFFFC21A), Color(0xFFFFE066), Color(0xFFFFFFFF), 1f),
    COSMICA(500, Color(0xFF5B2EFF), Color(0xFFFF4FD8), Color(0xFFFFFFFF), 1f),
    LEGENDARIA(1000, Color(0xFF00E5FF), Color(0xFFB2FFFF), Color(0xFFFFFFFF), 1f);

    /** La siguiente llama, o null si ya es la última. */
    val next: FlameTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** null sin racha: no hay llama. */
        fun of(streak: Int): FlameTier? = entries.lastOrNull { streak >= it.from }
    }
}

/** [tier] null = racha apagada: brasa gris con humo. */
@Composable
fun StreakFlame(tier: FlameTier?, modifier: Modifier = Modifier, animated: Boolean = true) {
    val phase = if (animated) {
        rememberInfiniteTransition(label = "flame").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "phase",
        ).value
    } else STILL
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2, (size.height - 100f * s) / 2) {
            scale(s, s, pivot = Offset.Zero) { if (tier == null) smoke(phase) else flame(tier, phase) }
        }
    }
}

private fun DrawScope.flame(tier: FlameTier, phase: Float) {
    // Varias frecuencias a la vez para que el fuego no se vea como un péndulo.
    val tau = 2 * PI.toFloat()
    val flicker = 1f + 0.04f * sin(3 * tau * phase) + 0.025f * sin(5 * tau * phase + 1f)
    val h = 84f * tier.size * flicker
    val w = 30f * tier.size * (1f - 0.03f * sin(3 * tau * phase))
    val lean = 3f * sin(tau * phase) + 1.5f * sin(2 * tau * phase + 0.7f)
    if (tier >= FlameTier.VIOLETA) {
        val r = if (tier >= FlameTier.COSMICA) 50f else 46f
        drawCircle(Brush.radialGradient(listOf(tier.mid.copy(alpha = 0.45f + 0.1f * sin(tau * phase)), Color.Transparent), Offset(50f, 64f), r), r, Offset(50f, 64f))
    }
    if (tier == FlameTier.LEGENDARIA) {
        // Anillo de colores alrededor de la llama, con dos chispas que lo recorren.
        val rainbow = listOf(Color(0xFFFF2E93), Color(0xFFFFC21A), Color(0xFF2ECC71), Color(0xFF00E5FF), Color(0xFF8E2DE2), Color(0xFFFF2E93))
        drawOval(Brush.sweepGradient(rainbow, Offset(50f, 70f)), Offset(14f, 63f), Size(72f, 14f), style = Stroke(2.2f), alpha = 0.8f)
        for (i in 0..1) {
            val a = tau * (phase + i * 0.5f)
            drawCircle(Color.White, 2.2f, Offset(50f + 36f * kotlin.math.cos(a), 70f + 7f * sin(a)))
        }
    }
    val outer = flamePath(50f, 92f, w, h, lean)
    val fill: Brush? = when (tier) {
        FlameTier.ARCOIRIS, FlameTier.LEGENDARIA -> Brush.verticalGradient(
            listOf(Color(0xFF8E2DE2), Color(0xFF2F6BFF), Color(0xFF2ECC71), Color(0xFFFFC21A), Color(0xFFFF2E93)), 92f - h, 92f)
        FlameTier.DORADA -> Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21A), Color(0xFFD08700)), 92f - h, 92f)
        FlameTier.COSMICA -> Brush.verticalGradient(listOf(Color(0xFF0B0630), Color(0xFF5B2EFF), Color(0xFFFF4FD8)), 92f - h, 92f)
        else -> null
    }
    if (fill != null) {
        drawPath(outer, fill)
        drawPath(outer, Ink, style = Stroke(LINE))
    } else {
        filledPath(outer, tier.outer)
    }
    if (tier == FlameTier.COSMICA) {
        // Estrellitas dentro del fuego.
        for ((i, xy) in listOf(44f to 0.55f, 56f to 0.45f, 50f to 0.75f, 41f to 0.35f, 58f to 0.65f).withIndex()) {
            val a = 0.4f + 0.6f * sin(tau * (phase * 2 + i * 0.2f)).let { it * it }
            drawCircle(Color.White.copy(alpha = a), 1.1f, Offset(xy.first, 92f - h * xy.second))
        }
    }
    drawPath(flamePath(50f, 90f, w * 0.62f, h * 0.66f, lean * 0.7f), tier.mid)
    drawPath(flamePath(50f, 88f, w * 0.3f, h * 0.36f, lean * 0.4f), tier.core)
    // Chispas que suben desde la azul; más cuanto más alta la llama.
    if (tier >= FlameTier.AZUL) {
        val count = tier.ordinal
        for (i in 0 until count) {
            val t = (phase * 2f + i.toFloat() / count) % 1f
            val x = 50f + (i - count / 2f) * (54f / count) + 3f * sin(tau * (t + i * 0.37f))
            val color = if (tier == FlameTier.LEGENDARIA) listOf(Color(0xFFFF2E93), Color(0xFFFFC21A), Color(0xFF00E5FF))[i % 3] else tier.mid
            drawCircle(color.copy(alpha = 1f - t), 2.2f * (1f - t) + 0.6f, Offset(x, 92f - h * 0.7f - 22f * t))
        }
    }
}

private fun DrawScope.smoke(phase: Float) {
    filledPath(flamePath(50f, 92f, 14f, 22f, 0f), Color(0xFF6B6F78))
    drawPath(flamePath(50f, 91f, 7f, 10f, 0f), Color(0xFF9AA0A8))
    for (i in 0..1) {
        val t = (phase + i * 0.5f) % 1f
        val x = 50f + 6f * sin(2 * PI.toFloat() * (t + i * 0.5f))
        drawPath(Path().apply { moveTo(50f, 68f - 10f * t); quadraticTo(x + 8f, 56f - 20f * t, x, 44f - 26f * t) },
            Color(0xFF9AA0A8).copy(alpha = 0.7f * (1f - t)), style = Stroke(3f, cap = StrokeCap.Round))
    }
}

/** Gota que termina en punta: [bottom] es la base, [lean] mueve la punta a los lados. */
internal fun flamePath(cx: Float, bottom: Float, w: Float, h: Float, lean: Float) = Path().apply {
    val tip = Offset(cx + lean, bottom - h)
    moveTo(tip.x, tip.y)
    cubicTo(cx + w * 0.25f + lean * 0.5f, bottom - h * 0.6f, cx + w, bottom - h * 0.5f, cx + w * 0.85f, bottom - h * 0.22f)
    cubicTo(cx + w * 0.7f, bottom + h * 0.02f, cx - w * 0.7f, bottom + h * 0.02f, cx - w * 0.85f, bottom - h * 0.22f)
    cubicTo(cx - w, bottom - h * 0.5f, cx - w * 0.25f + lean * 0.5f, bottom - h * 0.6f, tip.x, tip.y)
    close()
}
