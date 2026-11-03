package app.levelup.pets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

// Todas miran de frente, sentadas, en un lienzo de 100 × 100: cabeza hacia y = 44, cuerpo hacia y = 73, suelo en y = 92.

internal val Ink = Color(0xFF2B2234)
internal const val LINE = 2.2f
private val Blush = Color(0x66FF7A9C)
private val MouthRed = Color(0xFFE2557A)
private val Tear = Color(0xFF7FC8F8)

// Tlacuache: gris con la cara blanca, orejas y patas negras y cola pelona rosada.
private val OpGray = Color(0xFF9EA3AB)
private val OpBelly = Color(0xFFDADDE2)
private val OpFace = Color(0xFFF6F2EA)
private val OpPatch = Color(0xFFD9D5CE)
private val OpCrown = Color(0xFFC6C9CF)
private val OpPink = Color(0xFFF4A9BA)
private val OpNose = Color(0xFFEE7F9B)
private val OpDark = Color(0xFF3A3540)

internal fun DrawScope.tlacuache(p: Pose) {
    rotate(p.sway, pivot = Offset(64f, 84f)) {
        inkedPath(Path().apply { moveTo(62f, 86f); cubicTo(84f, 92f, 96f, 74f, 86f, 62f); cubicTo(80f, 55f, 72f, 62f, 78f, 67f) }, OpPink, 4.5f)
        drawPath(Path().apply { moveTo(62f, 86f); quadraticTo(67f, 88f, 72f, 88.5f) }, OpDark, style = Stroke(4.5f, cap = StrokeCap.Round))
    }
    for (x in listOf(39f, 61f)) {
        part(OpDark, x, 89f, 7f, 4.5f)
        part(OpPink, x, 91.2f, 4.5f, 1.8f, outline = false)
    }
    part(OpGray, 50f, 72f, 22f, 19f, shine = true)
    part(OpBelly, 50f, 76f, 13f, 12f, outline = false)
    part(OpDark, 31.5f, 76f, 4.5f, 7f)
    part(OpDark, 68.5f, 76f, 4.5f, 7f)
    head(p) {
        for ((x, left) in listOf(31f to true, 69f to false)) {
            val base = Offset(if (left) 36f else 64f, 37f)
            rotate(if (left) -p.earLeft else p.earRight, pivot = base) {
                part(OpDark, x, 30f, 9f, 9.5f)
                part(OpPink, x + (50f - x) * 0.03f, 31f, 5f, 5.5f, outline = false)
            }
        }
        // Mechones de pelo blanco en las mejillas, debajo de la cara para que solo asomen las puntas.
        for (m in listOf(false, true)) {
            fun x(v: Float) = if (m) 100f - v else v
            filledPath(Path().apply {
                moveTo(x(33f), 41f); lineTo(x(25f), 47f); lineTo(x(30.5f), 48.5f); lineTo(x(26.5f), 54f); lineTo(x(37f), 55f); close()
            }, OpFace)
        }
        part(OpFace, 50f, 44f, 21f, 17f, shine = true)
        part(OpCrown, 50f, 33f, 4.5f, 5f, outline = false)
        part(OpPatch, 41f, 43f, 6f, 5f, outline = false)
        part(OpPatch, 59f, 43f, 6f, 5f, outline = false)
        // Hocico en punta, como el de un tlacuache de verdad.
        val snout = Path().apply { moveTo(45f, 49f); quadraticTo(46.5f, 54.5f, 50f, 56.5f); quadraticTo(53.5f, 54.5f, 55f, 49f) }
        drawPath(snout, Ink.copy(alpha = 0.45f), style = Stroke(LINE * 0.6f, cap = StrokeCap.Round))
        whiskers(44f, 56f, 53f)
        mouth(p, 50f, 58.6f, 2.6f)
        part(OpNose, 50f, 55.5f, 3.4f, 2.4f)
        eyes(p, 41f, 59f, 43f)
        cheeks(p, 35f, 65f, 50f)
    }
}

// Ajolote: rosa con pecas, cabeza ancha, tres branquias por lado y una sonrisa de oreja a oreja.
private val AxPink = Color(0xFFF7B2CB)
private val AxBelly = Color(0xFFFCD6E2)
private val AxGill = Color(0xFFE5668A)
private val AxSpot = Color(0xFFEB8DB0)

