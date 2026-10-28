package app.levelup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.levelup.content.Categories
import app.levelup.domain.Achievement
import app.levelup.domain.AchievementState
import app.levelup.domain.AppStyle
import app.levelup.domain.TrophyTier
import app.levelup.ui.theme.LocalPrefs
import app.levelup.ui.theme.categoryColor
import app.levelup.ui.theme.isDarkTheme

/** Tres tonos de un material (luz, medio, sombra): con ellos se finge el volumen. */
private class Tones(val light: Color, val mid: Color, val dark: Color)

private fun tones(tier: TrophyTier) = when (tier) {
    TrophyTier.BRONZE -> Tones(Color(0xFFF2B27A), Color(0xFFC9773A), Color(0xFF7A4520))
    TrophyTier.SILVER -> Tones(Color(0xFFFFFFFF), Color(0xFFC9D1DA), Color(0xFF7D8894))
    TrophyTier.GOLD -> Tones(Color(0xFFFFF0A0), Color(0xFFFFC21A), Color(0xFFB37F00))
    TrophyTier.PLATINUM -> Tones(Color(0xFFFFFFFF), Color(0xFFB8C7D9), Color(0xFF5E7491))
}

private val Gray = Tones(Color(0xFFBDBDBD), Color(0xFF8E8E8E), Color(0xFF5E5E5E))

/** Símbolo dentro de la copa: la categoría en afinidades y dominios; si no, el de su colección. */
private fun symbolOf(a: Achievement): String {
    if (a.id == "mastery_all") return "👑"
    if (a.id.startsWith("affinity_") || a.id.startsWith("mastery_")) {
        return when (a.id.substringAfter('_')) {
            Categories.FISICO -> "💪"
            Categories.SOCIAL -> "🤝"
            Categories.MENTAL -> "🧠"
            Categories.PRODUCTIVIDAD -> "✅"
            Categories.BIENESTAR -> "🌿"
            Categories.CREATIVIDAD -> "🎨"
            Categories.FINANZAS -> "💰"
            Categories.DIGITAL -> "📱"
            Categories.HOGAR -> "🏠"
            else -> "⭐"
        }
    }
    return when (a.collection) {
        "Primeros pasos" -> "👣"
        "Constancia" -> "🔥"
        "Exploración" -> "🧭"
        "Crecimiento" -> "🚀"
        "A tu manera" -> "🛠️"
        else -> "🔮"
    }
}

/** Color del listón: el de la categoría en afinidades y dominios; si no, uno por colección. */
private fun ribbonOf(a: Achievement, dark: Boolean): Color {
    if (a.id.startsWith("affinity_") || a.id.startsWith("mastery_")) {
        if (a.id != "mastery_all") return categoryColor(a.id.substringAfter('_'), dark = false)
    }
    return Color(
        when (a.collection) {
            "Primeros pasos" -> 0xFF0F766E
            "Constancia" -> 0xFFD7263D
            "Exploración" -> 0xFF1D4ED8
            "Crecimiento" -> 0xFF6D28D9
            "A tu manera" -> 0xFFBE185D
            "Afinidades" -> 0xFF7E22CE
            else -> if (dark) 0xFF475569 else 0xFF334155
        },
    )
}

/**
 * Trofeo tipo escudo: copa que sostiene una esfera con el símbolo de su categoría o colección, sobre un listón de
 * su color. El nivel cambia el metal y los adornos: bronce liso, plata con asas, oro con alas y platino («platinar»,
 * como en los juegos) con alas, corona y destellos. Disponible: translúcido; bloqueado: gris con candado.
 * En el estilo Pop lleva contorno de tinta. El nombre va aparte: a este tamaño no se leería dentro.
 */
@Composable
fun TrophyBadge(a: Achievement, state: AchievementState, size: Dp = 48.dp) {
    val locked = state == AchievementState.LOCKED
    val t = if (locked) Gray else tones(a.tier)
    val prefs = LocalPrefs.current
    val ink = MaterialTheme.colorScheme.outline.takeIf { prefs.style == AppStyle.POP }
    val ribbon = if (locked) Gray.mid else ribbonOf(a, isDarkTheme(prefs))
    val tier = a.tier
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).alpha(if (state == AchievementState.UNLOCKED) 1f else 0.5f)) {
            val s = this.size.minDimension
            if (tier >= TrophyTier.GOLD) wings(s, t, ink)
            if (tier >= TrophyTier.SILVER) handles(s, t, ink)
            sphere(s, if (tier == TrophyTier.PLATINUM && !locked) Color(0xFF8BE9FF) else Color(0xFFF3E9D2), ink)
            cup(s, t, ink)
            ribbon(s, ribbon, ink)
            if (tier == TrophyTier.PLATINUM) crown(s, if (locked) Gray else tones(TrophyTier.GOLD), ink)
            if (tier == TrophyTier.PLATINUM && state == AchievementState.UNLOCKED) {
                sparkle(at(s, 0.88f, 0.1f), s * 0.08f, Color(0xFF8BE9FF))
                sparkle(at(s, 0.1f, 0.62f), s * 0.06f, Color(0xFF8BE9FF))
            }
        }
        val up = size * -0.17f
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Gray.dark, modifier = Modifier.offset(y = up).size(size * 0.22f))
        } else {
            val fontSize = with(LocalDensity.current) { (size * 0.19f).toSp() }
            androidx.compose.material3.Text(
                symbolOf(a), fontSize = fontSize,
                modifier = Modifier.offset(y = up).alpha(if (state == AchievementState.UNLOCKED) 1f else 0.6f),
            )
        }
    }
}