internal fun DrawScope.ajolote(p: Pose) {
    rotate(p.sway * 0.6f, pivot = Offset(64f, 82f)) {
        filledPath(Path().apply { moveTo(62f, 76f); quadraticTo(90f, 70f, 94f, 86f); quadraticTo(80f, 92f, 62f, 88f); close() }, AxPink)
        drawPath(Path().apply { moveTo(66f, 80f); quadraticTo(82f, 77f, 89f, 84f) }, AxSpot, style = Stroke(1.6f, cap = StrokeCap.Round))
    }
    part(AxPink, 38f, 90f, 5f, 3f)
    part(AxPink, 62f, 90f, 5f, 3f)
    part(AxPink, 50f, 74f, 19f, 16f, shine = true)
    part(AxBelly, 50f, 78f, 11f, 9f, outline = false)
    part(AxPink, 33f, 79f, 4f, 6f)
    part(AxPink, 67f, 79f, 4f, 6f)
    head(p) {
        val wiggle = p.sway * 0.15f
        for (side in listOf(-1f, 1f)) {
            val base = Offset(50f + side * 22f, 44f)
            rotate(if (side < 0) -p.earLeft else p.earRight, pivot = base) {
                listOf(36f to 28f, 44f to 44f, 52f to 60f).forEachIndexed { i, (from, to) ->
                    val reach = if (i == 1) 39f else 37f
                    val tip = Offset(50f + side * reach, to + wiggle * side)
                    inkedPath(Path().apply { moveTo(50f + side * 22f, from); lineTo(tip.x, tip.y) }, AxGill, 4.5f)
                    part(AxGill, tip.x, tip.y, 3f, 3f)
                }
            }
        }
        part(AxPink, 50f, 46f, 26f, 18f, shine = true)
        for ((x, y) in listOf(39f to 33f, 58f to 31.5f, 64f to 36f, 30f to 49f, 70f to 51f)) drawCircle(AxSpot, 1.5f, Offset(x, y))
        eyes(p, 37f, 63f, 45f, 3.2f)
        when {
            p.glad -> mouth(p, 50f, 53f, 4.5f)
            p.sad -> frown(50f, 54.5f, 5f)
            else -> drawPath(Path().apply { moveTo(42f, 53f); quadraticTo(50f, 58f, 58f, 53f) }, Ink, style = Stroke(LINE, cap = StrokeCap.Round))
        }
        cheeks(p, 32f, 68f, 51f)
    }
}

// Gato: naranja atigrado con el hocico y las patas crema.
private val CatOrange = Color(0xFFF5A35C)
private val CatStripe = Color(0xFFD6762F)
private val CatCream = Color(0xFFFCE8D2)
private val CatPink = Color(0xFFF4A9BA)

internal fun DrawScope.gato(p: Pose) {
    rotate(p.sway, pivot = Offset(66f, 84f)) {
        inkedPath(Path().apply { moveTo(64f, 86f); cubicTo(86f, 88f, 90f, 70f, 84f, 56f) }, CatOrange, 6f)
        drawPath(Path().apply { moveTo(85.5f, 63f); quadraticTo(85f, 59f, 84f, 56f) }, CatStripe, style = Stroke(6f, cap = StrokeCap.Round))
    }
    part(CatCream, 40f, 90f, 6f, 3.5f)
    part(CatCream, 60f, 90f, 6f, 3.5f)
    part(CatOrange, 50f, 72f, 21f, 19f, shine = true)
    part(CatCream, 50f, 77f, 11f, 11f, outline = false)
    for ((a, b) in listOf(Offset(31f, 66f) to Offset(36f, 67.5f), Offset(30f, 72f) to Offset(35.5f, 72.5f))) {
        drawLine(CatStripe, a, b, 2f, StrokeCap.Round)
        drawLine(CatStripe, Offset(100f - a.x, a.y), Offset(100f - b.x, b.y), 2f, StrokeCap.Round)
    }
    part(CatCream, 43f, 86f, 4.5f, 3.5f)
    part(CatCream, 57f, 86f, 4.5f, 3.5f)
    head(p) {
        for (m in listOf(false, true)) {
            fun x(v: Float) = if (m) 100f - v else v
            rotate(if (m) p.earRight else -p.earLeft, pivot = Offset(x(38f), 31f)) {
                filledPath(Path().apply { moveTo(x(30f), 38f); lineTo(x(31f), 17f); lineTo(x(47f), 29f); close() }, CatOrange)
                drawPath(Path().apply { moveTo(x(33f), 33f); lineTo(x(33.5f), 22f); lineTo(x(43f), 29.5f); close() }, CatPink)
            }
        }
        part(CatOrange, 50f, 44f, 22f, 18f, shine = true)
        listOf(44f to 45f, 50f to 50f, 56f to 55f).forEach { (top, bottom) ->
            drawLine(CatStripe, Offset(top, 28f), Offset(bottom, 33f), 2f, StrokeCap.Round)
        }
        part(CatCream, 45.5f, 52f, 5.5f, 4.2f, outline = false)
        part(CatCream, 54.5f, 52f, 5.5f, 4.2f, outline = false)
        whiskers(40f, 60f, 52f)
        mouth(p, 50f, 54.5f, 3f)
        filledPath(Path().apply { moveTo(47.5f, 49f); lineTo(52.5f, 49f); lineTo(50f, 52f); close() }, CatPink)
        eyes(p, 41f, 59f, 43f)
        cheeks(p, 35f, 65f, 49f)
    }
}