private fun at(s: Float, x: Float, y: Float) = Offset(s * x, s * y)

/** Luz desde la izquierda y sombra a la derecha. */
private fun sideLight(t: Tones, s: Float, from: Float, to: Float) =
    Brush.horizontalGradient(listOf(t.light, t.mid, t.mid, t.dark), startX = s * from, endX = s * to)

private fun DrawScope.edge(path: Path, ink: Color?, s: Float) {
    if (ink != null) drawPath(path, ink, style = Stroke(s * 0.03f))
}

private fun poly(s: Float, vararg p: Pair<Float, Float>) =
    Path().apply { p.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(s * x, s * y) else lineTo(s * x, s * y) }; close() }

/** Esfera del símbolo, iluminada desde arriba a la izquierda. */
private fun DrawScope.sphere(s: Float, color: Color, ink: Color?) {
    val c = at(s, 0.5f, 0.33f)
    val r = s * 0.19f
    drawCircle(Brush.radialGradient(listOf(Color.White, color, lerp(color, Color.Black, 0.25f)), at(s, 0.43f, 0.25f), r * 1.6f), r, c)
    if (ink != null) drawCircle(ink, r, c, style = Stroke(s * 0.03f))
}

/** Copa que abraza la mitad baja de la esfera, con su pie. */
private fun DrawScope.cup(s: Float, t: Tones, ink: Color?) {
    val foot = poly(s, 0.36f to 0.64f, 0.64f to 0.64f, 0.69f to 0.74f, 0.31f to 0.74f)
    drawPath(foot, sideLight(t, s, 0.31f, 0.69f))
    edge(foot, ink, s)
    val stem = poly(s, 0.45f to 0.54f, 0.55f to 0.54f, 0.55f to 0.65f, 0.45f to 0.65f)
    drawPath(stem, sideLight(t, s, 0.45f, 0.55f))
    edge(stem, ink, s)
    val bowl = Path().apply {
        moveTo(s * 0.24f, s * 0.34f)
        cubicTo(s * 0.24f, s * 0.6f, s * 0.76f, s * 0.6f, s * 0.76f, s * 0.34f)
        lineTo(s * 0.69f, s * 0.34f)
        cubicTo(s * 0.69f, s * 0.53f, s * 0.31f, s * 0.53f, s * 0.31f, s * 0.34f)
        close()
    }
    drawPath(bowl, sideLight(t, s, 0.24f, 0.76f))
    edge(bowl, ink, s)
}

/** Asas a los lados (plata en adelante). */
private fun DrawScope.handles(s: Float, t: Tones, ink: Color?) {
    for (left in listOf(true, false)) {
        val x = { v: Float -> if (left) v else 1f - v }
        val p = Path().apply {
            moveTo(s * x(0.27f), s * 0.22f)
            cubicTo(s * x(0.08f), s * 0.2f, s * x(0.08f), s * 0.48f, s * x(0.32f), s * 0.5f)
        }
        if (ink != null) drawPath(p, ink, style = Stroke(s * 0.1f))
        drawPath(p, if (left) t.light else t.mid, style = Stroke(s * 0.055f))
    }
}

/** Alas de plumas detrás de la copa (oro y platino). */
private fun DrawScope.wings(s: Float, t: Tones, ink: Color?) {
    for (left in listOf(true, false)) {
        val x = { v: Float -> if (left) v else 1f - v }
        val wing = poly(
            s, x(0.34f) to 0.2f, x(0.02f) to 0.1f, x(0.1f) to 0.22f, x(0.0f) to 0.26f, x(0.1f) to 0.36f,
            x(0.02f) to 0.42f, x(0.14f) to 0.5f, x(0.32f) to 0.46f,
        )
        drawPath(wing, Brush.verticalGradient(listOf(t.light, t.dark), startY = s * 0.1f, endY = s * 0.5f))
        edge(wing, ink, s)
    }
}

/** Listón de abajo con sus puntas dobladas, como en los escudos deportivos. */
private fun DrawScope.ribbon(s: Float, color: Color, ink: Color?) {
    val shade = lerp(color, Color.Black, 0.35f)
    for (left in listOf(true, false)) {
        val x = { v: Float -> if (left) v else 1f - v }
        val tail = poly(s, x(0.04f) to 0.78f, x(0.22f) to 0.78f, x(0.22f) to 0.94f, x(0.04f) to 0.94f, x(0.1f) to 0.86f)
        drawPath(tail, shade)
        edge(tail, ink, s)
    }
    val band = poly(s, 0.15f to 0.73f, 0.85f to 0.73f, 0.85f to 0.89f, 0.15f to 0.89f)
    drawPath(band, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.25f), color, shade), startY = s * 0.73f, endY = s * 0.89f))
    edge(band, ink, s)
}

/** Corona sobre la esfera (platino). */
private fun DrawScope.crown(s: Float, t: Tones, ink: Color?) {
    val p = poly(s, 0.36f to 0.16f, 0.36f to 0.04f, 0.43f to 0.1f, 0.5f to 0.0f, 0.57f to 0.1f, 0.64f to 0.04f, 0.64f to 0.16f)
    drawPath(p, sideLight(t, s, 0.36f, 0.64f))
    edge(p, ink, s)
}

/** Destello de cuatro puntas. */
private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