internal fun DrawScope.shadow(p: Pose) {
    val rx = 24f + p.hop * 0.8f
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(50f - rx, 89.5f), Size(rx * 2, 7f))
}

/** Tres zetas que suben y se desvanecen. */
internal fun DrawScope.zzz(phase: Float) {
    for (i in 0..2) {
        val t = (phase + i / 3f) % 1f
        val w = 4f + 4f * t
        val x = 72f + 10f * t
        val y = 30f - 22f * t
        drawPath(Path().apply { moveTo(x, y); lineTo(x + w, y); lineTo(x, y + w); lineTo(x + w, y + w) }, Ink.copy(alpha = 1f - t),
            style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Destellos dorados que titilan alrededor de la cabeza. */
internal fun DrawScope.sparkles(phase: Float) {
    listOf(Offset(16f, 22f), Offset(84f, 16f), Offset(12f, 60f), Offset(88f, 54f)).forEachIndexed { i, c ->
        val r = 2.5f + 3.5f * abs(sin(2 * PI.toFloat() * (phase + i * 0.25f)))
        filledPath(Path().apply {
            moveTo(c.x, c.y - r); quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
            quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
        }, Color(0xFFFFC21A), line = 1.2f)
    }
}

/** Papelitos de colores que caen girando. */
internal fun DrawScope.confetti(phase: Float) {
    val colors = listOf(Color(0xFFFF6FA5), Color(0xFF4CC9F0), Color(0xFFFFD23F), Color(0xFF7BD389), Color(0xFFB388FF), Color(0xFFFF9F1C))
    colors.forEachIndexed { i, c ->
        val t = (phase + i / colors.size.toFloat()) % 1f
        val x = 8f + i * 16.5f + 3f * sin(2 * PI.toFloat() * (t + i * 0.3f))
        val y = -2f + 70f * t
        rotate(360f * t * (if (i % 2 == 0) 1 else -1), pivot = Offset(x, y)) {
            drawRect(c.copy(alpha = if (t < 0.8f) 1f else (1f - t) / 0.2f), Offset(x - 1.6f, y - 2.6f), Size(3.2f, 5.2f))
        }
    }
}

/** Nubecita gris con gotas encima de la cabeza: la racha se apagó. */
internal fun DrawScope.rainCloud(phase: Float) {
    val bob = 1.2f * sin(2 * PI.toFloat() * phase)
    for ((i, x) in listOf(44f, 51f, 58f).withIndex()) {
        val t = (phase * 2f + i / 3f) % 1f
        drawLine(Tear.copy(alpha = 1f - t), Offset(x, 16f + 8f * t), Offset(x - 0.8f, 18.5f + 8f * t), 1.8f, StrokeCap.Round)
    }
    val cloud = Path().apply {
        addOval(Rect(Offset(43f, 9f + bob), 6f))
        addOval(Rect(Offset(51f, 7f + bob), 7.5f))
        addOval(Rect(Offset(58.5f, 10f + bob), 5f))
        addRect(Rect(41f, 10f + bob, 61f, 15f + bob))
    }
    drawPath(cloud, Ink, style = Stroke(LINE * 1.6f, join = StrokeJoin.Round))
    drawPath(cloud, Color(0xFFB9BEC7))
}

/** Corazón que sube al tocar la mascota. */
internal fun DrawScope.heart(poke: Float) {
    val c = Offset(74f, 30f - 20f * poke)
    drawPath(heartPath(c, 4f + 2f * poke), Color(0xFFFF4D7E).copy(alpha = 1f - poke * poke))
}

/** Óvalo relleno con borde de tinta; [shine] le da volumen con un brillo arriba y sombra abajo. */
internal fun DrawScope.part(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, outline: Boolean = true, shine: Boolean = false) {
    val topLeft = Offset(cx - rx, cy - ry)
    val size = Size(rx * 2, ry * 2)
    drawOval(color, topLeft, size)
    if (shine) {
        val r = max(rx, ry)
        drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), Offset(cx - rx * 0.35f, cy - ry * 0.45f), r * 0.8f), topLeft, size)
        drawOval(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)), Offset(cx - rx * 0.2f, cy - ry * 0.3f), r * 1.3f), topLeft, size)
    }
    if (outline) drawOval(Ink, topLeft, size, style = Stroke(LINE))
}

/** Trazo grueso con borde de tinta (colas, branquias). */
internal fun DrawScope.inkedPath(path: Path, color: Color, width: Float) {
    drawPath(path, Ink, style = Stroke(width + LINE * 2, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.filledPath(path: Path, color: Color, line: Float = LINE) {
    drawPath(path, color)
    drawPath(path, Ink, style = Stroke(line, join = StrokeJoin.Round))
}

internal fun DrawScope.eyes(p: Pose, left: Float, right: Float, y: Float, r: Float = 3.6f) {
    val stroke = Stroke(LINE, cap = StrokeCap.Round)
    for ((i, x) in listOf(left, right).withIndex()) {
        when {
            p.happyEyes -> drawArc(Ink, 180f, 180f, false, Offset(x - r, y - r * 0.4f), Size(r * 2, r * 2), style = stroke)
            p.sleeping -> drawArc(Ink, 0f, 180f, false, Offset(x - r, y - r), Size(r * 2, r * 2), style = stroke)
            p.eyeOpen < 0.25f -> drawLine(Ink, Offset(x - r, y), Offset(x + r, y), LINE, StrokeCap.Round)
            else -> {
                // El párpado baja aplastando el ojo, sin saltos.
                val c = Offset(x + p.look, y + if (p.sad) 0.6f else 0f)
                val h = r * p.eyeOpen
                drawOval(Ink, Offset(c.x - r, c.y - h), Size(r * 2, h * 2))
                if (p.eyeOpen > 0.6f) {
                    drawCircle(Color.White, r * (if (p.sad) 0.45f else 0.35f), Offset(c.x - r * 0.3f, c.y - h * 0.35f))
                    if (p.sad) drawCircle(Color.White, r * 0.2f, Offset(c.x + r * 0.35f, c.y + h * 0.35f))
                }
            }
        }
        if (p.sad) {
            // Cejas caídas: el extremo de dentro más alto.
            val inward = if (i == 0) 1f else -1f
            drawLine(Ink, Offset(x - inward * r, y - r - 1.5f), Offset(x + inward * r * 0.8f, y - r - 4.5f), 1.8f, StrokeCap.Round)
        }
    }
    if (p.sad) {
        val c = Offset(left - r * 0.5f, y + r + 1f + p.tearDrop)
        drawPath(Path().apply {
            moveTo(c.x, c.y - 3.2f); quadraticTo(c.x + 2f, c.y, c.x + 1.7f, c.y + 1f)
            quadraticTo(c.x, c.y + 3f, c.x - 1.7f, c.y + 1f); quadraticTo(c.x - 2f, c.y, c.x, c.y - 3.2f); close()
        }, Tear.copy(alpha = p.tearAlpha))
    }
}

internal fun DrawScope.cheeks(p: Pose, left: Float, right: Float, y: Float) {
    if (!p.glad) return
    for (x in listOf(left, right)) drawOval(Blush, Offset(x - 4f, y - 2.5f), Size(8f, 5f))
}

/** Boca en «w»; abierta si está contenta y hacia abajo si está triste. */
internal fun DrawScope.mouth(p: Pose, cx: Float, y: Float, w: Float) {
    when {
        p.glad -> {
            val topLeft = Offset(cx - w, y - w * 0.8f)
            val size = Size(w * 2, w * 1.6f)
            drawArc(MouthRed, 0f, 180f, true, topLeft, size)
            drawArc(Ink, 0f, 180f, true, topLeft, size, style = Stroke(LINE * 0.8f, join = StrokeJoin.Round))
        }
        p.sad -> frown(cx, y + 1f, w)
        else -> drawPath(Path().apply { moveTo(cx - w, y); quadraticTo(cx - w / 2, y + w * 0.7f, cx, y); quadraticTo(cx + w / 2, y + w * 0.7f, cx + w, y) },
            Ink, style = Stroke(LINE * 0.8f, cap = StrokeCap.Round))
    }
}

internal fun DrawScope.frown(cx: Float, y: Float, w: Float) =
    drawPath(Path().apply { moveTo(cx - w, y + 1.2f); quadraticTo(cx, y - 2.2f, cx + w, y + 1.2f) }, Ink, style = Stroke(LINE * 0.9f, cap = StrokeCap.Round))

internal fun DrawScope.whiskers(left: Float, right: Float, y: Float) {
    val color = Ink.copy(alpha = 0.55f)
    for ((x, dir) in listOf(left to -1f, right to 1f)) {
        drawLine(color, Offset(x, y), Offset(x + dir * 12f, y - 2f), 1f, StrokeCap.Round)
        drawLine(color, Offset(x, y + 1.5f), Offset(x + dir * 12f, y + 3.5f), 1f, StrokeCap.Round)
    }
}
